package com.clinova.scheduler;

import com.clinova.entity.Documento;
import com.clinova.repository.DocumentoRepository;
import com.clinova.service.DocumentoHistorialService;
import com.clinova.util.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentoStatusScheduler {

    private final DocumentoRepository repository;
    private final DocumentoHistorialService historialService;

    /**
     * Se ejecuta todos los días a la 1:00 AM para verificar vencimientos de documentos.
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void actualizarEstadosDocumentos() {
        log.info("Iniciando validación automática de estados de documentos...");
        
        List<Documento> documentos = repository.findAll();
        int vencidos = 0;
        int aVencer = 0;

            // Saltamos los que ya están retirados, obsoletos o no tienen estado
            if (doc.getEstado() == null || doc.getEstado().equalsIgnoreCase("RETIRADO") || doc.getEstado().equalsIgnoreCase("OBSOLETO")) {
                continue;
            }

            Integer dias = doc.getDiasFaltantes(); // Calculado por @PostLoad
            if (dias == null) {
                // Si no aplica vencimiento (ej. registros, folletos) y estaba marcado VENCIDO por error, restaurar a VIGENTE
                if (AppConstants.ESTADO_VENCIDO.equalsIgnoreCase(doc.getEstado()) || AppConstants.ESTADO_A_VENCER.equalsIgnoreCase(doc.getEstado())) {
                    doc.setEstado("VIGENTE");
                    repository.save(doc);
                }
                continue;
            }

            String estadoActual = doc.getEstado();
            String nuevoEstado = estadoActual;

            if (dias < 0) {
                nuevoEstado = AppConstants.ESTADO_VENCIDO;
            } else if (dias <= 30) { // Menos o igual a 30 días para vencer
                nuevoEstado = AppConstants.ESTADO_A_VENCER;
            } else if (estadoActual.equalsIgnoreCase(AppConstants.ESTADO_VENCIDO) || estadoActual.equalsIgnoreCase(AppConstants.ESTADO_A_VENCER)) {
                // Si pasaron a faltar más de 30 días
                nuevoEstado = "VIGENTE";
            }

            if (!Objects.equals(estadoActual, nuevoEstado)) {
                doc.setEstado(nuevoEstado);
                repository.save(doc);

                String msg = String.format("El estado del documento cambió automáticamente a %s (días faltantes: %d)", nuevoEstado, dias);
                historialService.registrarHistorial(doc.getId(), "CAMBIO_ESTADO", msg, "Sistema");
                
                if (AppConstants.ESTADO_VENCIDO.equals(nuevoEstado)) vencidos++;
                else if (AppConstants.ESTADO_A_VENCER.equals(nuevoEstado)) aVencer++;
            }
        }

        log.info("Validación terminada. Documentos vencidos: {}. Documentos a vencer (<=30 días): {}", vencidos, aVencer);
    }
}
