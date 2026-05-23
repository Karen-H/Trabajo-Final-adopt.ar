package com.adoptar.dto.response;

import com.adoptar.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class UsuarioAdminResponse {

    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private UserRole role;
    private LocalDateTime createdAt;
}
