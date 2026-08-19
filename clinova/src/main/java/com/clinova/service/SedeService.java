package com.clinova.service;

import com.clinova.entity.Sede;
import com.clinova.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SedeService {

    private final SedeRepository sedeRepository;

    @Transactional(readOnly = true)
    public List<Sede> obtenerTodas() {
        return sedeRepository.findAll();
    }

    @Transactional
    public Sede crear(Sede sede) {
        if (sede.getNombre() == null || sede.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la sede es obligatorio");
        }
        String nombreUpper = sede.getNombre().trim().toUpperCase();
        sedeRepository.findByNombre(nombreUpper).ifPresent(s -> {
            throw new IllegalArgumentException("Ya existe una sede con el nombre: " + nombreUpper);
        });
        sede.setNombre(nombreUpper);
        if (sede.getActivo() == null) {
            sede.setActivo(true);
        }
        return sedeRepository.save(sede);
    }

    @Transactional
    public Sede actualizar(Long id, Sede request) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada"));
        if (request.getNombre() != null && !request.getNombre().trim().isEmpty()) {
            String nombreUpper = request.getNombre().trim().toUpperCase();
            sedeRepository.findByNombre(nombreUpper).ifPresent(s -> {
                if (!s.getId().equals(id)) {
                    throw new IllegalArgumentException("Ya existe otra sede con el nombre: " + nombreUpper);
                }
            });
            sede.setNombre(nombreUpper);
        }
        if (request.getActivo() != null) {
            sede.setActivo(request.getActivo());
        }
        return sedeRepository.save(sede);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!sedeRepository.existsById(id)) {
            throw new IllegalArgumentException("Sede no encontrada");
        }
        sedeRepository.deleteById(id);
    }
}
