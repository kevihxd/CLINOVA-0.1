package com.clinova.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "personas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipoDocumento;
    private String numeroDocumento;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    private String fechaNacimiento;
    private String direccionResidencia;
    private String numeroTelefono;
    private String lugarNacimiento;

    @Column(name = "correo_electronico")
    private String correoElectronico;

    @Column(name = "perfil_vacunacion", length = 50)
    private String perfilVacunacion;

    @JsonIgnore
    @OneToOne(mappedBy = "persona", fetch = FetchType.LAZY)
    private Usuario usuario;
}