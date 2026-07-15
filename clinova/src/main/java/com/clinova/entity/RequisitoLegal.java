package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "requisito_legal")
public class RequisitoLegal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String tipo;
    private String nombre;
    private String anioPublicacion;
    private String emisor;
    private String articulos;
    private String descripcion;
    private String evidenciaAplicacion;
    private String tema;
    private String responsable;
    private String procesoResponsables;
    private String frecuenciaRevision;
    private String estado;
    private String calificacion;
    private String vencimiento;
    private String urlArchivo;
}
