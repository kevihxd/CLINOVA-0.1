package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vacuna")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vacuna {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String perfil;

    @Column(name = "dosis_requeridas")
    private Integer dosisRequeridas;

    @Column(name = "requiere_refuerzo")
    private Boolean requiereRefuerzo;
}