package com.clinova.service;

import com.clinova.entity.RequisitoLegal;
import com.clinova.repository.RequisitoLegalRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RequisitoLegalService {
    private final RequisitoLegalRepository repository;

    public List<RequisitoLegal> findAll() {
        return repository.findAll();
    }

    public RequisitoLegal findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public RequisitoLegal save(RequisitoLegal entity) {
        return repository.save(entity);
    }

    public RequisitoLegal update(Long id, RequisitoLegal entity) {
        return repository.findById(id).map(existing -> {
            if (entity.getTipo() != null) existing.setTipo(entity.getTipo());
            if (entity.getNombre() != null) existing.setNombre(entity.getNombre());
            if (entity.getAnioPublicacion() != null) existing.setAnioPublicacion(entity.getAnioPublicacion());
            if (entity.getEmisor() != null) existing.setEmisor(entity.getEmisor());
            if (entity.getArticulos() != null) existing.setArticulos(entity.getArticulos());
            if (entity.getDescripcion() != null) existing.setDescripcion(entity.getDescripcion());
            if (entity.getEvidenciaAplicacion() != null) existing.setEvidenciaAplicacion(entity.getEvidenciaAplicacion());
            if (entity.getTema() != null) existing.setTema(entity.getTema());
            if (entity.getResponsable() != null) existing.setResponsable(entity.getResponsable());
            if (entity.getProcesoResponsables() != null) existing.setProcesoResponsables(entity.getProcesoResponsables());
            if (entity.getFrecuenciaRevision() != null) existing.setFrecuenciaRevision(entity.getFrecuenciaRevision());
            if (entity.getEstado() != null) existing.setEstado(entity.getEstado());
            if (entity.getCalificacion() != null) existing.setCalificacion(entity.getCalificacion());
            if (entity.getVencimiento() != null) existing.setVencimiento(entity.getVencimiento());
            if (entity.getUrlArchivo() != null) existing.setUrlArchivo(entity.getUrlArchivo());
            return repository.save(existing);
        }).orElse(null);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * Calcula el nivel de cumplimiento de la matriz.
     * Fórmula oficial Kawak:
     * - Cumple: 100% (peso 1.0)
     * - Cumple Parcialmente: 50% (peso 0.5)
     * - No Cumple: 0% (peso 0.0)
     * - No Aplica: no evaluable (excluido)
     * Porcentaje = ((Cumple + 0.5 * CumpleParcial) / Evaluables) * 100
     */
    public Map<String, Object> getNivelCumplimiento() {
        long total = repository.count();
        long cumple = repository.countByCumple();
        long noCumple = repository.countByNoCumple();
        long parcial = repository.countByCumpleParcialmente();
        long evaluables = repository.countEvaluables();

        double puntos = cumple + (parcial * 0.5);
        int porcentaje = evaluables > 0 ? (int) Math.round((puntos / evaluables) * 100) : 0;

        return Map.of(
            "total", total,
            "cumple", cumple,
            "noCumple", noCumple,
            "cumpleParcialmente", parcial,
            "evaluables", evaluables,
            "porcentaje", porcentaje
        );
    }
}
