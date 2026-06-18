package com.clinova.controller;

import com.clinova.entity.SyncLog;
import com.clinova.integration.kawak.KawakSyncService;
import com.clinova.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/sync/kawak")
@RequiredArgsConstructor
public class KawakSyncController {

    private final KawakSyncService kawakSyncService;
    private final SyncLogRepository syncLogRepository;

    /**
     * POST /api/v1/sync/kawak
     * Sincroniza todos los módulos disponibles.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> sincronizarTodo() {
        log.info("Sync manual total iniciado por administrador");
        List<SyncLog> resultados = kawakSyncService.sincronizarTodo();

        int totalCreados = resultados.stream().mapToInt(s -> s.getRegistrosCreados() != null ? s.getRegistrosCreados() : 0).sum();
        int totalActualizados = resultados.stream().mapToInt(s -> s.getRegistrosActualizados() != null ? s.getRegistrosActualizados() : 0).sum();
        long fallidos = resultados.stream().filter(s -> "FALLIDO".equals(s.getEstado())).count();

        return ResponseEntity.ok(Map.of(
                "status", fallidos == 0 ? "EXITOSO" : "PARCIAL",
                "modulosSincronizados", resultados.size(),
                "totalCreados", totalCreados,
                "totalActualizados", totalActualizados,
                "modulos", resultados
        ));
    }

    /**
     * POST /api/v1/sync/kawak/{modulo}
     * Sincroniza un módulo específico: actas, documentos, empleados
     */
    @PostMapping("/{modulo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SyncLog> sincronizarModulo(@PathVariable String modulo) {
        log.info("Sync manual del módulo '{}' iniciado por administrador", modulo);
        SyncLog resultado = kawakSyncService.sincronizarModulo(modulo);
        return ResponseEntity.ok(resultado);
    }

    /**
     * GET /api/v1/sync/kawak/logs
     * Devuelve las últimas 20 ejecuciones de sincronización.
     */
    @GetMapping("/logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SyncLog>> obtenerLogs() {
        return ResponseEntity.ok(syncLogRepository.findTop20ByOrderByFechaEjecucionDesc());
    }

    /**
     * GET /api/v1/sync/kawak/logs/{modulo}
     * Devuelve el historial de sync de un módulo específico.
     */
    @GetMapping("/logs/{modulo}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SyncLog>> obtenerLogsPorModulo(@PathVariable String modulo) {
        return ResponseEntity.ok(syncLogRepository.findByModuloOrderByFechaEjecucionDesc(modulo));
    }

    /**
     * GET /api/v1/sync/kawak/modulos
     * Lista los módulos disponibles para sincronización.
     */
    @GetMapping("/modulos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> listarModulos() {
        return ResponseEntity.ok(Map.of(
                "modulosDisponibles", KawakSyncService.MODULOS_DISPONIBLES,
                "descripcion", Map.of(
                        "actas", "Actas e informes de reuniones",
                        "documentos", "Documentos del SGC (listado único)",
                        "empleados", "Hojas de vida del talento humano"
                )
        ));
    }
}
