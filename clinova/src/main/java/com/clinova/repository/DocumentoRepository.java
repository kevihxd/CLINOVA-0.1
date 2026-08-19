package com.clinova.repository;

import com.clinova.entity.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    Optional<Documento> findByKawakId(Long kawakId);
    Optional<Documento> findByCodigo(String codigo);
    long countByCodigoStartingWith(String prefix);
    boolean existsByCodigo(String codigo);

    @org.springframework.data.jpa.repository.Query("SELECT d.codigo FROM Documento d WHERE d.codigo LIKE CONCAT(:prefix, '%')")
    List<String> findCodigosByPrefix(@org.springframework.data.repository.query.Param("prefix") String prefix);

    @org.springframework.data.jpa.repository.Query("""
        SELECT new com.clinova.dto.DocumentoListDTO(
            d.id, d.kawakId, d.codigo, d.nombre, d.tipo, d.proceso, d.sede,
            d.estado, d.version, d.mesesRevision, d.metodoCreacion, d.normas,
            d.rutaArchivoLocal, d.ubicacion, d.ubicacionPdf, d.fechaAprobacion,
            d.fechaElaboracion, d.fechaRevision
        ) FROM Documento d ORDER BY d.codigo ASC
    """)
    List<com.clinova.dto.DocumentoListDTO> findAllLightweight();
}