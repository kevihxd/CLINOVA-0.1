package com.clinova.controller;

import com.clinova.entity.Cargo;
import com.clinova.entity.PerfilCargo;
import com.clinova.repository.CargoRepository;
import com.clinova.repository.PerfilCargoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/perfiles-cargo")
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
                .orElseGet(() -> {
                    Cargo cargo = cargoRepository.findById(cargoId).orElse(null);
                    PerfilCargo nuevoPerfil = new PerfilCargo();
                    nuevoPerfil.setCargo(cargo);
                    nuevoPerfil.setEstado("ACTIVO");
                    
                    String nombreCargo = cargo != null ? cargo.getNombre() : "";
                    String jefeNombre = (cargo != null && cargo.getReportaA() != null) ? cargo.getReportaA().getNombre() : "";
                    
                    nuevoPerfil.setJefeInmediato(jefeNombre);
                    nuevoPerfil.setVersion("1");
                    nuevoPerfil.setFecha("");
                    nuevoPerfil.setMision("");
                    nuevoPerfil.setResponsabilidades("{}");
                    nuevoPerfil.setRequisitosEducacion("{}");
                    nuevoPerfil.setRequisitosFormacion("{\"minimo\":0,\"items\":[]}");
                    nuevoPerfil.setRequisitosHabilidades("{\"minimo\":0,\"items\":[]}");
                    nuevoPerfil.setRequisitosExperiencia("{}");
                    nuevoPerfil.setVersiones("[]");
                    
                    if (cargo != null) {
                        try {
                            return ResponseEntity.ok(perfilCargoRepository.save(nuevoPerfil));
                        } catch (Exception e) {
                            return ResponseEntity.ok(nuevoPerfil);
                        }
                    }
                    return ResponseEntity.ok(nuevoPerfil);
                });
    }

    @PostMapping
    public ResponseEntity<?> guardar(@RequestBody java.util.Map<String, Object> payload) {
        try {
            Long cargoId = null;
            if (payload.get("cargoId") != null) {
                cargoId = Long.valueOf(payload.get("cargoId").toString());
            } else if (payload.get("cargo") != null) {
                Object cargoObj = payload.get("cargo");
                if (cargoObj instanceof java.util.Map) {
                    java.util.Map<?, ?> cargoMap = (java.util.Map<?, ?>) cargoObj;
                    if (cargoMap.get("id") != null) {
                        cargoId = Long.valueOf(cargoMap.get("id").toString());
                    }
                } else {
                    cargoId = Long.valueOf(cargoObj.toString());
                }
            }

            if (cargoId == null) {
                return ResponseEntity.badRequest().body("El ID del cargo es obligatorio");
            }

            final Long targetCargoId = cargoId;
            Cargo cargo = cargoRepository.findById(targetCargoId).orElse(null);
            if (cargo == null) {
                return ResponseEntity.notFound().build();
            }

            PerfilCargo perfil = perfilCargoRepository.findByCargoId(cargo.getId()).orElseGet(() -> {
                PerfilCargo p = new PerfilCargo();
                p.setCargo(cargo);
                return p;
            });

            if (payload.get("jefeInmediato") != null) perfil.setJefeInmediato(payload.get("jefeInmediato").toString());
            if (payload.get("version") != null) perfil.setVersion(payload.get("version").toString());
            if (payload.get("fecha") != null) perfil.setFecha(payload.get("fecha").toString());
            if (payload.get("mision") != null) perfil.setMision(payload.get("mision").toString());
            if (payload.get("responsabilidades") != null) perfil.setResponsabilidades(payload.get("responsabilidades").toString());
            if (payload.get("requisitosEducacion") != null) perfil.setRequisitosEducacion(payload.get("requisitosEducacion").toString());
            if (payload.get("requisitosFormacion") != null) perfil.setRequisitosFormacion(payload.get("requisitosFormacion").toString());
            if (payload.get("requisitosHabilidades") != null) perfil.setRequisitosHabilidades(payload.get("requisitosHabilidades").toString());
            if (payload.get("requisitosExperiencia") != null) perfil.setRequisitosExperiencia(payload.get("requisitosExperiencia").toString());
            if (payload.get("versiones") != null) perfil.setVersiones(payload.get("versiones").toString());
            if (payload.get("estado") != null) perfil.setEstado(payload.get("estado").toString());

            if (perfil.getEstado() == null) perfil.setEstado("ACTIVO");

            PerfilCargo guardado = perfilCargoRepository.save(perfil);
            return ResponseEntity.ok(guardado);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error al guardar perfil de cargo: " + e.getMessage());
        }
    }
}