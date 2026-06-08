package com.adoptar.enums;

public enum EstadoReserva {
    PENDIENTE,    // rescatista propuso, adoptante no respondió
    ACTIVA,       // adoptante aceptó
    CONCRETADA,   // adopción se concretó
    CANCELADA     // se canceló (por cualquiera de las partes)
}
