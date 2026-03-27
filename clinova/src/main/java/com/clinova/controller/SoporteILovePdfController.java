package com.clinova.controller;

import com.clinova.dto.SoporteResponseDTO;
import com.clinova.dto.StructureResponses;
import com.clinova.service.SoporteILovePdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/soportes/herramientas")
@RequiredArgsConstructor
public class SoporteILovePdfController {

    private final SoporteILovePdfService soporteILovePdfService;

    @PostMapping(value = "/dividir/{hojaVidaId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public StructureResponses<List<SoporteResponseDTO>> dividirLote(
            @PathVariable Long hojaVidaId,
            @RequestParam("archivoLote") MultipartFile archivoLote) {

        List<SoporteResponseDTO> response = soporteILovePdfService.dividirYGuardarPdf(hojaVidaId, archivoLote);
        return new StructureResponses<>("SUCCESS", "Documento dividido con iLovePDF", response);
    }
}