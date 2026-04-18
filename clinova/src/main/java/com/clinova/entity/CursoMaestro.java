package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cursos_maestros")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursoMaestro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    private String lugarRealizacion;
}