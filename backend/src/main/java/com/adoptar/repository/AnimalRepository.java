package com.adoptar.repository;

import com.adoptar.entity.Animal;
import com.adoptar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

    List<Animal> findByRescatista(User rescatista);

    // animales nuevos que el admin todavía no reviewó
    List<Animal> findByAprobadoFalseAndRechazadoFalse();
}
