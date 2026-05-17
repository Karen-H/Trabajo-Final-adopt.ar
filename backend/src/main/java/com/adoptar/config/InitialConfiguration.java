package com.adoptar.config;

import com.adoptar.entity.User;
import com.adoptar.enums.UserRole;
import com.adoptar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class InitialConfiguration implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("BD ya inicializada");
            return;
        }

        log.info("Cargando datos iniciales...");
        seedUsers();
        log.info("Datos iniciales cargados correctamente");
    }

    private void seedUsers() {
        userRepository.save(User.builder()
                .nombre("Admin")
                .apellido("Sistema")
                .dni(1000001L)
                .email("admin@adoptar.com")
                .tel("1100000001")
                .pass(passwordEncoder.encode("admin123"))
                .role(UserRole.ADMIN)
                .build());

        userRepository.save(User.builder()
                .nombre("Ingrid")
                .apellido("Usuario")
                .dni(1000002L)
                .email("ingrid@adoptar.com")
                .tel("1100000002")
                .pass(passwordEncoder.encode("admin123"))
                .role(UserRole.USER)
                .build());

        userRepository.save(User.builder()
                .nombre("Karen")
                .apellido("Usuario")
                .dni(1000003L)
                .email("karen@adoptar.com")
                .tel("1100000003")
                .pass(passwordEncoder.encode("admin123"))
                .role(UserRole.USER)
                .build());

        log.info("Usuarios iniciales creados: admin@adoptar.com, ingrid@adoptar.com, karen@adoptar.com / admin123");
    }
}
