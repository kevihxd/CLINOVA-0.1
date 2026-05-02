package com.clinova.service;

import com.clinova.dto.CategoriaSoporteDTO;
import com.clinova.repository.CategoriaSoporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaSoporteService {

    private final CategoriaSoporteRepository categoriaSoporteRepository;

    @Transactional(readOnly = true)
    public List<CategoriaSoporteDTO> obtenerTodas() {
        return categoriaSoporteRepository.findAll().stream()
                .map(cat -> new CategoriaSoporteDTO(cat.getId(), cat.getNombre()))
                .collect(Collectors.toList());
    }
}