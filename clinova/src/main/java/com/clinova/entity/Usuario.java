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

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private HojaVida hojaVida;

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (rol != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + rol.name()));
        }

        try {
            if (cargo != null && org.hibernate.Hibernate.isInitialized(cargo) && cargo.getPermisos() != null && org.hibernate.Hibernate.isInitialized(cargo.getPermisos())) {
                for (Permiso permiso : cargo.getPermisos()) {
                    authorities.add(new SimpleGrantedAuthority(permiso.getNombre()));
                }
            }
        } catch (Exception ignored) {}

        return authorities;
    }

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isEnabled() {
        try {
            if (hojaVida != null && org.hibernate.Hibernate.isInitialized(hojaVida)) {
                String e = hojaVida.getEstado();
                return e == null || e.equalsIgnoreCase("ACTIVO");
            }
        } catch (Exception e) {
            return true;
        }
        return true;
    }

    private boolean isHvInit() {
        try {
            return hojaVida != null && org.hibernate.Hibernate.isInitialized(hojaVida);
        } catch (Throwable e) {
            return false;
        }
    }

    @com.fasterxml.jackson.annotation.JsonProperty("arl")
    public String getArl() {
        try {
            return isHvInit() ? hojaVida.getArl() : null;
        } catch (Throwable e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonProperty("eps")
    public String getEps() {
        try {
            return isHvInit() ? hojaVida.getEps() : null;
        } catch (Throwable e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonProperty("afp")
    public String getAfp() {
        try {
            return isHvInit() ? hojaVida.getAfp() : null;
        } catch (Throwable e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonProperty("cajaCompensacion")
    public String getCajaCompensacion() {
        try {
            return isHvInit() ? hojaVida.getCajaCompensacion() : null;
        } catch (Throwable e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonProperty("fechaIngreso")
    public String getFechaIngreso() {
        try {
            return (isHvInit() && hojaVida.getFechaIngreso() != null) ? hojaVida.getFechaIngreso().toString() : null;
        } catch (Throwable e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonProperty("tipoContrato")
    public String getTipoContrato() {
        try {
            return isHvInit() ? hojaVida.getTipoContrato() : null;
        } catch (Throwable e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonProperty("salario")
    public Double getSalario() {
        try {
            return isHvInit() ? hojaVida.getSalario() : null;
        } catch (Throwable e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonProperty("subsidioTransporte")
    public String getSubsidioTransporte() {
        try {
            return isHvInit() ? hojaVida.getSubsidioTransporte() : null;
        } catch (Throwable e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonProperty("estado")
    public String getEstado() {
        try {
            return isHvInit() ? hojaVida.getEstado() : null;
        } catch (Throwable e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonProperty("fechaRetiro")
    public String getFechaRetiro() {
        try {
            return (isHvInit() && hojaVida.getFechaRetiro() != null) ? hojaVida.getFechaRetiro().toString() : null;
        } catch (Throwable e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonProperty("pesvFecha")
    public String getPesvFecha() {
        try {
            return isHvInit() ? hojaVida.getPesv() : null;
        } catch (Throwable e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonProperty("motivoRetiro")
    public String getMotivoRetiro() {
        try {
            return isHvInit() ? hojaVida.getMotivoRetiro() : null;
        } catch (Throwable e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonProperty("sede")
    public Sede getSede() {
        try {
            if (isHvInit() && hojaVida.getSedes() != null && org.hibernate.Hibernate.isInitialized(hojaVida.getSedes()) && !hojaVida.getSedes().isEmpty()) {
                return hojaVida.getSedes().get(0);
            }
        } catch (Throwable e) {}
        return null;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("sedeId")
    public Long getSedeId() {
        try {
            Sede s = getSede();
            return s != null ? s.getId() : null;
        } catch (Throwable e) { return null; }
    }

    @com.fasterxml.jackson.annotation.JsonProperty("responsableEvaluacionId")
    public Long getResponsableEvaluacionId() {
        try {
            return isHvInit() ? hojaVida.getResponsableEvaluacionId() : null;
        } catch (Throwable e) { return null; }
    }
}