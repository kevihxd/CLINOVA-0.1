package com.clinova.controller;

import com.clinova.entity.PerfilCargo;
import com.clinova.repository.CargoRepository;
import com.clinova.repository.PerfilCargoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/perfiles-cargo")
@CrossOrigin(origins = "*")
public class PerfilCargoController {

    @Autowired
    private PerfilCargoRepository perfilCargoRepository;

    @Autowired
    private CargoRepository cargoRepository;

    @GetMapping
    public List<PerfilCargo> listar() {
        return perfilCargoRepository.findAll();
    }

    @GetMapping("/cargo/{cargoId}")
    public ResponseEntity<PerfilCargo> obtenerPorCargo(@PathVariable Long cargoId) {
        return perfilCargoRepository.findByCargoId(cargoId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PerfilCargo> guardar(@RequestBody PerfilCargo perfil) {
        if (perfil.getCargo() == null || perfil.getCargo().getId() == null) {
            return ResponseEntity.badRequest().build();
        }

        return cargoRepository.findById(perfil.getCargo().getId()).map(cargo -> {
            perfil.setCargo(cargo);
            PerfilCargo existente = perfilCargoRepository.findByCargoId(cargo.getId()).orElse(null);

            if (existente != null) {
                existente.setMision(perfil.getMision());
                existente.setResponsabilidades(perfil.getResponsabilidades());
                existente.setRequisitosEducacion(perfil.getRequisitosEducacion());
                existente.setRequisitosExperiencia(perfil.getRequisitosExperiencia());
                existente.setEstado(perfil.getEstado() != null ? perfil.getEstado() : existente.getEstado());
                return ResponseEntity.ok(perfilCargoRepository.save(existente));
            }

            return ResponseEntity.ok(perfilCargoRepository.save(perfil));
        }).orElse(ResponseEntity.notFound().build());
    }
}