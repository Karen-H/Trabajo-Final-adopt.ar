package com.adoptar.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfileRequest {

    @Email(message = "El email no tiene un formato válido")
    private String email;

    private String tel;

    private String organizacion;
}
