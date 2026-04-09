package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "documentos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String tipo;
    private String proceso;
    private String ubicacion;
    private String sede;
    private Integer mesesRevision;
    private String alcance;
    private String confidencialidad;
    private String metodoCreacion;
    private String codigo;
    private String version;
    private String estado;
    private String plantilla;


    @Column(length = 1000) private String elabora;
    @Column(length = 1000) private String revisa;
    @Column(length = 1000) private String aprueba;
    @Column(length = 1000) private String visualizacion;
    @Column(length = 1000) private String impresion;
    @Column(length = 1000) private String descargaOriginal;
    @Column(length = 1000) private String descargaPdf;
    @Column(length = 1000) private String otrosProcesos;
    @Column(length = 1000) private String normas;
}