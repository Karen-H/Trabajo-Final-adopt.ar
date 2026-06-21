package com.adoptar.enums;

public enum EstadoEnvio {
    PENDIENTE_METODO,    // esperando que el comprador elija como recibir el pedido
    PENDIENTE_HORARIO,   // retiro en domicilio, esperando que elija dia/horario
    PENDIENTE_DOMICILIO, // envio (moto o correo), esperando que complete su domicilio
    CONFIRMADO           // el comprador ya eligio todo lo necesario
}
