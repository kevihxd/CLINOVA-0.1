package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categorias_soporte")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaSoporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean visible = true;
}