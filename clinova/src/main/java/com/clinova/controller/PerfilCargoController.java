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
                    
                    String nombreCargo = cargo != null ? cargo.getNombre() : "Cargo";
                    String jefeNombre = (cargo != null && cargo.getReportaA() != null) ? cargo.getReportaA().getNombre() : "Gerente General";
                    
                    nuevoPerfil.setJefeInmediato(jefeNombre);
                    nuevoPerfil.setVersion("1");
                    nuevoPerfil.setFecha("Enero 15-2026");
                    nuevoPerfil.setMision("Ejercer con excelencia el rol de " + nombreCargo + " de la institución garantizando el cumplimiento de la normatividad y los estándares del SIG.");
                    
                    nuevoPerfil.setResponsabilidades("{\"GENERALES\":[\"Ejercer las funciones correspondientes al cargo de " + nombreCargo + " bajo la supervisión de " + jefeNombre + ".\",\"Cumplir con los procedimientos, instructivos y políticas institucionales aprobadas.\",\"Garantizar la confidencialidad, reserva y custodia de la información institucional.\"],\"ADMINISTRATIVAS\":[\"Participar en las reuniones de equipo y capacitaciones convocadas por la empresa.\",\"Mantener en adecuado estado de conservación los insumos, herramientas y equipos asignados.\"],\"SST & CALIDAD\":[\"Cumplir las normas del Sistema de Gestión de Seguridad y Salud en el Trabajo (Decreto 1072/2015).\",\"Participar activamente en las auditorías e iniciativas de mejora continua del SIG.\"]}");
                    nuevoPerfil.setRequisitosEducacion("{\"nivel\":\"Profesional\",\"titulo\":\"" + nombreCargo + "\",\"puntaje\":15,\"minimo\":15}");
                    nuevoPerfil.setRequisitosFormacion("{\"minimo\":6,\"items\":[{\"programa\":\"Capacitación en SIG y Calidad\",\"puntaje\":2},{\"programa\":\"Seguridad y Salud en el Trabajo (SST)\",\"puntaje\":2},{\"programa\":\"Atención al Usuario y Trato Humanizado\",\"puntaje\":2}]}");
                    nuevoPerfil.setRequisitosHabilidades("{\"minimo\":0,\"items\":[{\"habilidad\":\"Trabajo en Equipo\",\"puntaje\":5},{\"habilidad\":\"Orientación al Servicio\",\"puntaje\":5}]}");
                    nuevoPerfil.setRequisitosExperiencia("{\"cargo\":\"" + nombreCargo + "\",\"duracion\":\"Mínimo 6 meses\",\"puntaje\":15,\"minimo\":30}");
                    nuevoPerfil.setVersiones("[{\"version\":\"Versión 1\",\"fecha\":\"2026-01-15\",\"descripcion\":\"Perfil oficial\"}]");
                    
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