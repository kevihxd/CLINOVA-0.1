package com.clinova.controller;

import com.clinova.dto.SemaforizacionReporteDTO;
import com.clinova.dto.StructureResponses;
import com.clinova.service.SemaforizacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/reportes/semaforizacion")
@RequiredArgsConstructor
public class SemaforizacionController {

    private final SemaforizacionService semaforizacionService;

    @GetMapping
    public ResponseEntity<StructureResponses<List<SemaforizacionReporteDTO>>> getReporte() {
        try {
            List<SemaforizacionReporteDTO> reporte = semaforizacionService.generarReporteSemaforizacion();
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Reporte generado exitosamente", reporte));
        } catch (Exception e) {
            log.error("Error al generar reporte de semaforizacion: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

}
