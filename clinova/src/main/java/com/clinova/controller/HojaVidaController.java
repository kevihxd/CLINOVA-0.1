package com.clinova.controller;

import com.clinova.entity.Cargo;
import com.clinova.entity.HojaVida;
import com.clinova.entity.Sede;
import com.clinova.entity.Usuario;
import com.clinova.repository.CargoRepository;
import com.clinova.repository.HojaVidaRepository;
import com.clinova.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/hojas-vida")
@RequiredArgsConstructor
public class HojaVidaController {

    private final HojaVidaRepository hojaVidaRepository;
    private final CargoRepository cargoRepository;
    private final SedeRepository sedeRepository;

    @GetMapping
    public ResponseEntity<List<HojaVida>> listarTodas() {
        return ResponseEntity.ok(hojaVidaRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HojaVida> obtenerPorId(@PathVariable Long id) {
        return hojaVidaRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cedula/{cedula}")
    public ResponseEntity<HojaVida> obtenerPorCedula(@PathVariable String cedula) {
        return hojaVidaRepository.findByCedula(cedula)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<HojaVida> crear(@RequestBody HojaVida hojaVida) {
        if (hojaVidaRepository.existsByCedula(hojaVida.getCedula())) {
            throw new RuntimeException("Ya existe una hoja de vida con esta cédula");
        }

        if (hojaVida.getUsuarioId() != null) {
            Usuario u = new Usuario();
            u.setId(hojaVida.getUsuarioId());
            hojaVida.setUsuario(u);
        }
        hojaVida.setFechaUltimaEdicion(LocalDateTime.now());

        return ResponseEntity.ok(hojaVidaRepository.save(hojaVida));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HojaVida> actualizar(@PathVariable Long id, @RequestBody HojaVida detalles) {
        return hojaVidaRepository.findById(id)
                .map(hv -> {
                    hv.setNombres(detalles.getNombres());
                    hv.setApellidos(detalles.getApellidos());
                    hv.setFechaNacimiento(detalles.getFechaNacimiento());
                    hv.setDireccionResidencia(detalles.getDireccionResidencia());
                    hv.setTelefono(detalles.getTelefono());
                    hv.setCorreoElectronico(detalles.getCorreoElectronico());
                    hv.setContactoEmergencia(detalles.getContactoEmergencia());
                    hv.setTelefonoContactoEmergencia(detalles.getTelefonoContactoEmergencia());
                    hv.setArl(detalles.getArl());
                    hv.setEps(detalles.getEps());
                    hv.setAfp(detalles.getAfp());
                    hv.setCajaCompensacion(detalles.getCajaCompensacion());
                    hv.setSalario(detalles.getSalario());
                    hv.setSubsidioTransporte(detalles.getSubsidioTransporte());
                    hv.setFechaIngreso(detalles.getFechaIngreso());
                    hv.setEstado(detalles.getEstado());
                    hv.setTipoContrato(detalles.getTipoContrato());
                    hv.setFechaRetiro(detalles.getFechaRetiro());
                    hv.setMotivoRetiro(detalles.getMotivoRetiro());
                    hv.setPerfilVacunacion(detalles.getPerfilVacunacion());
                    hv.setPesv(detalles.getPesv());
                    hv.setResponsableEvaluacionId(detalles.getResponsableEvaluacionId());
                    hv.setUsuarioUltimaEdicion(detalles.getUsuarioUltimaEdicion());
                    hv.setFotoUrl(detalles.getFotoUrl());
                    hv.setFechaUltimaEdicion(LocalDateTime.now());

                    if (detalles.getUsuarioId() != null) {
                        Usuario u = new Usuario();
                        u.setId(detalles.getUsuarioId());
                        hv.setUsuario(u);
                    } else if (detalles.getUsuario() != null) {
                        hv.setUsuario(detalles.getUsuario());
                    }

                    return ResponseEntity.ok(hojaVidaRepository.save(hv));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!hojaVidaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        hojaVidaRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{hojaVidaId}/cargos")
    public ResponseEntity<HojaVida> asignarCargo(@PathVariable Long hojaVidaId, @RequestBody Map<String, Long> payload) {
        Long cargoId = payload.get("cargoId");
        Optional<HojaVida> hvOpt = hojaVidaRepository.findById(hojaVidaId);
        Optional<Cargo> cargoOpt = cargoRepository.findById(cargoId);

        if (hvOpt.isPresent() && cargoOpt.isPresent()) {
            HojaVida hv = hvOpt.get();
            hv.getCargos().add(cargoOpt.get());
            return ResponseEntity.ok(hojaVidaRepository.save(hv));
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/{hojaVidaId}/sedes")
    public ResponseEntity<HojaVida> asignarSede(@PathVariable Long hojaVidaId, @RequestBody Map<String, Long> payload) {
        Long sedeId = payload.get("sedeId");
        Optional<HojaVida> hvOpt = hojaVidaRepository.findById(hojaVidaId);
        Optional<Sede> sedeOpt = sedeRepository.findById(sedeId);

        if (hvOpt.isPresent() && sedeOpt.isPresent()) {
            HojaVida hv = hvOpt.get();
            hv.getSedes().add(sedeOpt.get());
            return ResponseEntity.ok(hojaVidaRepository.save(hv));
        }
        return ResponseEntity.badRequest().build();
    }
}