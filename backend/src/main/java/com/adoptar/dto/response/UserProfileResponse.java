package com.adoptar.dto.response;

import com.adoptar.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String nombre;
    private String apellido;
    private Long dni;
    private String email;
    private String tel;
    private String organizacion;
    private UserRole role;
    private LocalDateTime createdAt;
}
