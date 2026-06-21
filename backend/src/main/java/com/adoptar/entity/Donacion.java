package com.adoptar.entity;

import com.adoptar.enums.EstadoDonacion;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "donaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // null si donó sin cuenta registrada
    @ManyToOne
    @JoinColumn(name = "donante_id")
    private User donante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rescatista_id", nullable = false)
    private User rescatista;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoDonacion estado = EstadoDonacion.PENDIENTE;

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
}
