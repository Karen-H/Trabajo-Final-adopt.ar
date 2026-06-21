package com.adoptar.enums;

public enum TipoNotificacion {
    // para admin/mod
    NUEVA_DENUNCIA,
    PUBLICACION_PENDIENTE,
    NUEVA_SOLICITUD_TIENDA,
    // para rescatista (publicaciones)
    PUBLICACION_APROBADA,
    PUBLICACION_RECHAZADA,
    // para rescatista (tienda)
    SOLICITUD_TIENDA_ACEPTADA,
    SOLICITUD_TIENDA_RECHAZADA,
    SOLICITUD_TIENDA_REPROGRAMADA,
    TIENDA_APROBADA,
    RECORDATORIO_LLAMADA_7,
    RECORDATORIO_LLAMADA_3,
    RECORDATORIO_LLAMADA_1,
    // para admin (rescatista reprogramó)
    SOLICITUD_RESCATISTA_REPROGRAMADA,
    // para adoptante
    RESERVA_PROPUESTA,
    RESERVA_CANCELADA,
    // para rescatista (reservas)
    RESERVA_ACEPTADA,
    RESERVA_RECHAZADA,
    // para cualquier usuario con favoritos
    ANIMAL_FAVORITO_NO_DISPONIBLE,
    // para el receptor de un mensaje
    NUEVO_MENSAJE,
    // para rescatista (donaciones)
    NUEVA_DONACION,
    // para rescatista (ventas de items de tienda)
    NUEVA_VENTA,
    // para el comprador, cuando el rescatista despacha la compra
    VENTA_ENVIADA
}
