package com.clinova.service;

import com.clinova.dto.CategoriaSoporteDTO;
import com.clinova.entity.CategoriaSoporte;
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
                .map(cat -> new CategoriaSoporteDTO(cat.getId(), cat.getNombre(), cat.getVisible() == null || cat.getVisible()))
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoriaSoporteDTO crear(CategoriaSoporteDTO dto) {
        if (dto == null || dto.nombre() == null || dto.nombre().trim().isEmpty()) {
            throw new RuntimeException("El nombre de la carpeta no puede estar vacío");
        }
        String nombreUpper = dto.nombre().trim().toUpperCase();
        List<CategoriaSoporte> todas = categoriaSoporteRepository.findAll();
        boolean existe = todas.stream().anyMatch(c -> c.getNombre() != null && c.getNombre().trim().toUpperCase().equals(nombreUpper));
        if (existe) {
            throw new RuntimeException("La carpeta ya existe");
        }
        CategoriaSoporte cat = CategoriaSoporte.builder()
                .nombre(nombreUpper)
                .visible(dto.visible() != null ? dto.visible() : true)
                .build();
        cat = categoriaSoporteRepository.save(cat);
        return new CategoriaSoporteDTO(cat.getId(), cat.getNombre(), cat.getVisible());
    }

    @Transactional
    public CategoriaSoporteDTO toggleVisibilidad(Long id, Boolean visible) {
        CategoriaSoporte cat = categoriaSoporteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Carpeta no encontrada"));
        cat.setVisible(visible != null ? visible : !Boolean.TRUE.equals(cat.getVisible()));
        cat = categoriaSoporteRepository.save(cat);
        return new CategoriaSoporteDTO(cat.getId(), cat.getNombre(), cat.getVisible());
    }

    @Transactional
    public void eliminarPorNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) return;
        String targetUpper = nombre.trim().toUpperCase();
        List<CategoriaSoporte> todas = categoriaSoporteRepository.findAll();
        for (CategoriaSoporte cat : todas) {
            if (cat.getNombre() != null && cat.getNombre().trim().toUpperCase().equals(targetUpper)) {
                categoriaSoporteRepository.delete(cat);
            }
        }
    }

    @Transactional
    public void eliminarPorId(Long id) {
        if (id != null && categoriaSoporteRepository.existsById(id)) {
            categoriaSoporteRepository.deleteById(id);
        }
    }
}