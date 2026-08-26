package com.clinova.repository;

import com.clinova.entity.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    Optional<Documento> findByKawakId(Long kawakId);
    Optional<Documento> findByCodigo(String codigo);
    List<Documento> findAllByCodigo(String codigo);
    long countByCodigoStartingWith(String prefix);
    boolean existsByCodigo(String codigo);

    @Query("SELECT d.codigo FROM Documento d WHERE d.codigo LIKE CONCAT(:prefix, '%')")
    List<String> findCodigosByPrefix(@Param("prefix") String prefix);

    @Query("""
        SELECT new com.clinova.dto.DocumentoListDTO(
            d.id, d.kawakId, d.codigo, d.nombre, d.tipo, d.proceso, d.sede,
            d.estado, d.version, d.mesesRevision, d.metodoCreacion, d.normas,
            d.rutaArchivoLocal, d.ubicacion, d.ubicacionPdf, d.fechaAprobacion,
            d.fechaElaboracion, d.fechaRevision
        ) FROM Documento d
        WHERE (d.codigo IS NULL OR d.codigo NOT LIKE 'EXT-%')
          AND (d.proceso IS NULL OR d.proceso NOT LIKE '%EXTERNA Y REQUISITOS%')
          AND (d.tipo IS NULL OR d.tipo NOT LIKE '%EXTERNO%')
        ORDER BY d.kawakId DESC, d.id DESC
    """)
    List<com.clinova.dto.DocumentoListDTO> findAllLightweight();
}