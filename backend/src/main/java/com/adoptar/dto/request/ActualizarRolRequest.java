package com.adoptar.dto.request;

import com.adoptar.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ActualizarRolRequest {

    @NotNull
    private UserRole rol;
}
