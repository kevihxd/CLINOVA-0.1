package com.clinova.repository;

import com.clinova.entity.DocumentoHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoHistorialRepository extends JpaRepository<DocumentoHistorial, Long> {

    // All logs for a document, newest first
    List<DocumentoHistorial> findByDocumentoIdOrderByFechaDesc(Long documentoId);

    // All logs for a list of document IDs, newest first
    List<DocumentoHistorial> findByDocumentoIdInOrderByFechaDesc(List<Long> documentoIds);

    // Version-specific logs, oldest first (control de cambios)
    @Query("SELECT h FROM DocumentoHistorial h WHERE h.documentoId = :docId AND h.accion = 'CREACION_VERSION' ORDER BY h.fecha ASC")
    List<DocumentoHistorial> findVersionHistoryByDocumentoId(@Param("docId") Long docId);
}
