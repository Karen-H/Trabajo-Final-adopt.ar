package com.adoptar.dto.response;

import com.adoptar.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private UserRole role;
}
