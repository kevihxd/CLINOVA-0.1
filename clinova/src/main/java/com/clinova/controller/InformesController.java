package com.clinova.controller;

import com.clinova.dto.ReporteTalentoHumanoDTO;
import com.clinova.dto.ReporteVacunacionDTO;
import com.clinova.service.InformesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/informes")
@RequiredArgsConstructor
public class InformesController {

    private final InformesService informesService;

    @GetMapping("/talento-humano")
    public ResponseEntity<List<ReporteTalentoHumanoDTO>> obtenerReporteTalentoHumano() {
        return ResponseEntity.ok(informesService.generarReporteTalentoHumano());
    }

    @GetMapping("/vacunacion")
    public ResponseEntity<List<ReporteVacunacionDTO>> obtenerReporteVacunacion() {
        return ResponseEntity.ok(informesService.generarReporteVacunacion());
    }

    @GetMapping("/incapacidades")
    public ResponseEntity<List<com.clinova.dto.ReporteIncapacidadDTO>> obtenerReporteIncapacidades() {
        return ResponseEntity.ok(informesService.generarReporteIncapacidades());
    }
}