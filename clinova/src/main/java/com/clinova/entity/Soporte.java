package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "soportes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Soporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String tipoDocumento;

    @Column(nullable = false, length = 255)
    private String nombreArchivo;

    @Column(nullable = false, length = 500)
    private String rutaArchivo;

    @Column(nullable = false)
    private Long tamano;

    @Column(nullable = false)
    private LocalDateTime fechaCarga;

    @Column(length = 50)
    private String estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoja_vida_id", nullable = false)
    private HojaVida hojaVida;
}