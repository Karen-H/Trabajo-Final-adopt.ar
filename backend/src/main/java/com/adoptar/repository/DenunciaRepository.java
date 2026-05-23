package com.adoptar.repository;

import com.adoptar.entity.Denuncia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DenunciaRepository extends JpaRepository<Denuncia, Long> {

    boolean existsByAnimalIdAndDenuncianteId(Long animalId, Long denuncianteId);

    List<Denuncia> findByResueltoFalse();

    List<Denuncia> findByAnimalIdAndResueltoFalse(Long animalId);

    void deleteByDenuncianteId(Long denuncianteId);

    void deleteByAnimalIdIn(List<Long> animalIds);
}
