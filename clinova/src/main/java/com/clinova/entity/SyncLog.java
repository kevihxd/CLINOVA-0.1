package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sync_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String modulo;

    @Column(nullable = false, length = 20)
    private String estado; // EXITOSO, FALLIDO, PARCIAL

    @Column(name = "registros_sincronizados")
    private Integer registrosSincronizados;

    @Column(name = "registros_creados")
    private Integer registrosCreados;

    @Column(name = "registros_actualizados")
    private Integer registrosActualizados;

    @Column(name = "error_mensaje", columnDefinition = "TEXT")
    private String errorMensaje;

    @Column(name = "fecha_ejecucion", nullable = false)
    private LocalDateTime fechaEjecucion;

    @Column(name = "duracion_ms")
    private Long duracionMs;

    @PrePersist
    protected void onCreate() {
        fechaEjecucion = LocalDateTime.now();
    }
}
