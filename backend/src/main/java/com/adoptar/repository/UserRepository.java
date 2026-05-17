package com.adoptar.repository;

import com.adoptar.entity.User;
import com.adoptar.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByDni(Long dni);

    boolean existsByTel(String tel);

    List<User> findByRole(UserRole role);
}
