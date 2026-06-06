package com.adoptar.entity;

import com.adoptar.enums.RazonDenuncia;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "denuncias", uniqueConstraints = @UniqueConstraint(columnNames = {"animal_id", "denunciante_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Denuncia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "denunciante_id", nullable = false)
    private User denunciante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RazonDenuncia razon;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    @Builder.Default
    private boolean resuelto = false;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
