package com.adoptar.service;

import com.adoptar.dto.request.ConfigurarDonacionRequest;
import com.adoptar.dto.request.DonacionRequest;
import com.adoptar.dto.response.DonacionRecibidaResponse;
import com.adoptar.dto.response.DonacionResponse;
import com.adoptar.dto.response.RescatistaDonacionResponse;
import com.adoptar.entity.Donacion;
import com.adoptar.entity.User;
import com.adoptar.enums.EstadoDonacion;
import com.adoptar.enums.TipoNotificacion;
import com.adoptar.enums.UserRole;
import com.adoptar.repository.DonacionRepository;
import com.adoptar.repository.UserRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DonacionService {

    private final DonacionRepository donacionRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final NotificacionService notificacionService;

    @Value("${mp.access.token}")
    private String appAccessToken;

    @Value("${mp.back.url.base}")
    private String backUrlBase;

    @Value("${mp.sandbox:true}")
    private boolean sandbox;

    // Configuracion de donaciones

    @Transactional
    public void configurar(User user, ConfigurarDonacionRequest request) {
        if (user.getRole() != UserRole.USER) {
            throw new IllegalArgumentException("Solo los usuarios pueden configurar donaciones");
        }
        if (request.isAceptaDonaciones()) {
            if (!user.isTieneTienda()) {
                throw new IllegalArgumentException("Necesitás estar verificado por un administrador para aceptar donaciones");
            }
            if (user.getOrganizacion() == null || user.getOrganizacion().isBlank()) {
                throw new IllegalArgumentException("Para aceptar donaciones necesitás tener una organización configurada en tu perfil");
            }
        }
        user.setAceptaDonaciones(request.isAceptaDonaciones());
        String desc = request.getDescripcionDonacion();
        user.setDescripcionDonacion(desc != null && !desc.isBlank() ? desc.trim() : null);
        userRepository.save(user);
    }

    // Listado publico de rescatistas

    @Transactional(readOnly = true)
    public List<RescatistaDonacionResponse> listarRescatistas(String provincia, String q, User usuarioActual) {
        return userRepository.findByAceptaDonacionesTrue().stream()
                .filter(u -> usuarioActual == null || !u.getId().equals(usuarioActual.getId()))
                .filter(u -> provincia == null || provincia.isBlank()
                        || provincia.equalsIgnoreCase(u.getProvincia()))
                .filter(u -> {
                    if (q == null || q.isBlank()) return true;
                    String busqueda = q.toLowerCase();
                    boolean enOrg = u.getOrganizacion() != null
                            && u.getOrganizacion().toLowerCase().contains(busqueda);
                    boolean enNombre = (u.getNombre() + " " + u.getApellido())
                            .toLowerCase().contains(busqueda);
                    return enOrg || enNombre;
                })
                .map(this::toRescatistaResponse)
                .toList();
    }

    // Crear preferencia de pago

    @Transactional
    public DonacionResponse crearPreferencia(DonacionRequest request, User donante)
            throws MPException, MPApiException {

        User rescatista = userRepository.findById(request.getRescatistaId())
                .orElseThrow(() -> new IllegalArgumentException("Rescatista no encontrado"));

        if (!rescatista.isAceptaDonaciones()) {
            throw new IllegalArgumentException("Este rescatista no acepta donaciones");
        }
        if (rescatista.getId().equals(donante.getId())) {
            throw new IllegalArgumentException("No podés donarte a vos mismo");
        }

        Donacion donacion = Donacion.builder()
                .donante(donante)
                .rescatista(rescatista)
                .monto(request.getMonto())
                .externalRef(UUID.randomUUID().toString())
                .build();
        donacionRepository.save(donacion);

        String nombreRescatista = rescatista.getOrganizacion() != null
                ? rescatista.getOrganizacion()
                : rescatista.getNombre() + " " + rescatista.getApellido();

        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .title("Donación a " + nombreRescatista)
                .quantity(1)
                .unitPrice(request.getMonto())
                .build();

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(backUrlBase + "/donar/exito")
                .failure(backUrlBase + "/donar/fallo")
                .pending(backUrlBase + "/donar/pendiente")
                .build();

        PreferenceRequest prefRequest = PreferenceRequest.builder()
                .items(List.of(item))
                .backUrls(backUrls)
                .externalReference(donacion.getExternalRef())
                .build();

        MercadoPagoConfig.setAccessToken(appAccessToken);
        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(prefRequest);

        donacion.setMpPreferenceId(preference.getId());
        donacionRepository.save(donacion);

        String url = sandbox ? preference.getSandboxInitPoint() : preference.getInitPoint();

        return DonacionResponse.builder()
                .id(donacion.getId())
                .checkoutUrl(url)
                .monto(request.getMonto())
                .rescatistaNombre(nombreRescatista)
                .build();
    }

    // Confirmar pago por donacion ID (busca en MP por external_reference)

    @Transactional
    public String confirmarPorDonacion(Long donacionId) {
        Donacion donacion = donacionRepository.findById(donacionId).orElse(null);
        if (donacion == null) return "PENDIENTE";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + appAccessToken);
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.mercadopago.com/v1/payments/search?external_reference=" + donacion.getExternalRef() + "&sort=date_created&criteria=desc",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            if (response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");
                if (results != null && !results.isEmpty()) {
                    Map<String, Object> payment = results.get(0);
                    String status = (String) payment.get("status");
                    String paymentId = String.valueOf(payment.get("id"));
                    aplicarEstadoPago(donacion.getExternalRef(), status, paymentId);
                }
            }
        } catch (Exception e) {
            // si falla, el estado queda PENDIENTE
        }
        return donacionRepository.findById(donacionId)
                .map(d -> d.getEstado().name())
                .orElse("PENDIENTE");
    }

    // Confirmar pago desde el return URL de MP (alternativa al webhook)

    @Transactional
    public void confirmarPago(String paymentId) {
        try {
            MercadoPagoConfig.setAccessToken(appAccessToken);
            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.get(Long.parseLong(paymentId));

            String externalRef = payment.getExternalReference();
            if (externalRef == null) return;

            aplicarEstadoPago(externalRef, payment.getStatus(), paymentId);
        } catch (Exception e) {
            // si falla la verificación el estado queda PENDIENTE
        }
    }

    // Webhook de MercadoPago

    @Transactional
    public void procesarWebhook(Map<String, Object> body) {
        String type = (String) body.get("type");
        if (!"payment".equals(type)) return;

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        if (data == null) return;

        String paymentIdStr = String.valueOf(data.get("id"));
        try {
            MercadoPagoConfig.setAccessToken(appAccessToken);
            PaymentClient paymentClient = new PaymentClient();
            Payment payment = paymentClient.get(Long.parseLong(paymentIdStr));

            String externalRef = payment.getExternalReference();
            if (externalRef == null) return;

            aplicarEstadoPago(externalRef, payment.getStatus(), paymentIdStr);
        } catch (Exception e) {
            // si falla el webhook no rompemos nada, solo ignoramos
        }
    }

    // aplica el estado del pago a la donacion y notifica al rescatista
    private void aplicarEstadoPago(String externalRef, String status, String paymentId) {
        donacionRepository.findByExternalRef(externalRef).ifPresent(donacion -> {
            if ("approved".equals(status)) {
                if (donacion.getEstado() != EstadoDonacion.COMPLETADA) {
                    notificacionService.crear(donacion.getRescatista(),
                            TipoNotificacion.NUEVA_DONACION,
                            "Recibiste una donación de $" + formatMonto(donacion.getMonto()),
                            "/perfil");
                }
                donacion.setEstado(EstadoDonacion.COMPLETADA);
            } else if ("rejected".equals(status) || "cancelled".equals(status)) {
                donacion.setEstado(EstadoDonacion.FALLIDA);
            }
            donacion.setMpPaymentId(paymentId);
            donacionRepository.save(donacion);
        });
    }

    // Historial del rescatista

    @Transactional(readOnly = true)
    public List<DonacionRecibidaResponse> getMisDonaciones(User user) {
        return donacionRepository.findByRescatistaOrderByCreadoEnDesc(user).stream()
                .filter(d -> d.getEstado() == EstadoDonacion.COMPLETADA)
                .map(d -> DonacionRecibidaResponse.builder()
                        .id(d.getId())
                        .donanteNombre(d.getDonante() != null
                                ? d.getDonante().getNombre() + " " + d.getDonante().getApellido()
                                : "Anónimo")
                        .monto(d.getMonto())
                        .estado(d.getEstado())
                        .creadoEn(d.getCreadoEn())
                        .build())
                .toList();
    }

    private String formatMonto(BigDecimal monto) {
        return monto.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private RescatistaDonacionResponse toRescatistaResponse(User u) {
        return RescatistaDonacionResponse.builder()
                .id(u.getId())
                .nombre(u.getNombre())
                .apellido(u.getApellido())
                .organizacion(u.getOrganizacion())
                .descripcionDonacion(u.getDescripcionDonacion())
                .provincia(u.getProvincia())
                .ciudad(u.getCiudad())
                .build();
    }
}
