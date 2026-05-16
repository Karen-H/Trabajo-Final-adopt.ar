package com.adoptar.repository;

import com.adoptar.entity.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    List<Favorito> findByUsuarioId(Long usuarioId);

    Optional<Favorito> findByUsuarioIdAndAnimalId(Long usuarioId, Long animalId);

    boolean existsByUsuarioIdAndAnimalId(Long usuarioId, Long animalId);

    void deleteByUsuarioIdAndAnimalId(Long usuarioId, Long animalId);
}
