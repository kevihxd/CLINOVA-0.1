package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "actas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Acta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kawak_id", unique = true)
    private Long kawakId;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private String tipo;

    @Column(nullable = false)
    private String estado;

    @Column(nullable = false)
    private String responsable;

    @Column
    private String proceso;

    @Column
    private String sede;

    @Column
    private String fechaInicio;

    @Column
    private String horaInicio;

    @Column
    private String fechaFin;

    @Column
    private String horaFin;

    @Column
    private String lugar;

    @Column
    private String enlaceVirtual;

    @Column
    private String quienCita;

    @Column
    private Boolean confidencial;

    @Column
    private String elaborador;

    @Column
    private String area;

    @Column
    private String palabrasClave;

    @Column
    private String compromisosAprobacion;

    @Column
    private String convertirDocumento;

    @Column
    private String requiereAprobacionActa;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenidoHtml;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
    }
}