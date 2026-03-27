package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "plantillas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plantilla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    private String descripcion;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenidoHtml;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }
}