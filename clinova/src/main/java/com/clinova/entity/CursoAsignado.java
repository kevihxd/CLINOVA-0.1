package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "curso_asignado")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CursoAsignado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_maestro_id", nullable = false)
    private CursoMaestro cursoMaestro;

    @Column(name = "certificado_url")
    private String certificadoUrl;

    private LocalDate fechaRealizacion;
    private LocalDate fechaExpiracion;
    private String estado; // PENDIENTE, COMPLETADO
}