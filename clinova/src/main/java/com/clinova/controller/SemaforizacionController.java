package com.clinova.controller;

import com.clinova.dto.CursoCsvRowDTO;
import com.clinova.dto.CsvUploadResultDTO;
import com.clinova.dto.SemaforizacionReporteDTO;
import com.clinova.dto.StructureResponses;
import com.clinova.service.CsvParserService;
import com.clinova.service.SemaforizacionProcessorService;
import com.clinova.service.SemaforizacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/semaforizacion")
@RequiredArgsConstructor
public class SemaforizacionController {

    private final SemaforizacionService semaforizacionService;
    private final CsvParserService csvParserService;
    private final SemaforizacionProcessorService processorService;

    @GetMapping("/reporte")
    public ResponseEntity<StructureResponses<List<SemaforizacionReporteDTO>>> getReporte(
            @RequestParam(required = false) String area) {
        try {
            List<SemaforizacionReporteDTO> reporte = semaforizacionService.obtenerReporteSemaforizacionGlobal(area);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Reporte generado exitosamente", reporte));
        } catch (Exception e) {
            log.error("Error al generar reporte de semaforizacion: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @PostMapping(value = "/upload-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StructureResponses<CsvUploadResultDTO>> uploadCsv(
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("Recibido CSV para semaforización: '{}' ({} bytes)",
                    file.getOriginalFilename(), file.getSize());

            // Paso 1: Parsear (responsabilidad del CsvParserService)
            List<CursoCsvRowDTO> filas = csvParserService.parse(file);

            // Paso 2: Procesar y persistir (responsabilidad del ProcessorService)
            CsvUploadResultDTO resultado = processorService.procesarFilas(filas);

            log.info("CSV procesado: {} filas, {} procesadas, {} errores",
                    resultado.getTotalFilas(), resultado.getProcesadas(), resultado.getErrores());

            String mensaje = String.format(
                    "CSV procesado exitosamente. %d nuevas asignaciones, %d actualizadas, %d errores.",
                    resultado.getNuevas(), resultado.getActualizadas(), resultado.getErrores());

            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", mensaje, resultado));

        } catch (IllegalArgumentException e) {
            log.warn("Error de validación en CSV: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new StructureResponses<>("ERROR", e.getMessage(), null));
        } catch (Exception e) {
            log.error("Error al procesar CSV de semaforización: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new StructureResponses<>("ERROR", "Error interno al procesar el archivo: " + e.getMessage(), null));
        }
    }
}