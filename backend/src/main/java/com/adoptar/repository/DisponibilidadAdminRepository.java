package com.adoptar.repository;

import com.adoptar.entity.DisponibilidadAdmin;
import com.adoptar.enums.DiaSemana;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DisponibilidadAdminRepository extends JpaRepository<DisponibilidadAdmin, Long> {

    List<DisponibilidadAdmin> findByAdminId(Long adminId);

    List<DisponibilidadAdmin> findByAdminIdAndDiaSemana(Long adminId, DiaSemana diaSemana);

    // para generar slots de todos los admins dado un dia de la semana
    List<DisponibilidadAdmin> findByDiaSemana(DiaSemana diaSemana);

    boolean existsByIdAndAdminId(Long id, Long adminId);

    // carga todos los bloques junto con el admin en una sola query (evita N+1)
    @Query("SELECT d FROM DisponibilidadAdmin d JOIN FETCH d.admin")
    List<DisponibilidadAdmin> findAllWithAdmin();
}
