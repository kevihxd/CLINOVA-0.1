package com.clinova.repository;

import com.clinova.entity.DocumentoHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoHistorialRepository extends JpaRepository<DocumentoHistorial, Long> {
    List<DocumentoHistorial> findByDocumentoIdOrderByFechaDesc(Long documentoId);
}
