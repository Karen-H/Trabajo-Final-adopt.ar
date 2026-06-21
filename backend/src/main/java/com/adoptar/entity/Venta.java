package com.adoptar.entity;

import com.adoptar.enums.EstadoEnvio;
import com.adoptar.enums.EstadoVenta;
import com.adoptar.enums.MetodoEnvio;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "comprador_id", nullable = false)
    private User comprador;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rescatista_id", nullable = false)
    private User rescatista;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VentaItem> items = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montoTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoVenta estado = EstadoVenta.PENDIENTE;

    @Column
    private String mpPreferenceId;

    @Column
    private String mpPaymentId;

    // external_reference para MP, no usar el id porque se recicla
    @Column(unique = true)
    private String externalRef;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    // envio / bot de chat

    @Enumerated(EnumType.STRING)
    private EstadoEnvio estadoEnvio;

    @Enumerated(EnumType.STRING)
    private MetodoEnvio metodoEnvio;

    // retiro en domicilio: bloque dia+horario elegido (texto informativo, ej "Lunes de 09:00 a 18:00hs")
    @Column
    private String horarioRetiroElegido;

    // envio (moto o correo): domicilio del comprador
    @Column
    private String domicilioCalle;

    @Column
    private String domicilioAltura;

    @Column
    private String domicilioPiso;

    @Column
    private String domicilioDepto;

    @Column(columnDefinition = "TEXT")
    private String domicilioDescripcion;
}
