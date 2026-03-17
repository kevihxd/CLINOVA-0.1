package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "experiencia_laboral")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienciaLaboral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String empresa;

    @Column(nullable = false, length = 100)
    private String cargo;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    private LocalDate fechaFin;
    @Column(length = 500)
    private String funciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoja_vida_id", nullable = false)
    private HojaVida hojaVida;
}