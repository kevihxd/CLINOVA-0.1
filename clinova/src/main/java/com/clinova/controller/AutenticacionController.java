package com.clinova.controller;

import com.clinova.dto.AutenticacionRequestDTO;
import com.clinova.dto.AutenticacionResponseDTO;
import com.clinova.service.AutenticacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AutenticacionController {

    private final AutenticacionService autenticacionService;

    @PostMapping("/registro")
    public ResponseEntity<AutenticacionResponseDTO> registrar(@RequestBody AutenticacionRequestDTO request) {
        log.info("Registro de usuario: {}", request.getUsername());
        return ResponseEntity.ok(autenticacionService.registro(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AutenticacionResponseDTO> autenticar(@RequestBody AutenticacionRequestDTO request) {
        log.info("Intento de login: {}", request.getUsername());
        return ResponseEntity.ok(autenticacionService.login(request));
    }
}