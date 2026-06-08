package com.adoptar.repository;

import com.adoptar.entity.Animal;
import com.adoptar.entity.BloqueoAdopcion;
import com.adoptar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BloqueoAdopcionRepository extends JpaRepository<BloqueoAdopcion, Long> {

    Optional<BloqueoAdopcion> findByAdoptanteAndAnimal(User adoptante, Animal animal);

    // IDs de animales bloqueados activos para un adoptante
    @Query("SELECT b.animal.id FROM BloqueoAdopcion b WHERE b.adoptante = :adoptante AND b.bloqueadoHasta > :ahora")
    List<Long> findAnimalIdsBloquedosActivos(@Param("adoptante") User adoptante, @Param("ahora") LocalDateTime ahora);
}
