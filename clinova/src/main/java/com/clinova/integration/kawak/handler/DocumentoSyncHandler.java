package com.clinova.integration.kawak.handler;

import com.clinova.entity.Documento;
import com.clinova.integration.kawak.dto.KawakDocumentoDTO;
import com.clinova.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentoSyncHandler {

    private final DocumentoRepository documentoRepository;
    private final com.clinova.integration.kawak.KawakBackupClient backupClient;

    @Transactional
    public int[] sincronizar(List<KawakDocumentoDTO> dtos) {
        int creados = 0;
        int actualizados = 0;

        for (KawakDocumentoDTO dto : dtos) {
            if (dto.id() == null) continue;
            try {
                boolean esNuevo = documentoRepository.findByKawakId(dto.id()).isEmpty();
                Documento doc = documentoRepository.findByKawakId(dto.id()).orElse(new Documento());

                doc.setKawakId(dto.id());
                doc.setNombre(dto.nombre());
                doc.setCodigo(dto.codigo());
                doc.setTipo(dto.tipo());
                doc.setProceso(dto.proceso());
                doc.setEstado(dto.estado());
                doc.setVersion(dto.version());
                doc.setSede(dto.sede());
                doc.setAlcance(dto.alcance());
                doc.setConfidencialidad(dto.confidencialidad());
                doc.setMesesRevision(dto.mesesRevision());
                doc.setElabora(dto.elabora());
                doc.setRevisa(dto.revisa());
                doc.setAprueba(dto.aprueba());
                doc.setVisualizacion(dto.visualizacion());
                doc.setImpresion(dto.impresion());
                doc.setDescargaOriginal(dto.descargaOriginal());
                doc.setDescargaPdf(dto.descargaPdf());
                doc.setNormas(dto.normas());
                doc.setOtrosProcesos(dto.otrosProcesos());
                doc.setFechaElaboracion(dto.fechaElaboracion());
                doc.setFechaRevision(dto.fechaRevision());
                doc.setFechaAprobacion(dto.fechaAprobacion());
                doc.setUbicacion(dto.ubicacion());
                doc.setPlantilla(dto.plantilla());
                doc.setMetodoCreacion("KAWAK_SYNC");

                // --- Sincronizar archivo físico ---
                if (doc.getRutaArchivoLocal() == null) {
                    com.clinova.integration.kawak.KawakBackupClient.DownloadResult result = backupClient.descargarDocumento(dto.id());
                    if (result != null) {
                        doc.setRutaArchivoLocal(result.localPath());
                        doc.setExtensionArchivo(result.extension());
                    }
                }

                documentoRepository.save(doc);

                if (esNuevo) creados++;
                else actualizados++;

            } catch (Exception e) {
                log.error("Error al sincronizar documento kawakId={}: {}", dto.id(), e.getMessage());
            }
        }

        log.info("Documentos sincronizados — creados: {}, actualizados: {}", creados, actualizados);
        return new int[]{creados, actualizados};
    }
}
