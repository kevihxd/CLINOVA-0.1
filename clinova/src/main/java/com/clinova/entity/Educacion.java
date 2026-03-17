package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "educacion")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Educacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nivelEstudio;

    @Column(nullable = false, length = 150)
    private String institucion;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    private LocalDate fechaFin;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoja_vida_id", nullable = false)
    private HojaVida hojaVida;
}