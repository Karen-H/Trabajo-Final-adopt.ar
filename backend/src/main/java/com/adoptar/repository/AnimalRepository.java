package com.adoptar.repository;

import com.adoptar.entity.Animal;
import com.adoptar.entity.User;
import com.adoptar.enums.CategoriaAnimal;
import com.adoptar.enums.EstadoAnimal;
import com.adoptar.enums.RangoEdad;
import com.adoptar.enums.SexoAnimal;
import com.adoptar.enums.TipoAdopcion;
import com.adoptar.enums.TipoAnimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

    List<Animal> findByPublicador(User publicador);

    // animales de adopción que el admin todavía no reviewó
    List<Animal> findByCategoriaAndAprobadoFalseAndRechazadoFalse(CategoriaAnimal categoria);

    // reportes por estado (para vista pública perdidos/encontrados)
    List<Animal> findByCategoriaAndAprobadoTrueAndEstado(CategoriaAnimal categoria, EstadoAnimal estado);

    // búsqueda pública de adopción: solo aprobados, con filtros opcionales
    @Query("SELECT a FROM Animal a JOIN a.publicador r WHERE a.categoria = 'ADOPCION' AND a.aprobado = true " +
           "AND (:tipo IS NULL OR a.tipo = :tipo) " +
           "AND (:sexo IS NULL OR a.sexo = :sexo) " +
           "AND (:edad IS NULL OR a.edad = :edad) " +
           "AND (:tipoAdopcion IS NULL OR a.tipoAdopcion = :tipoAdopcion) " +
           "AND (:provincia IS NULL OR r.provincia = :provincia)")
    List<Animal> buscarAdopcionAprobados(
            @Param("tipo") TipoAnimal tipo,
            @Param("sexo") SexoAnimal sexo,
            @Param("edad") RangoEdad edad,
            @Param("tipoAdopcion") TipoAdopcion tipoAdopcion,
            @Param("provincia") String provincia);
}
