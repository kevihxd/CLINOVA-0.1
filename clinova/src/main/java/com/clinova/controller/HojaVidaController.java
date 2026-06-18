package com.clinova.controller;

import com.clinova.dto.HojaVidaHistorialDTO;
import com.clinova.dto.HojaVidaRequestDTO;
import com.clinova.dto.HojaVidaResponseDTO;
import com.clinova.service.HojaVidaService;
import com.clinova.service.HojaVidaHistorialService;
import com.clinova.repository.HojaVidaRepository;
import com.clinova.entity.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/hojas-vida")
@RequiredArgsConstructor
public class HojaVidaController {

    private final HojaVidaService hojaVidaService;
    private final HojaVidaRepository hojaVidaRepository;
    private final HojaVidaHistorialService historialService;

    @GetMapping
    public ResponseEntity<List<HojaVidaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(hojaVidaService.obtenerTodasLasHojasDeVida());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HojaVidaResponseDTO> obtenerPorId(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(hojaVidaService.obtenerHojaVidaPorId(id));
        } catch (Exception e) {
            log.error("Error en obtenerPorId [id={}]: {}", id, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/cedula/{cedula}")
    public ResponseEntity<HojaVidaResponseDTO> obtenerPorCedula(@PathVariable String cedula) {
        try {
            return ResponseEntity.ok(hojaVidaService.obtenerHojaVidaPorCedula(cedula));
        } catch (Exception e) {
            log.error("Error en obtenerPorCedula [cedula={}]: {}", cedula, e.getMessage(), e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HojaVidaHistorialDTO>> obtenerHistorial(@PathVariable Long id) {
        return ResponseEntity.ok(historialService.obtenerHistorialPorHojaVida(id));
    }

    @PostMapping
    public ResponseEntity<HojaVidaResponseDTO> crear(
            @RequestBody HojaVidaRequestDTO hojaVida,
            @AuthenticationPrincipal Usuario usuario) {
        HojaVidaResponseDTO resultado = hojaVidaService.crearHojaVida(hojaVida);
        historialService.registrarHistorial(
                resultado.id(), "CREACION",
                "Hoja de vida creada para " + resultado.nombres() + " " + resultado.apellidos(),
                usuario);
        return ResponseEntity.ok(resultado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HojaVidaResponseDTO> actualizar(
            @PathVariable Long id,
            @RequestBody HojaVidaRequestDTO detalles,
            @AuthenticationPrincipal Usuario usuario) {
        HojaVidaResponseDTO resultado = hojaVidaService.actualizarHojaVida(id, detalles);
        historialService.registrarHistorial(
                id, "MODIFICACION",
                "Datos de la hoja de vida actualizados",
                usuario);
        return ResponseEntity.ok(resultado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        if (!hojaVidaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        historialService.registrarHistorial(id, "ELIMINACION", "Hoja de vida eliminada", usuario);
        hojaVidaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<HojaVidaResponseDTO> subirFoto(
            @PathVariable Long id,
            @RequestParam("foto") MultipartFile archivo,
            @AuthenticationPrincipal Usuario usuario) {
        try {
            HojaVidaResponseDTO resultado = hojaVidaService.subirFoto(id, archivo);
            historialService.registrarHistorial(id, "MODIFICACION", "Foto de perfil actualizada", usuario);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            log.error("Error en subirFoto [id={}]: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}