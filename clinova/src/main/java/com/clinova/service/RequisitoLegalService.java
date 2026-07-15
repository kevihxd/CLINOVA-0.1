package com.clinova.service;

import com.clinova.entity.RequisitoLegal;
import com.clinova.repository.RequisitoLegalRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;

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
}
