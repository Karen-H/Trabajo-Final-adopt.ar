package com.adoptar.repository;

import com.adoptar.entity.AnimalFoto;
import com.adoptar.enums.EstadoFoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnimalFotoRepository extends JpaRepository<AnimalFoto, Long> {

    // fotos pendientes en animales ya aprobados (para la sección de fotos del panel admin)
    List<AnimalFoto> findByEstadoAndAnimal_Aprobado(EstadoFoto estado, boolean aprobado);
}
