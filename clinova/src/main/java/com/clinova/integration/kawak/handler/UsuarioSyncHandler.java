package com.clinova.integration.kawak.handler;

import com.clinova.entity.HojaVida;
import com.clinova.integration.kawak.dto.KawakUsuarioDTO;
import com.clinova.repository.HojaVidaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Hace upsert de HojaVida usando los campos de GET /api/v1/usuarios de Kawak:
 *   id, nombre, apellido, login, cargo, tipo_usuario, email,
 *   area_dependencia, jefe_inmediato, sedes, grupos_distribucion
 *
 * Upsert por kawakId. Si no hay kawakId, fallback por cedula/login.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UsuarioSyncHandler {

    private final HojaVidaRepository hojaVidaRepository;

    @Transactional
    public int[] sincronizar(List<KawakUsuarioDTO> dtos) {
        int creados = 0;
        int actualizados = 0;

        for (KawakUsuarioDTO dto : dtos) {
            if (dto.id() == null) continue;
            try {
                boolean esNuevo = hojaVidaRepository.findByKawakId(dto.id()).isEmpty();
                HojaVida hv = hojaVidaRepository.findByKawakId(dto.id())
                        .orElseGet(() -> hojaVidaRepository.findByCedula(dto.login()).orElse(new HojaVida()));

                hv.setKawakId(dto.id());
                hv.setNombres(strOrDefault(dto.nombre(), "Sin nombre"));
                hv.setApellidos(strOrDefault(dto.apellido(), "Sin apellido"));

                // En Kawak 'login' es la cédula o el usuario — usarlo como cedula si no existe
                if (hv.getCedula() == null || hv.getCedula().isBlank()) {
                    hv.setCedula(strOrDefault(dto.login(), String.valueOf(dto.id())));
                }

                hv.setCorreoElectronico(dto.email());
                hv.setEstado("ACTIVO");
                hv.setFechaIngreso(hv.getFechaIngreso() != null ? hv.getFechaIngreso() : LocalDate.now());
                hv.setFechaUltimaEdicion(LocalDateTime.now());
                hv.setUsuarioUltimaEdicion("KAWAK_SYNC");

                hojaVidaRepository.save(hv);

                if (esNuevo) creados++;
                else actualizados++;

            } catch (Exception e) {
                log.error("Error sincronizando usuario kawakId={}: {}", dto.id(), e.getMessage());
            }
        }

        log.info("Usuarios (HojaVida) — creados: {}, actualizados: {}", creados, actualizados);
        return new int[]{creados, actualizados};
    }

    private String strOrDefault(String val, String def) {
        return (val != null && !val.isBlank()) ? val : def;
    }
}
