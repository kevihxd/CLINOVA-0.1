package com.clinova.controller;

import com.clinova.dto.RechazoSoporteRequestDTO;
import com.clinova.dto.SoporteRequestDTO;
import com.clinova.dto.SoporteResponseDTO;
import com.clinova.dto.StructureResponses;
import com.clinova.service.SoporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/soportes")
@RequiredArgsConstructor
public class SoporteController {

    private final SoporteService soporteService;

    @GetMapping("/hoja-vida/{hojaVidaId}")
    public StructureResponses<List<SoporteResponseDTO>> obtenerSoportesPorHojaVida(@PathVariable Long hojaVidaId) {
        List<SoporteResponseDTO> response = soporteService.obtenerSoportesPorHojaVidaId(hojaVidaId);
        return new StructureResponses<>("SUCCESS", "Soportes obtenidos exitosamente", response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public StructureResponses<SoporteResponseDTO> crearSoporte(
            @RequestPart("datos") SoporteRequestDTO request,
            @RequestPart("archivo") MultipartFile archivo) {
        SoporteResponseDTO response = soporteService.crearSoporte(request, archivo);
        return new StructureResponses<>("SUCCESS", "Documento subido exitosamente", response);
    }

    @DeleteMapping("/{id}")
    public StructureResponses<Void> eliminarSoporte(@PathVariable Long id) {
        soporteService.eliminarSoporte(id);
        return new StructureResponses<>("SUCCESS", "Documento eliminado exitosamente", null);
    }

    @PutMapping("/{id}/tipo")
    public StructureResponses<SoporteResponseDTO> actualizarTipoDocumento(
            @PathVariable Long id,
            @RequestParam String tipoDocumento) {
        SoporteResponseDTO response = soporteService.actualizarTipoDocumento(id, tipoDocumento);
        return new StructureResponses<>("SUCCESS", "Nombre del documento actualizado", response);
    }

    @PutMapping("/{id}/rechazar")
    public StructureResponses<SoporteResponseDTO> rechazarSoporte(
            @PathVariable Long id,
            @RequestBody RechazoSoporteRequestDTO request) {
        SoporteResponseDTO response = soporteService.rechazarSoporte(id, request);
        return new StructureResponses<>("SUCCESS", "Documento rechazado y notificación enviada", response);
    }
}