package com.clinova.repository;

import com.clinova.entity.Acta;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActaRepository extends JpaRepository<Acta, Long> {
    Optional<Acta> findByKawakId(Long kawakId);

    /**
     * Carga actas SIN contenidoHtml para evitar saturar memoria en listas.
     * El contenido HTML completo solo se carga al consultar por ID individual.
     */
    @Query("SELECT new com.clinova.entity.Acta(a.id, a.kawakId, a.titulo, a.fecha, a.tipo, a.estado, a.responsable, " +
           "a.proceso, a.sede, a.fechaInicio, a.horaInicio, a.fechaFin, a.horaFin, a.lugar, a.enlaceVirtual, " +
           "a.quienCita, a.confidencial, a.elaborador, a.area, a.palabrasClave, a.compromisosAprobacion, " +
           "a.convertirDocumento, a.requiereAprobacionActa, null, a.fechaCreacion) FROM Acta a ORDER BY a.id DESC")
    List<Acta> findAllResumen();
}