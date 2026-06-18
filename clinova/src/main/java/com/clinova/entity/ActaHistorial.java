package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "actas_historial")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActaHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accion; // CREACION, MODIFICACION, CAMBIO_ESTADO, COMENTARIO, ELIMINACION

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private String usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "acta_id")
    private Long actaId;

    @PrePersist
    protected void onCreate() {
        this.fecha = LocalDateTime.now();
    }
}
