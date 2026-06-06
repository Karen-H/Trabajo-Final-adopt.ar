package com.adoptar.repository;

import com.adoptar.entity.Donacion;
import com.adoptar.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DonacionRepository extends JpaRepository<Donacion, Long> {

    List<Donacion> findByRescatistaOrderByCreadoEnDesc(User rescatista);

    Optional<Donacion> findByMpPreferenceId(String mpPreferenceId);
}
