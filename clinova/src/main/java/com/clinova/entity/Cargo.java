package com.clinova.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "cargos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cargo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "reporta_a_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "reportaA", "permisos"})
    private Cargo reportaA;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "cargo_permisos",
            joinColumns = @JoinColumn(name = "cargo_id"),
            inverseJoinColumns = @JoinColumn(name = "permiso_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Set<Permiso> permisos = new HashSet<>();
}