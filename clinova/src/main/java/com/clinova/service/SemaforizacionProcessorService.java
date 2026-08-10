package com.clinova.service;

import com.clinova.dto.CursoCsvRowDTO;
import com.clinova.dto.CsvUploadResultDTO;
import com.clinova.entity.CursoAsignado;
import com.clinova.entity.CursoMaestro;
import com.clinova.entity.HojaVida;
import com.clinova.repository.CursoAsignadoRepository;
import com.clinova.repository.CursoMaestroRepository;
import com.clinova.repository.HojaVidaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Responsabilidad: recibir DTOs parseados, aplicar la lógica de negocio
 * (tipos de fecha, cálculo de expiración) y persistir en batch.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemaforizacionProcessorService {

    private final HojaVidaRepository hojaVidaRepository;
    private final CursoMaestroRepository cursoMaestroRepository;
    private final CursoAsignadoRepository cursoAsignadoRepository;

    @Transactional
    public CsvUploadResultDTO procesarFilas(List<CursoCsvRowDTO> filas) {
        List<String> advertencias = new ArrayList<>();
        int actualizadas = 0;
        int nuevas = 0;
        int errores = 0;

        // Pre-cargar catálogos en memoria para evitar N+1 queries
        Map<String, CursoMaestro> cursosPorNombre = cursoMaestroRepository.findAll().stream()
                .collect(Collectors.toMap(
                        cm -> cm.getNombre().toUpperCase().trim(),
                        Function.identity(),
                        (a, b) -> a // Si hay duplicados, usa el primero
                ));

        List<CursoAsignado> batchToSave = new ArrayList<>();

        for (CursoCsvRowDTO fila : filas) {
            try {
                // 1. Buscar empleado por cédula
                Optional<HojaVida> hvOpt = hojaVidaRepository.findByCedula(fila.getDocumentoEmpleado());
                if (hvOpt.isEmpty()) {
                    advertencias.add("Línea " + fila.getLineaArchivo()
                            + ": Empleado con documento '" + fila.getDocumentoEmpleado() + "' no encontrado. Se omite.");
                    errores++;
                    continue;
                }
                HojaVida hojaVida = hvOpt.get();

                // 2. Buscar curso maestro por nombre
                CursoMaestro cursoMaestroMatch = cursosPorNombre.get(fila.getNombreCurso());
                if (cursoMaestroMatch == null) {
                    // Búsqueda flexible: contiene el nombre
                    cursoMaestroMatch = cursosPorNombre.values().stream()
                            .filter(cm -> cm.getNombre().toUpperCase().contains(fila.getNombreCurso())
                                    || fila.getNombreCurso().contains(cm.getNombre().toUpperCase()))
                            .findFirst().orElse(null);

                    if (cursoMaestroMatch == null) {
                        advertencias.add("Línea " + fila.getLineaArchivo()
                                + ": Curso '" + fila.getNombreCurso() + "' no existe en el catálogo. Se omite.");
                        errores++;
                        continue;
                    }
                }

                // Variable final para uso en lambda
                final CursoMaestro cursoMaestro = cursoMaestroMatch;

                // 3. Calcular fecha de expiración según tipo de curso
                LocalDate fechaExpiracion = calcularFechaExpiracion(fila, cursoMaestro);

                // 4. Calcular estado
                String estado = calcularEstado(fechaExpiracion);

                // 5. Buscar si ya existe una asignación para este empleado + curso
                Optional<CursoAsignado> existente = cursoAsignadoRepository
                        .findByHojaVidaId(hojaVida.getId()).stream()
                        .filter(ca -> ca.getCursoMaestro().getId().equals(cursoMaestro.getId()))
                        .findFirst();

                if (existente.isPresent()) {
                    // UPDATE: Actualizar fechas y estado
                    CursoAsignado ca = existente.get();
                    ca.setFechaRealizacion(fila.getFechaRealizacion());
                    ca.setFechaExpiracion(fechaExpiracion);
                    ca.setEstado("COMPLETADO");
                    if (ca.getUsuario() == null && hojaVida.getUsuario() != null) {
                        ca.setUsuario(hojaVida.getUsuario());
                    }
                    batchToSave.add(ca);
                    actualizadas++;
                } else {
                    // INSERT: Nueva asignación
                    CursoAsignado ca = CursoAsignado.builder()
                            .hojaVida(hojaVida)
                            .usuario(hojaVida.getUsuario())
                            .cursoMaestro(cursoMaestro)
                            .fechaRealizacion(fila.getFechaRealizacion())
                            .fechaExpiracion(fechaExpiracion)
                            .estado("COMPLETADO")
                            .build();
                    batchToSave.add(ca);
                    nuevas++;
                }

            } catch (Exception e) {
                errores++;
                advertencias.add("Línea " + fila.getLineaArchivo() + ": Error inesperado - " + e.getMessage());
                log.error("Error procesando línea {}: {}", fila.getLineaArchivo(), e.getMessage());
            }
        }

        // Batch save
        if (!batchToSave.isEmpty()) {
            cursoAsignadoRepository.saveAll(batchToSave);
            log.info("Batch guardado: {} registros ({} nuevos, {} actualizados)", 
                    batchToSave.size(), nuevas, actualizadas);
        }

        return CsvUploadResultDTO.builder()
                .totalFilas(filas.size())
                .procesadas(nuevas + actualizadas)
                .errores(errores)
                .actualizadas(actualizadas)
                .nuevas(nuevas)
                .advertencias(advertencias)
                .build();
    }

    /**
     * Lógica de los 2 tipos de fecha:
     * - Tipo 1 (por tiempo): fecha_realizacion + meses_vigencia del curso maestro
     * - Tipo 2 (fecha fija): el CSV trae fecha_vencimiento_fija explícita
     */
    private LocalDate calcularFechaExpiracion(CursoCsvRowDTO fila, CursoMaestro cursoMaestro) {
        // Tipo 2: Si el CSV trae fecha fija, esa tiene prioridad
        if (fila.getFechaVencimientoFija() != null) {
            return fila.getFechaVencimientoFija();
        }

        // Tipo 1: Calcular sumando meses de vigencia a la fecha de realización
        if (fila.getFechaRealizacion() != null && cursoMaestro.getMesesVigencia() != null
                && cursoMaestro.getMesesVigencia() > 0) {
            return fila.getFechaRealizacion().plusMonths(cursoMaestro.getMesesVigencia());
        }

        // Sin fecha de vencimiento calculable
        return null;
    }

    /**
     * Calcula el estado de semaforización.
     */
    private String calcularEstado(LocalDate fechaExpiracion) {
        if (fechaExpiracion == null) {
            return "VIGENTE"; // Sin vencimiento = vigente de por vida
        }

        long diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), fechaExpiracion);

        if (diasRestantes < 0) return "VENCIDO";
        if (diasRestantes <= 30) return "POR_VENCER";
        return "VIGENTE";
    }
}
