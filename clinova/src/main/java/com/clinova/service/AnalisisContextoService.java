package com.clinova.service;

import com.clinova.entity.AnalisisContexto;
import com.clinova.repository.AnalisisContextoRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalisisContextoService {
    private final AnalisisContextoRepository repository;

    public List<AnalisisContexto> findAll() {
        return repository.findAll();
    }

    public AnalisisContexto findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public AnalisisContexto save(AnalisisContexto entity) {
        return repository.save(entity);
    }

    public AnalisisContexto update(Long id, AnalisisContexto entity) {
        if (repository.existsById(id)) {
            entity.setId(id);
            return repository.save(entity);
        }
        return null;
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
