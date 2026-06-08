package com.adoptar.repository;

import com.adoptar.entity.Animal;
import com.adoptar.entity.Reserva;
import com.adoptar.entity.User;
import com.adoptar.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    // reservas pendientes de un adoptante con un rescatista
    List<Reserva> findByAdoptanteAndRescatistaAndEstadoIn(User adoptante, User rescatista, List<EstadoReserva> estados);

    // reserva pendiente o activa de un animal
    Optional<Reserva> findByAnimalAndEstadoIn(Animal animal, List<EstadoReserva> estados);

    // reservas del rescatista
    List<Reserva> findByRescatistaOrderByCreadoEnDesc(User rescatista);

    // reservas del adoptante
    List<Reserva> findByAdoptanteOrderByCreadoEnDesc(User adoptante);

    // reservas pendientes/activas por rescatista
    List<Reserva> findByRescatistaAndEstadoIn(User rescatista, List<EstadoReserva> estados);

    // reservas pendientes/activas por adoptante
    List<Reserva> findByAdoptanteAndEstadoIn(User adoptante, List<EstadoReserva> estados);
}
