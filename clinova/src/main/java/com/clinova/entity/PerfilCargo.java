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

    @Column(columnDefinition = "TEXT")
    private String mision;

    @Column(columnDefinition = "TEXT")
    private String responsabilidades;

    @Column(columnDefinition = "TEXT")
    private String requisitosEducacion;

    @Column(columnDefinition = "TEXT")
    private String requisitosExperiencia;

    private String estado = "ACTIVO";
}