package com.clinova.service;

import com.clinova.dto.PlantillaDTO;
import com.clinova.entity.Plantilla;
import com.clinova.repository.PlantillaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlantillaService {

    private final PlantillaRepository plantillaRepository;

    @Transactional
    public PlantillaDTO crearPlantilla(PlantillaDTO request) {
        Plantilla plantilla = Plantilla.builder()
                .titulo(request.titulo())
                .descripcion(request.descripcion())
                .contenidoHtml(request.contenidoHtml())
                .build();

        Plantilla guardada = plantillaRepository.save(plantilla);
        return mapearADTO(guardada);
    }

    @Transactional
    public PlantillaDTO actualizarPlantilla(Long id, PlantillaDTO request) {
        Plantilla plantilla = plantillaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));

        plantilla.setTitulo(request.titulo());
        plantilla.setDescripcion(request.descripcion());
        plantilla.setContenidoHtml(request.contenidoHtml());

        Plantilla actualizada = plantillaRepository.save(plantilla);
        return mapearADTO(actualizada);
    }

    @Transactional
    public void eliminarPlantilla(Long id) {
        if (!plantillaRepository.existsById(id)) {
            throw new RuntimeException("Plantilla no encontrada");
        }
        plantillaRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<PlantillaDTO> obtenerTodas() {
        return plantillaRepository.findAll().stream()
                .map(this::mapearADTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PlantillaDTO obtenerPorId(Long id) {
        Plantilla plantilla = plantillaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));
        return mapearADTO(plantilla);
    }

    private PlantillaDTO mapearADTO(Plantilla plantilla) {
        return new PlantillaDTO(
                plantilla.getId(),
                plantilla.getTitulo(),
                plantilla.getDescripcion(),
                plantilla.getContenidoHtml(),
                plantilla.getFechaCreacion()
        );
    }
}