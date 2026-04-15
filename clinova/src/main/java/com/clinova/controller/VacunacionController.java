package com.clinova.controller;

import com.clinova.service.VacunacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/vacunacion")
@RequiredArgsConstructor
public class VacunacionController {

    private final VacunacionService vacunacionService;

    @PostMapping("/importar")
    public ResponseEntity<String> importarVacunas(@RequestParam("archivo") MultipartFile archivo) {
        vacunacionService.procesarExcelVacunas(archivo);
        return ResponseEntity.ok("Archivo procesado y registros de vacunación actualizados exitosamente.");
    }
}