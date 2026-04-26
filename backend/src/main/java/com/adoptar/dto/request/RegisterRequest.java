package com.adoptar.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    private String apellido;

    @NotNull(message = "El documento es obligatorio")
    @Min(value = 1000000, message = "El DNI debe tener entre 7 y 8 dígitos")
    @Max(value = 99999999, message = "El DNI debe tener entre 7 y 8 dígitos")
    private Long dni;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    private String tel;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String pass;

    private String organizacion;
}
