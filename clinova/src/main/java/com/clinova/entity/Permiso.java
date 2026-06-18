package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permisos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    private String descripcion;

    @Column(length = 50)
    private String modulo;

    @Column(length = 20)
    private String accion; // CREAR, MODIFICAR, ELIMINAR, VER
}