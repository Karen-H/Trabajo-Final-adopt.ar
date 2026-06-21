package com.adoptar.service;

import com.adoptar.dto.request.EnvioDomicilioRequest;
import com.adoptar.dto.response.DisponibilidadRescatistaResponse;
import com.adoptar.dto.response.EnvioPendienteResponse;
import com.adoptar.dto.response.VentaHistorialResponse;
import com.adoptar.dto.response.VentaItemResponse;
import com.adoptar.dto.response.VentaResponse;
import com.adoptar.entity.CarritoItem;
import com.adoptar.entity.DisponibilidadRescatista;
import com.adoptar.entity.ItemTienda;
import com.adoptar.entity.User;
import com.adoptar.entity.Venta;
import com.adoptar.entity.VentaItem;
import com.adoptar.enums.EstadoEnvio;
import com.adoptar.enums.EstadoVenta;
import com.adoptar.enums.MetodoEnvio;
import com.adoptar.enums.TipoNotificacion;
import com.adoptar.enums.UserProfile;
import com.adoptar.repository.DisponibilidadRescatistaRepository;
import com.adoptar.repository.ItemTiendaRepository;
import com.adoptar.repository.UserRepository;
import com.adoptar.repository.VentaRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ItemTiendaRepository itemTiendaRepository;
    private final UserRepository userRepository;
    private final CarritoService carritoService;
    private final RestTemplate restTemplate;
    private final NotificacionService notificacionService;
    private final ChatService chatService;
    private final DisponibilidadRescatistaRepository disponibilidadRescatistaRepository;

    @Value("${mp.access.token}")
    private String appAccessToken;

    @Value("${mp.back.url.base}")
    private String backUrlBase;

    @Value("${mp.sandbox:true}")
    private boolean sandbox;

    // Crear preferencia de pago con los items del carrito del comprador para un rescatista

    @Transactional
    public VentaResponse crearPreferencia(User comprador, Long rescatistaId)
            throws MPException, MPApiException {

        if (comprador.getActiveProfile() != UserProfile.ADOPTANTE) {
            throw new IllegalArgumentException("Solo podés comprar con el perfil de adoptante activo");
        }

        User rescatista = userRepository.findById(rescatistaId)
                .orElseThrow(() -> new IllegalArgumentException("Rescatista no encontrado"));
        if (rescatista.getId().equals(comprador.getId())) {
            throw new IllegalArgumentException("No podés comprarte a vos mismo");
        }

        List<CarritoItem> carritoItems = carritoService.listarPorRescatista(comprador, rescatistaId);
        if (carritoItems.isEmpty()) {
            throw new IllegalArgumentException("No tenés ítems de esta tienda en el carrito");
        }

        for (CarritoItem ci : carritoItems) {
            if (ci.getCantidad() > ci.getItem().getStock()) {
                throw new IllegalArgumentException("No hay suficiente stock de \"" + ci.getItem().getTitulo() + "\"");
            }
        }

        BigDecimal montoTotal = carritoItems.stream()
                .map(ci -> ci.getItem().getPrecio().multiply(BigDecimal.valueOf(ci.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Venta venta = Venta.builder()
                .comprador(comprador)
                .rescatista(rescatista)
                .montoTotal(montoTotal)
                .externalRef(UUID.randomUUID().toString())
                .build();
        ventaRepository.save(venta);

        List<VentaItem> ventaItems = carritoItems.stream()
                .map(ci -> VentaItem.builder()
                        .venta(venta)
                        .item(ci.getItem())
                        .cantidad(ci.getCantidad())
                        .precioUnitario(ci.getItem().getPrecio())
                        .tituloItem(ci.getItem().getTitulo())
                        .build())
                .collect(Collectors.toList());
        venta.setItems(ventaItems);
        ventaRepository.save(venta);

        List<PreferenceItemRequest> preferenceItems = ventaItems.stream()
                .map(vi -> PreferenceItemRequest.builder()
                        .title(vi.getTituloItem())
                        .quantity(vi.getCantidad())
                        .unitPrice(vi.getPrecioUnitario())
                        .build())
                .toList();

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(backUrlBase + "/compra/exito")
                .failure(backUrlBase + "/compra/fallo")
                .pending(backUrlBase + "/compra/pendiente")
                .build();

        PreferenceRequest prefRequest = PreferenceRequest.builder()
                .items(preferenceItems)
                .backUrls(backUrls)
                .externalReference(venta.getExternalRef())
                .build();

        MercadoPagoConfig.setAccessToken(appAccessToken);
        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(prefRequest);

        venta.setMpPreferenceId(preference.getId());
        ventaRepository.save(venta);

        String url = sandbox ? preference.getSandboxInitPoint() : preference.getInitPoint();

        String nombreRescatista = rescatista.getOrganizacion() != null
                ? rescatista.getOrganizacion()
                : rescatista.getNombre() + " " + rescatista.getApellido();

        return VentaResponse.builder()
                .id(venta.getId())
                .checkoutUrl(url)
                .montoTotal(montoTotal)
                .rescatistaNombre(nombreRescatista)
                .build();
    }

    // ventas pendientes de pago del comprador

    @Transactional(readOnly = true)
    public Map<Long, Long> getVentasPendientesDePago(User comprador) {
        return ventaRepository.findByCompradorAndEstadoInOrderByCreadoEnDesc(comprador, List.of(EstadoVenta.PENDIENTE))
                .stream()
                .collect(Collectors.toMap(v -> v.getRescatista().getId(), Venta::getId, (a, b) -> a));
    }

    // Confirmar pago por venta ID (busca en MP por external_reference, para polling del frontend)

    @Transactional
    public String confirmarPorVenta(Long ventaId) {
        Venta venta = ventaRepository.findById(ventaId).orElse(null);
        if (venta == null) return "PENDIENTE";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + appAccessToken);
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://api.mercadopago.com/v1/payments/search?external_reference=" + venta.getExternalRef() + "&sort=date_created&criteria=desc",
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
                    aplicarEstadoPago(venta.getExternalRef(), status, paymentId);
                }
            }
        } catch (Exception e) {
            // si falla, el estado queda PENDIENTE
        }
        return ventaRepository.findById(ventaId)
                .map(v -> v.getEstado().name())
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

    // descuenta stock, vacia el carrito y abre el chat con el comprador
    private void aplicarEstadoPago(String externalRef, String status, String paymentId) {
        ventaRepository.findByExternalRef(externalRef).ifPresent(venta -> {
            if ("approved".equals(status)) {
                if (venta.getEstado() != EstadoVenta.COMPLETADA) {
                    completarVenta(venta);
                }
            } else if ("rejected".equals(status) || "cancelled".equals(status)) {
                venta.setEstado(EstadoVenta.FALLIDA);
            }
            venta.setMpPaymentId(paymentId);
            ventaRepository.save(venta);
        });
    }

    private void completarVenta(Venta venta) {
        venta.setEstado(EstadoVenta.COMPLETADA);
        venta.setEstadoEnvio(EstadoEnvio.PENDIENTE_METODO);

        List<ItemTienda> itemsComprados = venta.getItems().stream()
                .map(vi -> {
                    ItemTienda item = vi.getItem();
                    item.setStock(Math.max(0, item.getStock() - vi.getCantidad()));
                    itemTiendaRepository.save(item);
                    return item;
                })
                .toList();
        carritoService.eliminarItemsDelCarrito(venta.getComprador(), itemsComprados);

        String detalle = venta.getItems().stream()
                .map(vi -> vi.getTituloItem() + " x" + vi.getCantidad())
                .collect(Collectors.joining(", "));

        String montoSinDecimales = venta.getMontoTotal().setScale(0, RoundingMode.HALF_UP).toPlainString();

        notificacionService.crear(venta.getRescatista(), TipoNotificacion.NUEVA_VENTA,
                "Tenés una nueva venta de $" + montoSinDecimales + ": " + detalle,
                "/mis-ventas");

        chatService.abrirChatConMensaje(venta.getComprador(), venta.getRescatista(),
                venta.getComprador().getNombre() + " " + venta.getComprador().getApellido()
                        + " compró: " + detalle + " (Total: $" + montoSinDecimales + ")");

        chatService.abrirChatConMensaje(venta.getComprador(), venta.getRescatista(),
                "¡Gracias por tu compra! Por favor, indicá cómo querés recibir tu pedido.");
    }

    // bot de envio

    @Transactional(readOnly = true)
    public EnvioPendienteResponse getEnvioPendiente(User comprador, Long rescatistaId) {
        List<Venta> pendientes = ventaRepository.findPendientesEnvio(comprador, rescatistaId);
        if (pendientes.isEmpty()) return null;
        return toEnvioPendienteResponse(pendientes.get(0));
    }

    @Transactional
    public EnvioPendienteResponse elegirMetodoEnvio(Long ventaId, User comprador, MetodoEnvio metodo) {
        Venta venta = obtenerVentaDelComprador(ventaId, comprador);
        if (venta.getEstadoEnvio() != EstadoEnvio.PENDIENTE_METODO) {
            throw new IllegalArgumentException("Ya elegiste el método de envío para esta compra");
        }
        if (metodo == MetodoEnvio.RETIRO_DOMICILIO
                && !disponibilidadRescatistaRepository.existsByRescatistaId(venta.getRescatista().getId())) {
            throw new IllegalArgumentException("Esa opción no está disponible");
        }

        venta.setMetodoEnvio(metodo);
        String mensaje;
        if (metodo == MetodoEnvio.RETIRO_DOMICILIO) {
            venta.setEstadoEnvio(EstadoEnvio.PENDIENTE_HORARIO);
            mensaje = "Elegiste: retiro por el domicilio del vendedor. Elegí el día y horario que te quede mejor:";
        } else {
            venta.setEstadoEnvio(EstadoEnvio.PENDIENTE_DOMICILIO);
            mensaje = "Elegiste: " + etiquetaMetodo(metodo) + ". Por favor, completá tu domicilio:";
        }
        ventaRepository.save(venta);

        chatService.abrirChatConMensaje(venta.getComprador(), venta.getRescatista(), mensaje);

        return toEnvioPendienteResponse(venta);
    }

    @Transactional
    public EnvioPendienteResponse volverAElegirMetodo(Long ventaId, User comprador) {
        Venta venta = obtenerVentaDelComprador(ventaId, comprador);
        if (venta.getEstadoEnvio() != EstadoEnvio.PENDIENTE_HORARIO
                && venta.getEstadoEnvio() != EstadoEnvio.PENDIENTE_DOMICILIO) {
            throw new IllegalArgumentException("No podés volver atrás en este paso");
        }

        venta.setEstadoEnvio(EstadoEnvio.PENDIENTE_METODO);
        venta.setMetodoEnvio(null);
        venta.setHorarioRetiroElegido(null);
        venta.setDomicilioCalle(null);
        venta.setDomicilioAltura(null);
        venta.setDomicilioPiso(null);
        venta.setDomicilioDepto(null);
        venta.setDomicilioDescripcion(null);
        ventaRepository.save(venta);

        chatService.abrirChatConMensaje(venta.getComprador(), venta.getRescatista(),
                "Volviste a elegir cómo recibir tu pedido.");

        return toEnvioPendienteResponse(venta);
    }

    @Transactional
    public EnvioPendienteResponse elegirHorarioRetiro(Long ventaId, User comprador, Long bloqueId) {
        Venta venta = obtenerVentaDelComprador(ventaId, comprador);
        if (venta.getEstadoEnvio() != EstadoEnvio.PENDIENTE_HORARIO) {
            throw new IllegalArgumentException("Esta compra no está esperando un horario de retiro");
        }

        DisponibilidadRescatista bloque = disponibilidadRescatistaRepository.findById(bloqueId)
                .orElseThrow(() -> new IllegalArgumentException("Bloque de horario no encontrado"));
        if (!bloque.getRescatista().getId().equals(venta.getRescatista().getId())) {
            throw new IllegalArgumentException("Ese bloque de horario no pertenece a este vendedor");
        }

        String texto = etiquetaDia(bloque.getDiaSemana()) + " de "
                + bloque.getHoraInicio().toString().substring(0, 5) + " a "
                + bloque.getHoraFin().toString().substring(0, 5) + "hs";

        venta.setHorarioRetiroElegido(texto);
        venta.setEstadoEnvio(EstadoEnvio.CONFIRMADO);
        ventaRepository.save(venta);

        chatService.abrirChatConMensaje(venta.getComprador(), venta.getRescatista(),
                "Elegiste retirar: " + texto + ". Coordiná el horario exacto con el vendedor por este chat.");

        return toEnvioPendienteResponse(venta);
    }

    @Transactional
    public EnvioPendienteResponse completarDomicilio(Long ventaId, User comprador, EnvioDomicilioRequest request) {
        Venta venta = obtenerVentaDelComprador(ventaId, comprador);
        if (venta.getEstadoEnvio() != EstadoEnvio.PENDIENTE_DOMICILIO) {
            throw new IllegalArgumentException("Esta compra no está esperando un domicilio");
        }

        venta.setDomicilioCalle(request.getCalle().trim());
        venta.setDomicilioAltura(blankToNull(request.getAltura()));
        venta.setDomicilioPiso(blankToNull(request.getPiso()));
        venta.setDomicilioDepto(blankToNull(request.getDepto()));
        venta.setDomicilioDescripcion(blankToNull(request.getDescripcion()));
        venta.setEstadoEnvio(EstadoEnvio.CONFIRMADO);
        ventaRepository.save(venta);

        StringBuilder domicilio = new StringBuilder(venta.getDomicilioCalle());
        if (venta.getDomicilioAltura() != null) domicilio.append(" ").append(venta.getDomicilioAltura());
        if (venta.getDomicilioPiso() != null) domicilio.append(", piso ").append(venta.getDomicilioPiso());
        if (venta.getDomicilioDepto() != null) domicilio.append(", depto ").append(venta.getDomicilioDepto());

        chatService.abrirChatConMensaje(venta.getComprador(), venta.getRescatista(),
                "Domicilio confirmado: " + domicilio
                        + (venta.getDomicilioDescripcion() != null ? " (" + venta.getDomicilioDescripcion() + ")" : "")
                        + ".");

        return toEnvioPendienteResponse(venta);
    }

    private Venta obtenerVentaDelComprador(Long ventaId, User comprador) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada"));
        if (!venta.getComprador().getId().equals(comprador.getId())) {
            throw new IllegalArgumentException("No tenés permiso sobre esta venta");
        }
        return venta;
    }

    private EnvioPendienteResponse toEnvioPendienteResponse(Venta venta) {
        boolean retiroDisponible = venta.getEstadoEnvio() == EstadoEnvio.PENDIENTE_METODO
                && disponibilidadRescatistaRepository.existsByRescatistaId(venta.getRescatista().getId());

        List<DisponibilidadRescatistaResponse> bloques = venta.getEstadoEnvio() == EstadoEnvio.PENDIENTE_HORARIO
                ? disponibilidadRescatistaRepository.findByRescatistaId(venta.getRescatista().getId()).stream()
                        .map(d -> DisponibilidadRescatistaResponse.builder()
                                .id(d.getId())
                                .diaSemana(d.getDiaSemana())
                                .horaInicio(d.getHoraInicio())
                                .horaFin(d.getHoraFin())
                                .build())
                        .toList()
                : List.of();

        return EnvioPendienteResponse.builder()
                .ventaId(venta.getId())
                .estadoEnvio(venta.getEstadoEnvio())
                .metodoEnvio(venta.getMetodoEnvio())
                .retiroDisponible(retiroDisponible)
                .bloquesRetiro(bloques)
                .build();
    }

    private String etiquetaMetodo(MetodoEnvio metodo) {
        return switch (metodo) {
            case RETIRO_DOMICILIO -> "retiro por el domicilio del vendedor";
            case ENVIO_MOTO -> "envío en moto en el día";
            case CORREO_ARGENTINO -> "correo argentino";
        };
    }

    private String etiquetaDia(com.adoptar.enums.DiaSemana dia) {
        return switch (dia) {
            case LUNES -> "Lunes";
            case MARTES -> "Martes";
            case MIERCOLES -> "Miércoles";
            case JUEVES -> "Jueves";
            case VIERNES -> "Viernes";
            case SABADO -> "Sábado";
            case DOMINGO -> "Domingo";
        };
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    // Rescatista marca la venta como enviada

    @Transactional
    public void marcarEnviada(Long ventaId, User rescatista) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada"));
        if (!venta.getRescatista().getId().equals(rescatista.getId())) {
            throw new IllegalArgumentException("No tenés permiso sobre esta venta");
        }
        if (venta.getEstado() != EstadoVenta.COMPLETADA) {
            throw new IllegalArgumentException("Solo podés marcar como enviadas las ventas pagadas");
        }
        venta.setEstado(EstadoVenta.ENVIADA);
        ventaRepository.save(venta);

        notificacionService.crear(venta.getComprador(), TipoNotificacion.VENTA_ENVIADA,
                "Tu compra a " + nombreDe(rescatista) + " fue enviada", "/mis-compras");
    }

    @Transactional(readOnly = true)
    public List<VentaHistorialResponse> getMisVentas(User rescatista) {
        return ventaRepository.findByRescatistaAndEstadoInOrderByCreadoEnDesc(
                        rescatista, List.of(EstadoVenta.COMPLETADA, EstadoVenta.ENVIADA))
                .stream()
                .map(v -> toHistorialResponse(v, nombreDe(v.getComprador())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VentaHistorialResponse> getMisCompras(User comprador) {
        return ventaRepository.findByCompradorAndEstadoInOrderByCreadoEnDesc(
                        comprador, List.of(EstadoVenta.COMPLETADA, EstadoVenta.ENVIADA))
                .stream()
                .map(v -> toHistorialResponse(v, nombreDe(v.getRescatista())))
                .toList();
    }

    private VentaHistorialResponse toHistorialResponse(Venta venta, String otroUsuarioNombre) {
        List<VentaItemResponse> items = venta.getItems().stream()
                .map(vi -> VentaItemResponse.builder()
                        .itemId(vi.getItem().getId())
                        .titulo(vi.getTituloItem())
                        .cantidad(vi.getCantidad())
                        .precioUnitario(vi.getPrecioUnitario())
                        .build())
                .toList();
        return VentaHistorialResponse.builder()
                .id(venta.getId())
                .estado(venta.getEstado())
                .montoTotal(venta.getMontoTotal())
                .creadoEn(venta.getCreadoEn())
                .otroUsuarioNombre(otroUsuarioNombre)
                .items(items)
                .build();
    }

    private String nombreDe(User user) {
        return user.getOrganizacion() != null
                ? user.getOrganizacion()
                : user.getNombre() + " " + user.getApellido();
    }
}
