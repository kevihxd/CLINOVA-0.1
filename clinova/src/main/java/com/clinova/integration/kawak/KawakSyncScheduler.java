package com.clinova.integration.kawak;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Ejecuta la sincronización con Kawak automáticamente según el cron configurado.
 * Por defecto: todos los días a las 2:00 AM.
 * Se puede desactivar poniendo kawak.sync.enabled=false en application.properties.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KawakSyncScheduler {

    private final KawakSyncService kawakSyncService;

    @Value("${kawak.sync.enabled:true}")
    private boolean syncEnabled;

    /**
     * Cron configurable desde application.properties con kawak.sync.cron
     * Default: 0 0 2 * * * (2:00 AM todos los días)
     */
    @Scheduled(cron = "${kawak.sync.cron:0 0 2 * * *}")
    public void ejecutarSyncProgramado() {
        if (!syncEnabled) {
            log.debug("Sync automático de Kawak desactivado (kawak.sync.enabled=false)");
            return;
        }
        log.info("=== Iniciando sync automático programado de Kawak ===");
        kawakSyncService.sincronizarTodo();
    }
}
