package com.clinova.controller;

import com.clinova.dto.HojaVidaRequestDTO;
import com.clinova.dto.HojaVidaResponseDTO;
import com.clinova.service.HojaVidaService;
import com.clinova.repository.HojaVidaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hojas-vida")
@RequiredArgsConstructor
public class HojaVidaController {

    private final HojaVidaService hojaVidaService;
    private final HojaVidaRepository hojaVidaRepository;

    @GetMapping
    public ResponseEntity<List<HojaVidaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(hojaVidaService.obtenerTodasLasHojasDeVida());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HojaVidaResponseDTO> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(hojaVidaService.obtenerHojaVidaPorId(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/cedula/{cedula}")
    public ResponseEntity<HojaVidaResponseDTO> obtenerPorCedula(@PathVariable String cedula) {
        try {
            return ResponseEntity.ok(hojaVidaService.obtenerHojaVidaPorCedula(cedula));
        } catch (Exception e) {
            // Evita el error 500 y devuelve 404 correctamente si no existe
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<HojaVidaResponseDTO> crear(@RequestBody HojaVidaRequestDTO hojaVida) {
        return ResponseEntity.ok(hojaVidaService.crearHojaVida(hojaVida));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HojaVidaResponseDTO> actualizar(@PathVariable Long id, @RequestBody HojaVidaRequestDTO detalles) {
        return ResponseEntity.ok(hojaVidaService.actualizarHojaVida(id, detalles));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!hojaVidaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        hojaVidaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HojaVidaResponseDTO> subirFoto(
            @PathVariable Long id,
            @RequestParam("foto") MultipartFile archivo) {
        try {
            return ResponseEntity.ok(hojaVidaService.subirFoto(id, archivo));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}