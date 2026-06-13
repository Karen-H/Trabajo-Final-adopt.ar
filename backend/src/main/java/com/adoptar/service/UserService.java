package com.adoptar.service;

import com.adoptar.dto.request.UpdateProfileRequest;
import com.adoptar.dto.response.UserProfileResponse;
import com.adoptar.entity.Animal;
import com.adoptar.entity.ItemTienda;
import com.adoptar.entity.Reserva;
import com.adoptar.entity.User;
import com.adoptar.enums.CategoriaAnimal;
import com.adoptar.enums.EstadoReserva;
import com.adoptar.enums.EstadoSolicitudTienda;
import com.adoptar.enums.PreferenciaRol;
import com.adoptar.enums.UserProfile;
import com.adoptar.exception.EmailAlreadyExistsException;
import com.adoptar.exception.TelAlreadyExistsException;
import com.adoptar.repository.AnimalRepository;
import com.adoptar.repository.ItemTiendaRepository;
import com.adoptar.repository.ReservaRepository;
import com.adoptar.repository.SolicitudTiendaRepository;
import com.adoptar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AnimalRepository animalRepository;
    private final ReservaRepository reservaRepository;
    private final ItemTiendaRepository itemTiendaRepository;
    private final SolicitudTiendaRepository solicitudTiendaRepository;

    public UserProfileResponse getProfile(User user) {
        return toResponse(user);
    }

    @Transactional
    public UserProfileResponse switchProfile(User user) {
        if (user.getPreferencia() != PreferenciaRol.AMBOS) {
            throw new RuntimeException("No podés cambiar de perfil con tu configuración actual");
        }
        if (user.getActiveProfile() == UserProfile.ADOPTANTE) {
            user.setActiveProfile(UserProfile.RESCATISTA);
        } else {
            user.setActiveProfile(UserProfile.ADOPTANTE);
        }
        userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(User user, UpdateProfileRequest request) {
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new EmailAlreadyExistsException("Ya existe una cuenta con ese email");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getTel() != null && !request.getTel().isBlank()
                && !request.getTel().equals(user.getTel())) {
            if (userRepository.existsByTel(request.getTel())) {
                throw new TelAlreadyExistsException("Ya existe una cuenta con ese teléfono");
            }
            user.setTel(request.getTel());
        }

        if (request.getOrganizacion() != null) {
            String org = request.getOrganizacion().isBlank() ? null : request.getOrganizacion().trim();
            if (org != null && !org.equals(user.getOrganizacion())) {
                if (userRepository.existsByOrganizacion(org)) {
                    throw new IllegalArgumentException("Ya existe una cuenta con esa organización");
                }
            }
            user.setOrganizacion(org);
        }

        if (request.getProvincia() != null) {
            user.setProvincia(request.getProvincia().isBlank() ? null : request.getProvincia());
        }

        if (request.getCiudad() != null) {
            user.setCiudad(request.getCiudad().isBlank() ? null : request.getCiudad());
        }

        if (request.getPreferencia() != null && request.getPreferencia() != user.getPreferencia()) {
            aplicarCambioPreferencia(user, request.getPreferencia());
        }

        userRepository.save(user);
        return toResponse(user);
    }

    private void aplicarCambioPreferencia(User user, PreferenciaRol nueva) {
        PreferenciaRol vieja = user.getPreferencia();

        // pierde el rol rescatista
        if (nueva == PreferenciaRol.ADOPTANTE && vieja != PreferenciaRol.ADOPTANTE) {
            eliminarContenidoRescatista(user);
            user.setActiveProfile(UserProfile.ADOPTANTE);
        }

        // pierde el rol adoptante
        if (nueva == PreferenciaRol.RESCATISTA && vieja != PreferenciaRol.RESCATISTA) {
            cancelarReservasAdoptante(user);
            user.setActiveProfile(UserProfile.RESCATISTA);
        }

        user.setPreferencia(nueva);
    }

    private void eliminarContenidoRescatista(User user) {
        // eliminar animales en adopción y cancelar sus reservas pendientes/activas
        List<Animal> animales = animalRepository.findByPublicadorAndCategoriaAndEliminadoPermanenteFalse(user, CategoriaAnimal.ADOPCION);
        for (Animal a : animales) {
            a.setEliminadoPermanente(true);
            reservaRepository.findByAnimalAndEstadoIn(a, List.of(EstadoReserva.PENDIENTE, EstadoReserva.ACTIVA))
                    .ifPresent(r -> {
                        r.setEstado(EstadoReserva.CANCELADA);
                        reservaRepository.save(r);
                    });
        }
        animalRepository.saveAll(animales);

        // eliminar items de tienda
        List<ItemTienda> items = itemTiendaRepository.findByRescatistaAndEliminadoFalse(user);
        items.forEach(i -> i.setEliminado(true));
        itemTiendaRepository.saveAll(items);

        // cancelar solicitud de tienda si está pendiente o reprogramada
        solicitudTiendaRepository.findByRescatistaId(user.getId()).ifPresent(s -> {
            if (s.getEstado() == EstadoSolicitudTienda.PENDIENTE
                    || s.getEstado() == EstadoSolicitudTienda.REPROGRAMADA) {
                s.setEstado(EstadoSolicitudTienda.RECHAZADA);
                solicitudTiendaRepository.save(s);
            }
        });

        user.setTieneTienda(false);
        user.setAceptaDonaciones(false);
        user.setDescripcionDonacion(null);
    }

    private void cancelarReservasAdoptante(User user) {
        List<Reserva> reservas = reservaRepository.findByAdoptanteAndEstadoIn(
                user, List.of(EstadoReserva.PENDIENTE, EstadoReserva.ACTIVA));
        reservas.forEach(r -> r.setEstado(EstadoReserva.CANCELADA));
        reservaRepository.saveAll(reservas);
    }

    private UserProfileResponse toResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .nombre(user.getNombre())
                .apellido(user.getApellido())
                .dni(user.getDni())
                .email(user.getEmail())
                .tel(user.getTel())
                .organizacion(user.getOrganizacion())
                .provincia(user.getProvincia())
                .ciudad(user.getCiudad())
                .role(user.getRole())
                .activeProfile(user.getActiveProfile())
                .preferencia(user.getPreferencia())
                .tieneTienda(user.isTieneTienda())
                .aceptaDonaciones(user.isAceptaDonaciones())
                .descripcionDonacion(user.getDescripcionDonacion())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
