package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "perfiles_cargo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilCargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "cargo_id", nullable = false, unique = true)
    private Cargo cargo;

    @Column(length = 255)
    private String jefeInmediato;

    @Column(length = 50)
    private String version = "1";

    @Column(length = 100)
    private String fecha;

    @Column(columnDefinition = "LONGTEXT")
    private String mision;

    @Column(columnDefinition = "LONGTEXT")
    private String responsabilidades;

    @Column(columnDefinition = "LONGTEXT")
    private String requisitosEducacion;

    @Column(columnDefinition = "LONGTEXT")
    private String requisitosFormacion;

    @Column(columnDefinition = "LONGTEXT")
    private String requisitosHabilidades;

    @Column(columnDefinition = "LONGTEXT")
    private String requisitosExperiencia;

    @Column(columnDefinition = "LONGTEXT")
    private String versiones;

    private String estado = "ACTIVO";
}