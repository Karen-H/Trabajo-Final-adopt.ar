package com.adoptar.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MesCountResponse {
    private String mes; // formato "2025-01"
    private long cantidad;
}
