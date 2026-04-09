package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipos_documento")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 20, unique = true)
    private String prefijo;

    @Column(name = "orden_visualizacion")
    private Integer orden;

    @Column(length = 5)
    private String esFormato;

    @Column(length = 50)
    private String marcaAgua;

    @Column(length = 100)
    private String plantilla;

    @Column(length = 5)
    private String esCaracterizacion;
}