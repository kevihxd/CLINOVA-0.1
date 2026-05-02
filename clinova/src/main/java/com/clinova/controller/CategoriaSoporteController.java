package com.clinova.controller;

import com.clinova.dto.CategoriaSoporteDTO;
import com.clinova.service.CategoriaSoporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias-soportes")
@RequiredArgsConstructor
public class CategoriaSoporteController {

    private final CategoriaSoporteService categoriaSoporteService;

    @GetMapping
    public ResponseEntity<List<CategoriaSoporteDTO>> obtenerTodas() {
        return ResponseEntity.ok(categoriaSoporteService.obtenerTodas());
    }
}