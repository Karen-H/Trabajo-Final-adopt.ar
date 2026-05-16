package com.adoptar.repository;

import com.adoptar.entity.DisponibilidadAdmin;
import com.adoptar.enums.DiaSemana;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisponibilidadAdminRepository extends JpaRepository<DisponibilidadAdmin, Long> {

    List<DisponibilidadAdmin> findByAdminId(Long adminId);

    // para generar slots de todos los admins dado un dia de la semana
    List<DisponibilidadAdmin> findByDiaSemana(DiaSemana diaSemana);

    boolean existsByIdAndAdminId(Long id, Long adminId);
}
