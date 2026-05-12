package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "curso_maestro")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CursoMaestro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    private String descripcion;

    @Builder.Default
    @Column(name = "meses_vigencia", nullable = false)
    private Integer mesesVigencia = 12;

    @Builder.Default
    @Column(name = "es_global", nullable = false)
    private Boolean esGlobal = true;

    @Column(name = "fecha_limite_global")
    private LocalDate fechaLimiteGlobal;
}