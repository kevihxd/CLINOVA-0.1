package com.clinova.controller;

import com.clinova.dto.SoporteResponseDTO;
import com.clinova.dto.StructureResponses;
import com.clinova.service.SoporteInteligenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/soportes-inteligentes")
@RequiredArgsConstructor
public class SoporteInteligenteController {

    private final SoporteInteligenteService soporteInteligenteService;

    @PostMapping(value = "/procesar-lote/{hojaVidaId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public StructureResponses<List<SoporteResponseDTO>> procesarSoporteLote(
            @PathVariable Long hojaVidaId,
            @RequestPart("archivoLote") MultipartFile archivoLote) {

        List<SoporteResponseDTO> response = soporteInteligenteService.procesarSoporteLote(hojaVidaId, archivoLote);
        return new StructureResponses<>("SUCCESS", "Archivo procesado y clasificado por IA exitosamente", response);
    }
}