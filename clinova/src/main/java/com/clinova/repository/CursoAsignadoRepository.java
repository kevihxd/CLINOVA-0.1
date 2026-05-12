package com.clinova.repository;

import com.clinova.entity.CursoAsignado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CursoAsignadoRepository extends JpaRepository<CursoAsignado, Long> {
    List<CursoAsignado> findByUsuarioId(Long usuarioId);
}