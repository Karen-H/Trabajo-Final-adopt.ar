package com.adoptar.repository;

import com.adoptar.entity.User;
import com.adoptar.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByOrganizacion(String organizacion);

    boolean existsByDni(Long dni);

    boolean existsByTel(String tel);

    List<User> findByRole(UserRole role);

    List<User> findByTieneTiendaTrue();

    List<User> findByAceptaDonacionesTrue();

    // nuevos usuarios por mes (último año)
    @Query("SELECT YEAR(u.createdAt), MONTH(u.createdAt), COUNT(u) FROM User u WHERE u.createdAt >= :desde " +
           "GROUP BY YEAR(u.createdAt), MONTH(u.createdAt) ORDER BY YEAR(u.createdAt), MONTH(u.createdAt)")
    List<Object[]> countUsuariosPorMes(@Param("desde") LocalDateTime desde);
}
