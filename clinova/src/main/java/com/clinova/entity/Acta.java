package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "actas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Acta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private String estado;

    @Column(nullable = false)
    private String responsable;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenidoHtml;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }
}