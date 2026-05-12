package com.clinova.controller;

import com.clinova.entity.Sede;
import com.clinova.dto.SedeDTO;
import com.clinova.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sedes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SedeController {

    private final SedeRepository sedeRepository;

    @GetMapping
    public ResponseEntity<List<SedeDTO>> listarSedes() {
        try {
            if (sedeRepository.count() == 0) {
                sedeRepository.save(Sede.builder().nombre("PAMI").build());
                sedeRepository.save(Sede.builder().nombre("PRINCIPAL").build());
                sedeRepository.save(Sede.builder().nombre("CAOBOS 2").build());
            }
        } catch (Exception e) {
            // Ignorar de forma segura si otra transacción ya las creó
        }
        List<SedeDTO> sedesDTO = sedeRepository.findAll().stream()
                .map(sede -> new SedeDTO(sede.getId(), sede.getNombre()))
                .toList();
        return ResponseEntity.ok(sedesDTO);
    }
}
