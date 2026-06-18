package com.clinova.integration.kawak;

import com.clinova.entity.SyncLog;
import com.clinova.integration.kawak.handler.ActaSyncHandler;
import com.clinova.integration.kawak.handler.DocumentoSyncHandler;
import com.clinova.integration.kawak.handler.UsuarioSyncHandler;
import com.clinova.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Orquestador principal de sincronización con Kawak.
 *
 * Módulos disponibles según el Swagger de Kawak:
 *   - actas     → GET /api/v1/actas
 *   - documentos → GET /api/v1/documentos
 *   - usuarios   → GET /api/v1/usuarios
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KawakSyncService {

    private final KawakApiClient apiClient;
    private final ActaSyncHandler actaSyncHandler;
    private final DocumentoSyncHandler documentoSyncHandler;
    private final UsuarioSyncHandler usuarioSyncHandler;
    private final SyncLogRepository syncLogRepository;

    public static final String MODULO_ACTAS      = "actas";
    public static final String MODULO_DOCUMENTOS = "documentos";
    public static final String MODULO_USUARIOS   = "usuarios";
    public static final List<String> MODULOS_DISPONIBLES = List.of(MODULO_ACTAS, MODULO_DOCUMENTOS, MODULO_USUARIOS);

    public List<SyncLog> sincronizarTodo() {
        log.info("=== Iniciando sincronización completa con Kawak ===");
        return MODULOS_DISPONIBLES.stream().map(this::sincronizarModulo).toList();
    }

    public SyncLog sincronizarModulo(String modulo) {
        long inicio = System.currentTimeMillis();
        log.info("Sincronizando módulo Kawak: {}", modulo);

        SyncLog.SyncLogBuilder logBuilder = SyncLog.builder()
                .modulo(modulo)
                .fechaEjecucion(LocalDateTime.now());
        try {
            int[] resultado = ejecutarModulo(modulo);
            long duracion = System.currentTimeMillis() - inicio;

            SyncLog syncLog = logBuilder
                    .estado("EXITOSO")
                    .registrosCreados(resultado[0])
                    .registrosActualizados(resultado[1])
                    .registrosSincronizados(resultado[0] + resultado[1])
                    .duracionMs(duracion)
                    .build();

            syncLogRepository.save(syncLog);
            log.info("Módulo {} OK en {}ms — creados: {}, actualizados: {}", modulo, duracion, resultado[0], resultado[1]);
            return syncLog;

        } catch (Exception e) {
            long duracion = System.currentTimeMillis() - inicio;
            log.error("Error sincronizando módulo {}: {}", modulo, e.getMessage(), e);

            SyncLog syncLog = logBuilder
                    .estado("FALLIDO")
                    .registrosSincronizados(0)
                    .registrosCreados(0)
                    .registrosActualizados(0)
                    .duracionMs(duracion)
                    .errorMensaje(e.getMessage())
                    .build();

            syncLogRepository.save(syncLog);
            return syncLog;
        }
    }

    private int[] ejecutarModulo(String modulo) {
        return switch (modulo.toLowerCase()) {
            case MODULO_ACTAS      -> actaSyncHandler.sincronizar(apiClient.obtenerActas());
            case MODULO_DOCUMENTOS -> documentoSyncHandler.sincronizar(apiClient.obtenerDocumentos());
            case MODULO_USUARIOS   -> usuarioSyncHandler.sincronizar(apiClient.obtenerUsuarios());
            default -> throw new IllegalArgumentException(
                    "Módulo no soportado: " + modulo + ". Disponibles: " + MODULOS_DISPONIBLES);
        };
    }
}
