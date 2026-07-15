package com.clinova.service;

import com.clinova.entity.ParteInteresada;
import com.clinova.repository.ParteInteresadaRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParteInteresadaService {
    private final ParteInteresadaRepository repository;

    public List<ParteInteresada> findAll() {
        return repository.findAll();
    }

    public ParteInteresada findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public ParteInteresada save(ParteInteresada entity) {
        return repository.save(entity);
    }

    public ParteInteresada update(Long id, ParteInteresada entity) {
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
