package com.adoptar.repository;

import com.adoptar.entity.Chat;
import com.adoptar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    Optional<Chat> findByAdoptanteAndRescatista(User adoptante, User rescatista);

    @Query("SELECT c FROM Chat c WHERE c.adoptante = :user OR c.rescatista = :user ORDER BY c.creadoEn DESC")
    List<Chat> findByParticipante(@Param("user") User user);

    @Query("SELECT COUNT(DISTINCT c) FROM Chat c JOIN c.animales a WHERE a.id = :animalId")
    long countByAnimalId(@Param("animalId") Long animalId);
}
