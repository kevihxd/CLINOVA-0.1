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

    @Column(unique = true, nullable = false)
    private String nombre;
}