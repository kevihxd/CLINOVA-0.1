package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "analisis_contexto")
public class AnalisisContexto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nombre;
    private String fechaCreacion;
    private String fechaModificacion;
    private String rutaArchivo;
}
