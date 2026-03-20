package com.clinova.controller;

import com.clinova.dto.HojaVidaRequestDTO;
import com.clinova.dto.HojaVidaResponseDTO;
import com.clinova.dto.StructureResponses;
import com.clinova.service.HojaVidaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hojas-vida")
@RequiredArgsConstructor
public class HojaVidaController {

    private final HojaVidaService hojaVidaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StructureResponses<HojaVidaResponseDTO> crearHojaVida(@Valid @RequestBody HojaVidaRequestDTO request) {
        HojaVidaResponseDTO response = hojaVidaService.crearHojaVida(request);
        return new StructureResponses<>("SUCCESS", "Hoja de vida creada exitosamente", response);
    }

    @PutMapping("/{id}")
    public StructureResponses<HojaVidaResponseDTO> actualizarHojaVida(
            @PathVariable Long id,
            @Valid @RequestBody HojaVidaRequestDTO request) {
        HojaVidaResponseDTO response = hojaVidaService.actualizarHojaVida(id, request);
        return new StructureResponses<>("SUCCESS", "Hoja de vida actualizada exitosamente", response);
    }

    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StructureResponses<HojaVidaResponseDTO> subirFoto(
            @PathVariable Long id,
            @RequestParam("foto") MultipartFile foto) {
        HojaVidaResponseDTO response = hojaVidaService.subirFoto(id, foto);
        return new StructureResponses<>("SUCCESS", "Foto subida y vinculada exitosamente", response);
    }

    @GetMapping("/{id}")
    public StructureResponses<HojaVidaResponseDTO> obtenerHojaVida(@PathVariable Long id) {
        HojaVidaResponseDTO response = hojaVidaService.obtenerHojaVidaPorId(id);
        return new StructureResponses<>("SUCCESS", "Hoja de vida obtenida exitosamente", response);
    }

    @GetMapping
    public StructureResponses<List<HojaVidaResponseDTO>> obtenerTodasLasHojasDeVida() {
        List<HojaVidaResponseDTO> response = hojaVidaService.obtenerTodasLasHojasDeVida();
        return new StructureResponses<>("SUCCESS", "Hojas de vida obtenidas exitosamente", response);
    }

    @GetMapping("/cedula/{cedula}")
    public StructureResponses<HojaVidaResponseDTO> obtenerHojaVidaPorCedula(@PathVariable String cedula) {
        HojaVidaResponseDTO response = hojaVidaService.obtenerHojaVidaPorCedula(cedula);
        return new StructureResponses<>("SUCCESS", "Hoja de vida encontrada", response);
    }
}