package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "documentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kawak_id", unique = true)
    private Long kawakId;

    private String alcance;
    private String codigo;
    private String confidencialidad;
    private String estado;

    @Column(name = "meses_revision")
    private Integer mesesRevision;

    @Column(name = "metodo_creacion")
    private String metodoCreacion;

    private String nombre;
    private String proceso;
    private String sede;
    private String tipo;
    private String ubicacion;
    private String version;

    @Column(length = 1000)
    private String aprueba;

    @Column(name = "descarga_original", length = 1000)
    private String descargaOriginal;

    @Column(name = "descarga_pdf", length = 1000)
    private String descargaPdf;

    @Column(length = 1000)
    private String elabora;

    @Column(length = 1000)
    private String impresion;

    @Column(length = 1000)
    private String normas;

    @Column(name = "otros_procesos", length = 1000)
    private String otrosProcesos;

    private String plantilla;

    @Column(length = 1000)
    private String revisa;

    @Column(length = 1000)
    private String visualizacion;
}