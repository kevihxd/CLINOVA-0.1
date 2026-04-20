package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "registro_vacuna")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroVacuna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vacuna_id", nullable = false)
    private Vacuna vacuna;

    @Column(name = "detalle_dosis", length = 50)
    private String detalleDosis;

    @Column(name = "fecha_aplicacion", nullable = false)
    private LocalDate fechaAplicacion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;
}