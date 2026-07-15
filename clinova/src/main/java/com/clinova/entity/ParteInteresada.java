package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "parte_interesada")
public class ParteInteresada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String tipo;
    private String nombre;
    private String impacto;
    private String calificacion;
    private String requisito;
    private String fechaCreacion;
}
