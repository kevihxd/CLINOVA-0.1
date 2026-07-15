package com.clinova.controller;

import com.clinova.entity.Sede;
import com.clinova.dto.SedeDTO;
import com.clinova.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/sedes")
@RequiredArgsConstructor
public class SedeController {

    private final SedeRepository sedeRepository;

    @GetMapping
    public ResponseEntity<List<SedeDTO>> listarSedes() {
        try {
            // Semilla inicial si la tabla está vacía
            try {
                if (sedeRepository.count() == 0) {
                    sedeRepository.save(Sede.builder().nombre("PAMI").build());
                    sedeRepository.save(Sede.builder().nombre("PRINCIPAL").build());
                    sedeRepository.save(Sede.builder().nombre("CAOBOS 2").build());
                }
            } catch (Exception seed) {
                log.warn("No se pudieron insertar sedes iniciales (puede que ya existan): {}", seed.getMessage());
            }

            List<SedeDTO> sedesDTO = sedeRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id")).stream()
                    .map(sede -> new SedeDTO(sede.getId(), sede.getNombre()))
                    .toList();
            return ResponseEntity.ok(sedesDTO);

        } catch (Exception e) {
            log.error("Error al listar sedes: {}", e.getMessage(), e);
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    @PostMapping
    public ResponseEntity<SedeDTO> crearSede(@RequestBody SedeDTO dto) {
        try {
            Sede sede = Sede.builder().nombre(dto.nombre()).build();
            Sede guardada = sedeRepository.save(sede);
            return ResponseEntity.ok(new SedeDTO(guardada.getId(), guardada.getNombre()));
        } catch (Exception e) {
            log.error("Error al crear sede: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarSede(@PathVariable Long id) {
        try {
            sedeRepository.deleteById(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error al eliminar sede {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
