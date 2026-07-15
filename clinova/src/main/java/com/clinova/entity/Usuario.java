package com.clinova.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuarios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;

    @Column(name = "requiere_cambio_password")
    @Builder.Default
    private Boolean requiereCambioPassword = true;

    @Enumerated(EnumType.STRING)
    private Role rol;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "persona_id", referencedColumnName = "id", nullable = true)
    private Persona persona;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cargo_id", nullable = true)
    private Cargo cargo;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "usuario", "cargos", "sedes", "competencias", "soportes", "documentos", "historiales"})
    private HojaVida hojaVida;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();


        if (rol != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        }

        if (cargo != null && cargo.getPermisos() != null) {
            for (Permiso permiso : cargo.getPermisos()) {
                authorities.add(new SimpleGrantedAuthority(permiso.getNombre()));
            }
        }

        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public String getArl() {
        return hojaVida != null ? hojaVida.getArl() : null;
    }

    public String getEps() {
        return hojaVida != null ? hojaVida.getEps() : null;
    }

    public String getAfp() {
        return hojaVida != null ? hojaVida.getAfp() : null;
    }

    public String getCajaCompensacion() {
        return hojaVida != null ? hojaVida.getCajaCompensacion() : null;
    }

    public String getFechaIngreso() {
        return (hojaVida != null && hojaVida.getFechaIngreso() != null) ? hojaVida.getFechaIngreso().toString() : null;
    }

    public String getTipoContrato() {
        return hojaVida != null ? hojaVida.getTipoContrato() : null;
    }

    public Double getSalario() {
        return hojaVida != null ? hojaVida.getSalario() : null;
    }

    public String getSubsidioTransporte() {
        return hojaVida != null ? hojaVida.getSubsidioTransporte() : null;
    }

    public String getEstado() {
        return hojaVida != null ? hojaVida.getEstado() : null;
    }

    public String getFechaRetiro() {
        return (hojaVida != null && hojaVida.getFechaRetiro() != null) ? hojaVida.getFechaRetiro().toString() : null;
    }

    public String getPesvFecha() {
        return hojaVida != null ? hojaVida.getPesv() : null;
    }

    public String getMotivoRetiro() {
        return hojaVida != null ? hojaVida.getMotivoRetiro() : null;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public Sede getSede() {
        if (hojaVida != null && hojaVida.getSedes() != null && !hojaVida.getSedes().isEmpty()) {
            return hojaVida.getSedes().get(0);
        }
        return null;
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public Long getSedeId() {
        Sede s = getSede();
        return s != null ? s.getId() : null;
    }

    public Long getResponsableEvaluacionId() {
        return hojaVida != null ? hojaVida.getResponsableEvaluacionId() : null;
    }
}