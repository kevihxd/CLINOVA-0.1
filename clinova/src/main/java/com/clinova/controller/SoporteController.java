package com.clinova.controller;

import com.clinova.dto.SoporteRequestDTO;
import com.clinova.dto.SoporteResponseDTO;
import com.clinova.dto.StructureResponses;
import com.clinova.service.SoporteService;
import jakarta.validation.Valid;
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

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public StructureResponses<SoporteResponseDTO> crearSoporte(
            @Valid @RequestPart("datos") SoporteRequestDTO request,
            @RequestPart("archivo") MultipartFile archivo) {
        SoporteResponseDTO response = soporteService.crearSoporte(request, archivo);
        return new StructureResponses<>("SUCCESS", "Soporte creado exitosamente", response);
    }

    @GetMapping("/hoja-vida/{hojaVidaId}")
    public StructureResponses<List<SoporteResponseDTO>> obtenerSoportesPorHojaVida(@PathVariable Long hojaVidaId) {
        List<SoporteResponseDTO> response = soporteService.obtenerSoportesPorHojaVidaId(hojaVidaId);
        return new StructureResponses<>("SUCCESS", "Soportes obtenidos exitosamente", response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public StructureResponses<Void> eliminarSoporte(@PathVariable Long id) {
        soporteService.eliminarSoporte(id);
        return new StructureResponses<>("SUCCESS", "Soporte eliminado exitosamente", null);
    }
}