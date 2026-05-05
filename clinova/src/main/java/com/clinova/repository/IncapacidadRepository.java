package com.clinova.repository;

import com.clinova.entity.Incapacidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncapacidadRepository extends JpaRepository<Incapacidad, Long> {
    List<Incapacidad> findByUsuarioId(Long usuarioId);
    List<Incapacidad> findByUsuario_Persona_NumeroDocumento(String numeroDocumento);
}
