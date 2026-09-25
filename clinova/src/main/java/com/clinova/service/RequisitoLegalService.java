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
        if (repository.existsById(id)) {
            entity.setId(id);
            return repository.save(entity);
        }
        return null;
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    /**
     * Calcula el nivel de cumplimiento de la matriz.
     * Fórmula: (Cumple / Evaluables) * 100
     * Evaluables = todos los que NO son "No Aplica"
     */
    public Map<String, Object> getNivelCumplimiento() {
        long total = repository.count();
        long cumple = repository.countByCumple();
        long noCumple = repository.countByNoCumple();
        long parcial = repository.countByCumpleParcialmente();
        long evaluables = repository.countEvaluables();

        int porcentaje = evaluables > 0 ? (int) Math.round((double) cumple / evaluables * 100) : 0;

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
