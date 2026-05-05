package com.clinova.controller;

import com.clinova.entity.Sede;
import com.clinova.repository.SedeRepository;
import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    public void init() {
        if (sedeRepository.count() == 0) {
            sedeRepository.save(Sede.builder().nombre("PAMI").build());
            sedeRepository.save(Sede.builder().nombre("PRINCIPAL").build());
            sedeRepository.save(Sede.builder().nombre("CAOBOS 2").build());
        }
    }

    @GetMapping
    public ResponseEntity<List<Sede>> listarSedes() {
        return ResponseEntity.ok(sedeRepository.findAll());
    }
}
