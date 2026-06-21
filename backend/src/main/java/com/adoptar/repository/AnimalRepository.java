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

import java.time.LocalDateTime;
import java.util.List;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

    List<Animal> findByPublicador(User publicador);

    List<Animal> findByPublicadorAndCategoriaAndEliminadoPermanenteFalse(User publicador, CategoriaAnimal categoria);

    // reportes por estado para vista pública (excluye eliminados)
    List<Animal> findByCategoriaAndAprobadoTrueAndEstadoAndEliminadoFalse(CategoriaAnimal categoria, EstadoAnimal estado);

    // búsqueda pública de adopción: solo aprobados y no eliminados, con filtros opcionales
    @Query("SELECT a FROM Animal a JOIN a.publicador r WHERE a.categoria = 'ADOPCION' AND a.aprobado = true AND a.eliminado = false AND a.estado = 'EN_ADOPCION' " +
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

    // dashboard

    // total adoptados histórico
    long countByCategoriaAndEstado(CategoriaAnimal categoria, EstadoAnimal estado);

    // activos aprobados por categoría y estado (para conteos de perdidos/encontrados/en adopción)
    @Query("SELECT COUNT(a) FROM Animal a WHERE a.categoria = :cat AND a.aprobado = true AND a.eliminado = false AND a.estado = :estado")
    long countActivosByCategoriaAndEstado(@Param("cat") CategoriaAnimal cat, @Param("estado") EstadoAnimal estado);

    @Query("SELECT COUNT(a) FROM Animal a WHERE a.categoria = 'ADOPCION' AND a.aprobado = true AND a.eliminado = false AND a.estado = 'EN_ADOPCION'")
    long countEnAdopcionActivos();

    @Query("SELECT COUNT(a) FROM Animal a WHERE a.categoria = 'PERDIDO_ENCONTRADO' AND a.aprobado = true AND a.eliminado = false AND a.estado = 'PERDIDO'")
    long countPerdidosActivos();

    @Query("SELECT COUNT(a) FROM Animal a WHERE a.categoria = 'PERDIDO_ENCONTRADO' AND a.aprobado = true AND a.eliminado = false AND a.estado = 'ENCONTRADO'")
    long countEncontradosActivos();

    // activos en adopción por tipo de adopción
    @Query("SELECT COUNT(a) FROM Animal a WHERE a.categoria = 'ADOPCION' AND a.aprobado = true AND a.eliminado = false AND a.estado = 'EN_ADOPCION' AND a.tipoAdopcion = :tipo")
    long countActivosByTipoAdopcion(@Param("tipo") TipoAdopcion tipo);

    // adoptados por mes (último año)
    @Query("SELECT YEAR(a.adoptadoEn), MONTH(a.adoptadoEn), COUNT(a) FROM Animal a " +
           "WHERE a.categoria = 'ADOPCION' AND a.estado = 'ADOPTADO' AND a.adoptadoEn >= :desde " +
           "GROUP BY YEAR(a.adoptadoEn), MONTH(a.adoptadoEn) " +
           "ORDER BY YEAR(a.adoptadoEn), MONTH(a.adoptadoEn)")
    List<Object[]> countAdoptadosPorMes(@Param("desde") LocalDateTime desde);

    // total histórico por especie (solo adopciones, excluye eliminados)
    @Query("SELECT a.tipo, COUNT(a) FROM Animal a WHERE a.categoria = 'ADOPCION' AND a.eliminado = false GROUP BY a.tipo")
    List<Object[]> countByTipoHistorico();

    // actualmente publicados por especie
    @Query("SELECT a.tipo, COUNT(a) FROM Animal a WHERE a.categoria = 'ADOPCION' AND a.eliminado = false AND a.aprobado = true AND a.estado = 'EN_ADOPCION' GROUP BY a.tipo")
    List<Object[]> countPublicadosByTipo();

    // tasa de éxito

    @Query("SELECT COUNT(a) FROM Animal a WHERE a.categoria = 'ADOPCION'")
    long countTotalHistoricoAdopcion();

    @Query("SELECT COUNT(a) FROM Animal a WHERE a.estadoInicial = 'PERDIDO'")
    long countTotalHistoricoPerdidos();

    @Query("SELECT COUNT(a) FROM Animal a WHERE a.estadoInicial = 'ENCONTRADO'")
    long countTotalHistoricoEncontrados();

    @Query("SELECT COUNT(a) FROM Animal a WHERE a.estadoInicial = 'PERDIDO' AND a.estado = 'RESUELTO'")
    long countResueltosPerdidos();

    @Query("SELECT COUNT(a) FROM Animal a WHERE a.estadoInicial = 'ENCONTRADO' AND a.estado = 'RESUELTO'")
    long countResueltosEncontrados();

    @Query("SELECT COUNT(a) FROM Animal a WHERE a.eliminado = true")
    long countEliminados();
}
