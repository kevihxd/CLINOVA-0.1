package com.clinova.controller;

import com.clinova.dto.AutenticacionRequestDTO;
import com.clinova.dto.AutenticacionResponseDTO;
import com.clinova.service.AutenticacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AutenticacionController {

    private final AutenticacionService autenticacionService;

    @PostMapping("/registro")
    public ResponseEntity<AutenticacionResponseDTO> registrar(@RequestBody AutenticacionRequestDTO request) {
        return ResponseEntity.ok(autenticacionService.registrar(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AutenticacionResponseDTO> autenticar(@RequestBody AutenticacionRequestDTO request) {
        return ResponseEntity.ok(autenticacionService.autenticar(request));
    }
}