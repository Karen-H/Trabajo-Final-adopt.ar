package com.adoptar.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"adoptante_id", "rescatista_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "adoptante_id", nullable = false)
    private User adoptante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "rescatista_id", nullable = false)
    private User rescatista;

    // animales que el adoptante mencionó en este chat
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "chat_animales",
            joinColumns = @JoinColumn(name = "chat_id"),
            inverseJoinColumns = @JoinColumn(name = "animal_id")
    )
    @Builder.Default
    private List<Animal> animales = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
