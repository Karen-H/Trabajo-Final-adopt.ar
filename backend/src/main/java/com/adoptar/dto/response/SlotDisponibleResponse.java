package com.adoptar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
@AllArgsConstructor
public class SlotDisponibleResponse {

    private LocalDate fecha;
    private LocalTime hora;
}
