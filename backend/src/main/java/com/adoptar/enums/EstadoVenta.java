package com.adoptar.enums;

public enum EstadoVenta {
    PENDIENTE,   // se creo la preferencia de pago, esperando confirmacion de MP
    COMPLETADA,  // pago aprobado, pendiente de envio
    FALLIDA,     // pago rechazado o cancelado
    ENVIADA      // el rescatista marco la venta como enviada
}
