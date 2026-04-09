package com.clinova.controller;

import com.clinova.dto.StructureResponses;
import com.clinova.entity.TipoDocumento;
import com.clinova.repository.TipoDocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tipos-documento")
@RequiredArgsConstructor
public class TipoDocumentoController {

    private final TipoDocumentoRepository repository;

    @GetMapping
    public ResponseEntity<StructureResponses<List<TipoDocumento>>> obtenerTodos() {
        List<TipoDocumento> lista = repository.findAll(Sort.by(Sort.Direction.ASC, "orden"));
        return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Tipos obtenidos exitosamente", lista));
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody TipoDocumento tipoDocumento) {
        try {
            TipoDocumento guardado = repository.save(tipoDocumento);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Tipo de documento creado exitosamente", guardado));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(new StructureResponses<>("ERROR", "El Prefijo ya está en uso o hay datos inválidos.", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", "Error del servidor: " + e.getMessage(), null));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @RequestBody TipoDocumento datos) {
        try {
            TipoDocumento actual = repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Tipo de documento no encontrado"));

            actual.setNombre(datos.getNombre());
            actual.setPrefijo(datos.getPrefijo());
            actual.setOrden(datos.getOrden());
            actual.setEsFormato(datos.getEsFormato());
            actual.setMarcaAgua(datos.getMarcaAgua());
            actual.setPlantilla(datos.getPlantilla());
            actual.setEsCaracterizacion(datos.getEsCaracterizacion());

            TipoDocumento actualizado = repository.save(actual);
            return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Tipo de documento actualizado exitosamente", actualizado));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(new StructureResponses<>("ERROR", "El Prefijo ya está en uso.", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new StructureResponses<>("ERROR", e.getMessage(), null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<StructureResponses<Void>> eliminar(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok(new StructureResponses<>("SUCCESS", "Tipo de documento eliminado", null));
    }
}