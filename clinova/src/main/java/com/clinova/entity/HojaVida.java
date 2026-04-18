package com.clinova.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    @Column(nullable = false, unique = true)
    private String cedula;

    @Column(nullable = false)
    private String nombres;

    @Column(nullable = false)
    private String apellidos;

    private LocalDate fechaNacimiento;
    private String direccionResidencia;
    private String telefono;
    private String correoElectronico;

    private String contactoEmergencia;
    private String telefonoContactoEmergencia;

    private String arl;
    private String eps;
    private String afp;
    private String cajaCompensacion;

    private Double salario;
    private String subsidioTransporte;
    private LocalDate fechaIngreso;
    private String estado;
    private String tipoContrato;
    private LocalDate fechaRetiro;
    private String motivoRetiro;

    private String perfilVacunacion;

    private String pesv;
    private Long responsableEvaluacionId;
    private LocalDateTime fechaUltimaEdicion;
    private String usuarioUltimaEdicion;
    private String fotoUrl;

    @Transient
    private Long usuarioId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @JsonIgnore
    private Usuario usuario;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "hoja_vida_cargo",
            joinColumns = @JoinColumn(name = "hoja_vida_id"),
            inverseJoinColumns = @JoinColumn(name = "cargo_id")
    )
    @JsonIgnoreProperties({"hojasVida", "hibernateLazyInitializer", "handler"})
    private List<Cargo> cargos;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "hoja_vida_sede",
            joinColumns = @JoinColumn(name = "hoja_vida_id"),
            inverseJoinColumns = @JoinColumn(name = "sede_id")
    )
    @JsonIgnoreProperties({"hojasVida", "hibernateLazyInitializer", "handler"})
    private List<Sede> sedes;

    @OneToMany(mappedBy = "hojaVida", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore
    private List<Soporte> soportes;

    @OneToMany(mappedBy = "hojaVida", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore
    private List<CursoAsignado> cursosAsignados;
}