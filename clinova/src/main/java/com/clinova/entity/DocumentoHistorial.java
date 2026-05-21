package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_historial")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accion;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    @Column(nullable = false)
    private String usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "documento_id")
    private Long documentoId;

    @PrePersist
    protected void onCreate() {
        this.fecha = LocalDateTime.now();
    }
}
