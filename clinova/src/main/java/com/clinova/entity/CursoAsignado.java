package com.clinova.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "cursos_asignados")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursoAsignado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoja_vida_id", nullable = false)
    @JsonIgnore
    private HojaVida hojaVida;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "curso_maestro_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private CursoMaestro cursoMaestro;

    private LocalDate fechaLimite;

    private String estado; // Ejemplo: PENDIENTE, ENTREGADO

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "soporte_id")
    private Soporte certificado;
}