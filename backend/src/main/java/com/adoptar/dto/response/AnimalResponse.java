package com.adoptar.dto.response;

import com.adoptar.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AnimalResponse {

    private Long id;
    private String nombre;
    private SexoAnimal sexo;
    private RangoEdad edad;
    private TipoAnimal tipo;
    private TipoAdopcion tipoAdopcion;
    private EstadoAnimal estado;
    private boolean amigableConGatos;
    private boolean amigableConPerros;
    private boolean amigableConNinos;
    private String descripcion;
    private String provincia;
    private String ciudad;
    private String rescatistaNombre;
    private List<FotoResponse> fotos;
    private boolean aprobado;
    private boolean rechazado;
    private String motivoRechazo;
    private LocalDateTime creadoEn;
}
