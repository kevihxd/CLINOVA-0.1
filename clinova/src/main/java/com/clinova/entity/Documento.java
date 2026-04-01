package com.clinova.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "documentos")
public class Documento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo;
    private String version;
    private String nombre;
    private String tipo;
    private String proceso;
    private String ubicacion;
    private String sede;
    private String alcance;
    private String confidencialidad;
    private String metodoCreacion;
    private Integer mesesRevision;
    private String estado = "VIGENTE";
}