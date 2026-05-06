package com.clinova.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "hojas_vida")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HojaVida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 20)
    private String cedula;

    @Column
    private LocalDate fechaNacimiento;

    @Column(length = 200)
    private String direccionResidencia;

    @Column(length = 50)
    private String telefono;

    @Column(length = 100)
    private String contactoEmergencia;

    @Column(length = 50)
    private String telefonoContactoEmergencia;

    @Column(length = 100)
    private String arl;

    @Column(length = 100)
    private String eps;

    @Column(length = 100)
    private String afp;

    @Column(length = 100)
    private String cajaCompensacion;

    private Double salario;

    @Column(length = 20)
    private String subsidioTransporte;

    @Column(length = 500)
    private String fotoUrl;

    @Column(nullable = false)
    private LocalDate fechaIngreso;

    @Column(length = 50)
    private String estado;

    @Column(length = 100)
    private String tipoContrato;

    private LocalDate fechaRetiro;

    @Column(length = 250)
    private String motivoRetiro;

    @Column(length = 150)
    private String correoElectronico;

    @Column(length = 50)
    private String pesv;

    @Column(name = "responsable_evaluacion_id")
    private Long responsableEvaluacionId;

    @Column(name = "fecha_ultima_edicion")
    private LocalDateTime fechaUltimaEdicion;

    @Column(name = "usuario_ultima_edicion", length = 150)
    private String usuarioUltimaEdicion;

    // --- CAMPOS NUEVOS PARA VACUNACIÓN ---
    @Column(name = "perfil_vacunacion", length = 50)
    private String perfilVacunacion;

    @Column(name = "detalle_vacunas", columnDefinition = "TEXT")
    private String detalleVacunas;

    // --- CAMPO REQUERIDO PARA EVITAR EL ERROR getUsuarioId() ---
    @Transient
    private Long usuarioId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", unique = true)
    private Usuario usuario;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "hojas_vida_cargos",
            joinColumns = @JoinColumn(name = "hoja_vida_id"),
            inverseJoinColumns = @JoinColumn(name = "cargo_id")
    )
    private List<Cargo> cargos;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "hojas_vida_sedes",
            joinColumns = @JoinColumn(name = "hoja_vida_id"),
            inverseJoinColumns = @JoinColumn(name = "sede_id")
    )
    private List<Sede> sedes;
}