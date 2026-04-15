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

    @Column(nullable = false)
    private String detalleDosis;

    @Column(nullable = false)
    private LocalDate fechaAplicacion;
}