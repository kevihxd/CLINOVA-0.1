package com.clinova.integration.kawak.handler;

import com.clinova.entity.Acta;
import com.clinova.integration.kawak.dto.KawakActaDTO;
import com.clinova.repository.ActaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Hace upsert de Actas usando los campos exactos de la API de Kawak:
 *   nombre, codigo, fecha_inicio, feha_final (typo de Kawak), sede, proceso,
 *   quien_cita, elaborador, tipo, AREA, lugar, es_confidencial, estado,
 *   requiere_aprobacion, convocados_y_asistentes, contenido
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActaSyncHandler {

    private final ActaRepository actaRepository;

    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter D_FORMATTER   = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Transactional
    public int[] sincronizar(List<KawakActaDTO> dtos) {
        int creados = 0;
        int actualizados = 0;

        for (KawakActaDTO dto : dtos) {
            if (dto.id() == null) continue;
            try {
                boolean esNuevo = actaRepository.findByKawakId(dto.id()).isEmpty();
                Acta acta = actaRepository.findByKawakId(dto.id()).orElse(new Acta());

                acta.setKawakId(dto.id());
                acta.setTitulo(strOrDefault(dto.nombre(), "Sin título"));
                acta.setContenidoHtml(strOrDefault(dto.contenido(), ""));
                acta.setEstado(strOrDefault(dto.estado(), "BORRADOR"));
                acta.setTipo(strOrDefault(dto.tipo(), "GENERAL"));
                acta.setResponsable(strOrDefault(dto.elaborador(), "Sistema"));
                acta.setFecha(extraerFecha(dto.fechaInicio()));
                acta.setProceso(dto.proceso());
                acta.setSede(dto.sede());
                acta.setFechaInicio(dto.fechaInicio());
                acta.setHoraInicio(extraerHora(dto.fechaInicio()));
                acta.setFechaFin(dto.fechaFinal());        // campo "feha_final" de Kawak
                acta.setHoraFin(extraerHora(dto.fechaFinal()));
                acta.setLugar(dto.lugar());
                acta.setQuienCita(dto.quienCita());
                acta.setConfidencial("Si".equalsIgnoreCase(dto.esConfidencial()));
                acta.setElaborador(dto.elaborador());
                acta.setArea(dto.area());
                acta.setRequiereAprobacionActa(dto.requiereAprobacion());
                acta.setCompromisosAprobacion(dto.convocadosYAsistentes());

                actaRepository.save(acta);

                if (esNuevo) creados++;
                else actualizados++;

            } catch (Exception e) {
                log.error("Error sincronizando acta kawakId={}: {}", dto.id(), e.getMessage());
            }
        }

        log.info("Actas — creadas: {}, actualizadas: {}", creados, actualizados);
        return new int[]{creados, actualizados};
    }

    // "2021-05-03 14:05:03" → LocalDate 2021-05-03
    private LocalDate extraerFecha(String fechaHora) {
        if (fechaHora == null || fechaHora.isBlank()) return LocalDate.now();
        try {
            return LocalDateTime.parse(fechaHora, DT_FORMATTER).toLocalDate();
        } catch (DateTimeParseException e1) {
            try { return LocalDate.parse(fechaHora, D_FORMATTER); }
            catch (DateTimeParseException e2) { return LocalDate.now(); }
        }
    }

    // "2021-05-03 14:05:03" → "14:05"
    private String extraerHora(String fechaHora) {
        if (fechaHora == null || fechaHora.length() < 16) return null;
        try {
            LocalDateTime ldt = LocalDateTime.parse(fechaHora, DT_FORMATTER);
            return String.format("%02d:%02d", ldt.getHour(), ldt.getMinute());
        } catch (DateTimeParseException e) { return null; }
    }

    private boolean esConfidencial(String valor) {
        return "Si".equalsIgnoreCase(valor) || "Sí".equalsIgnoreCase(valor) || "true".equalsIgnoreCase(valor);
    }

    private String strOrDefault(String val, String def) {
        return (val != null && !val.isBlank()) ? val : def;
    }
}
