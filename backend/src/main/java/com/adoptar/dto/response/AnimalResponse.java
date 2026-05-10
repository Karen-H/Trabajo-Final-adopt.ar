package com.adoptar.dto.response;

import com.adoptar.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AnimalResponse {

    private Long id;
    private CategoriaAnimal categoria;
    private String nombre;
    private SexoAnimal sexo;
    private RangoEdad edad;
    private TipoAnimal tipo;
    private TipoAdopcion tipoAdopcion;
    private EstadoAnimal estado;
    private Boolean amigableConGatos;
    private Boolean amigableConPerros;
    private Boolean amigableConNinos;
    private String descripcion;
    private String provincia;
    private String ciudad;
    private String rescatistaNombre;
    // campos solo para reportes
    private String direccion;
    private Double latitud;
    private Double longitud;
    private Boolean enPosesionDelPublicador;
    private LocalDate fechaAvistamiento;
    private List<FotoResponse> fotos;
    private boolean aprobado;
    private boolean rechazado;
    private String motivoRechazo;
    private LocalDateTime creadoEn;
}
