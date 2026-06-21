package com.adoptar.repository;

import com.adoptar.entity.DisponibilidadRescatista;
import com.adoptar.enums.DiaSemana;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisponibilidadRescatistaRepository extends JpaRepository<DisponibilidadRescatista, Long> {

    List<DisponibilidadRescatista> findByRescatistaId(Long rescatistaId);

    List<DisponibilidadRescatista> findByRescatistaIdAndDiaSemana(Long rescatistaId, DiaSemana diaSemana);

    boolean existsByIdAndRescatistaId(Long id, Long rescatistaId);

    boolean existsByRescatistaId(Long rescatistaId);
}
