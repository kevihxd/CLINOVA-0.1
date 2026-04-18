package com.clinova.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cursos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String institucion;
    private String fechaFinalizacion;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "soporte_id")
    private Soporte certificado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoja_vida_id")
    @JsonIgnore
    private HojaVida hojaVida;
}