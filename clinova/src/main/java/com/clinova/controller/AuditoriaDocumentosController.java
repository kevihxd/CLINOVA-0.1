package com.clinova.controller;

import com.clinova.entity.Documento;
import com.clinova.repository.DocumentoHistorialRepository;
import com.clinova.repository.DocumentoRepository;
import com.clinova.service.FileLocatorService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping({"/api/v1/documentos/auditoria-integral", "/api/v1/auditoria/documentos"})
@RequiredArgsConstructor
public class AuditoriaDocumentosController {

    private final DocumentoRepository documentoRepository;
    private final DocumentoHistorialRepository historialRepository;
    private final FileLocatorService fileLocator;
    private final ObjectMapper objectMapper;

    private final List<ExcelAuditCase> excelAuditCases = new ArrayList<>();
    private final Map<String, List<ExcelAuditCase>> excelCasesByNormKey = new HashMap<>();

    @Data
    public static class ExcelAuditCase {
        private String hoja;
        private Integer fila;
        private String nombre;
        private String codigo;
        private Long kawakId;
        private Long docId;
        private String tipoError;
        private String causa;
        private String observacion;
    }

    @Data
    @Builder
    public static class DocumentoAuditDTO {
        private Long id;
        private Long kawakId;
        private String codigo;
        private String nombre;
        private String proceso;
        private String alcance;
        private String tipo;
        private String version;
        private String estado;
        private String elabora;
        private String fechaElaboracion;

        // Archivo físico
        private boolean archivoExiste;
        private String rutaArchivoFisico;
        private Long tamanoBytes;
        private List<String> candidatosProbados;

        // Control de cambios
        private boolean tieneControlCambios;
        private String controlCambiosTexto;
        private int cantidadLogsHistorial;
        private boolean autorEsGenerico;

        // Versionamiento y Duplicidad
        private boolean esDuplicadoVigente;
        private int totalVersionesMismoCodigo;
        private List<String> versionesRegistradas;

        // Comparación con Excel
        private boolean reportadoEnExcel;
        private String hojaExcel;
        private String problemaExcelReportado;
        private String estadoResolucionExcel;
        private String observacionResolucion;
    }

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("audit/excel_audit_reference.json");
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    List<ExcelAuditCase> cases = objectMapper.readValue(is, new TypeReference<List<ExcelAuditCase>>() {});
                    excelAuditCases.addAll(cases);

                    for (ExcelAuditCase c : cases) {
                        if (c.getCodigo() != null && !c.getCodigo().isBlank()) {
                            String k = normalizeKey(c.getCodigo());
                            excelCasesByNormKey.computeIfAbsent(k, x -> new ArrayList<>()).add(c);
                        }
                        if (c.getNombre() != null && !c.getNombre().isBlank()) {
                            String k = normalizeKey(c.getNombre());
                            excelCasesByNormKey.computeIfAbsent(k, x -> new ArrayList<>()).add(c);
                        }
                    }
                    log.info("AuditoriaDocumentosController: Cargados {} casos de auditoría desde Excel reference.", excelAuditCases.size());
                }
            } else {
                log.warn("AuditoriaDocumentosController: audit/excel_audit_reference.json no encontrado en classpath.");
            }
        } catch (Exception e) {
            log.error("Error al cargar excel_audit_reference.json: {}", e.getMessage());
        }
    }

    private static String normalizeKey(String s) {
        if (s == null) return "";
        String clean = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
        return clean.replaceAll("[^a-zA-Z0-9]", "").toUpperCase().trim();
    }

    @GetMapping
    public ResponseEntity<?> ejecutarAuditoria(
            @RequestParam(value = "filtro", defaultValue = "resumen") String filtro,
            @RequestParam(value = "proceso", required = false) String filtroProceso,
            @RequestParam(value = "codigo", required = false) String filtroCodigo,
            @RequestParam(value = "id", required = false) Long filtroId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size
    ) {
        List<Documento> todos = documentoRepository.findAll();

        // 1. Agrupar por código para analizar versionamiento y unicidad
        Map<String, List<Documento>> porCodigo = new HashMap<>();
        for (Documento d : todos) {
            String cod = d.getCodigo() != null ? d.getCodigo().trim().toUpperCase() : "S_C";
            if (cod.isBlank() || cod.equals("--") || cod.equals("S/C")) {
                cod = "NAME_" + normalizeKey(d.getNombre());
            }
            porCodigo.computeIfAbsent(cod, k -> new ArrayList<>()).add(d);
        }

        // 2. Evaluar cada documento
        List<DocumentoAuditDTO> resultados = new ArrayList<>();
        int totalConArchivo = 0;
        int totalSinArchivo = 0;
        int totalConControlCambios = 0;
        int totalVigentes = 0;
        int totalHistoricos = 0;
        int totalObsoletos = 0;
        int totalDuplicadosVigentes = 0;

        Map<String, Integer> casosPorHoja = new HashMap<>();
        Map<String, Integer> casosResueltosPorHoja = new HashMap<>();

        for (Documento doc : todos) {
            String estadoNorm = doc.getEstado() != null ? doc.getEstado().trim().toUpperCase() : "NULL";
            if ("VIGENTE".equals(estadoNorm)) totalVigentes++;
            else if ("HISTORICO_VERSION".equals(estadoNorm)) totalHistoricos++;
            else if (estadoNorm.contains("OBSOLETO")) totalObsoletos++;

            // Clave del grupo
            String groupKey = doc.getCodigo() != null ? doc.getCodigo().trim().toUpperCase() : "S_C";
            if (groupKey.isBlank() || groupKey.equals("--") || groupKey.equals("S/C")) {
                groupKey = "NAME_" + normalizeKey(doc.getNombre());
            }
            List<Documento> hermanos = porCodigo.getOrDefault(groupKey, Collections.emptyList());
            long vigentesEnGrupo = hermanos.stream().filter(h -> "VIGENTE".equalsIgnoreCase(h.getEstado())).count();
            boolean esDuplicadoVigente = "VIGENTE".equalsIgnoreCase(doc.getEstado()) && vigentesEnGrupo > 1;
            if (esDuplicadoVigente) totalDuplicadosVigentes++;

            List<String> versionesHermanos = hermanos.stream()
                    .map(h -> "ID:" + h.getId() + "_v" + h.getVersion() + "(" + h.getEstado() + ")")
                    .collect(Collectors.toList());

            // Ubicación física
            List<String> candidatos = new ArrayList<>();
            if (doc.getRutaArchivoLocal() != null && !doc.getRutaArchivoLocal().isBlank()) candidatos.add(doc.getRutaArchivoLocal());
            if (doc.getUbicacionPdf() != null && !doc.getUbicacionPdf().isBlank()) candidatos.add(doc.getUbicacionPdf());
            if (doc.getUbicacion() != null && !doc.getUbicacion().isBlank()) candidatos.add(doc.getUbicacion());

            Long kId = doc.getKawakId() != null ? doc.getKawakId() : doc.getId();
            Long did = doc.getId();
            candidatos.add("files/Formatos/1/" + kId + ".pdf");
            candidatos.add("files/Formatos/1/" + did + ".pdf");
            candidatos.add("files/Documentos/1/" + kId + ".pdf");
            candidatos.add("files/Documentos/1/" + did + ".pdf");

            Path pathEncontrado = null;
            Long tamano = null;
            for (String cand : candidatos) {
                if (cand == null || cand.contains("SIN_ARCHIVO") || cand.contains(".swf")) continue;
                Path p = fileLocator.buscarArchivo(cand);
                if (p != null && Files.exists(p) && Files.isRegularFile(p)) {
                    pathEncontrado = p;
                    try { tamano = Files.size(p); } catch (Exception ignored) {}
                    break;
                }
            }

            boolean archivoExiste = (pathEncontrado != null);
            if (archivoExiste) totalConArchivo++;
            else totalSinArchivo++;

            // Control de cambios
            String cc = doc.getControlCambios() != null && !doc.getControlCambios().isBlank() 
                    ? doc.getControlCambios().trim() 
                    : (doc.getDescripcion() != null ? doc.getDescripcion().trim() : "");
            boolean tieneCC = !cc.isBlank() && cc.length() > 5;
            if (tieneCC) totalConControlCambios++;

            String elabora = doc.getElabora() != null ? doc.getElabora().trim() : "";
            boolean autorGenerico = elabora.equalsIgnoreCase("Gerente") || elabora.equalsIgnoreCase("Usuario del Sistema") || elabora.equalsIgnoreCase("admin") || elabora.isBlank();

            // Comparación con casos de Excel
            ExcelAuditCase casoExcel = null;
            if (doc.getCodigo() != null) {
                List<ExcelAuditCase> match = excelCasesByNormKey.get(normalizeKey(doc.getCodigo()));
                if (match != null && !match.isEmpty()) casoExcel = match.get(0);
            }
            if (casoExcel == null && doc.getNombre() != null) {
                List<ExcelAuditCase> match = excelCasesByNormKey.get(normalizeKey(doc.getNombre()));
                if (match != null && !match.isEmpty()) casoExcel = match.get(0);
            }

            boolean reportadoExcel = (casoExcel != null);
            String hojaExcel = null;
            String problemaExcel = null;
            String estadoResolucion = null;
            String obsResolucion = null;

            if (reportadoExcel) {
                hojaExcel = casoExcel.getHoja();
                problemaExcel = casoExcel.getTipoError();
                casosPorHoja.put(hojaExcel, casosPorHoja.getOrDefault(hojaExcel, 0) + 1);

                // Evaluar resolución según el tipo de problema reportado
                boolean resuelto = true;
                StringBuilder obs = new StringBuilder();

                // Caso 1: Oculto en Calidad ("C NO TIENE" o 'O')
                if ("27_de_Agosto".equals(hojaExcel) || (problemaExcel != null && problemaExcel.contains("NO VISIBLE"))) {
                    if (doc.getProceso() != null && !"GESTIÓN DE CALIDAD".equalsIgnoreCase(doc.getProceso())) {
                        obs.append("Proceso reasignado correctamente a: ").append(doc.getProceso()).append("; ");
                    } else if (doc.getCodigo() != null && doc.getCodigo().startsWith("PGC-")) {
                        obs.append("Proceso Calidad legítimo según código; ");
                    } else {
                        resuelto = false;
                        obs.append("Alerta: continúa en Calidad; ");
                    }
                }

                // Caso 2: Duplicados
                if (problemaExcel != null && problemaExcel.contains("DUPLICADO")) {
                    if (!esDuplicadoVigente) {
                        obs.append("Duplicidad resuelta (1 única versión VIGENTE); ");
                    } else {
                        resuelto = false;
                        obs.append("Alerta: coexisten múltiples versiones VIGENTES; ");
                    }
                }

                // Caso 3: Metadatos / Autor
                if (hojaExcel.contains("Talento") || hojaExcel.contains("Mapa")) {
                    if (!autorGenerico) {
                        obs.append("Autor sincronizado con registro real: ").append(elabora).append("; ");
                    } else {
                        resuelto = false;
                        obs.append("Autor genérico pendiente de sincronizar; ");
                    }
                }

                if (archivoExiste) {
                    obs.append("Archivo físico verificado OK.");
                } else {
                    obs.append("Archivo físico no encontrado en disco.");
                }

                estadoResolucion = resuelto ? "RESUELTO" : "PENDIENTE";
                obsResolucion = obs.toString();

                if (resuelto) {
                    casosResueltosPorHoja.put(hojaExcel, casosResueltosPorHoja.getOrDefault(hojaExcel, 0) + 1);
                }
            }

            DocumentoAuditDTO dto = DocumentoAuditDTO.builder()
                    .id(doc.getId())
                    .kawakId(doc.getKawakId())
                    .codigo(doc.getCodigo())
                    .nombre(doc.getNombre())
                    .proceso(doc.getProceso())
                    .alcance(doc.getAlcance())
                    .tipo(doc.getTipo())
                    .version(doc.getVersion())
                    .estado(doc.getEstado())
                    .elabora(doc.getElabora())
                    .fechaElaboracion(doc.getFechaElaboracion())
                    .archivoExiste(archivoExiste)
                    .rutaArchivoFisico(pathEncontrado != null ? pathEncontrado.toString() : null)
                    .tamanoBytes(tamano)
                    .candidatosProbados(candidatos)
                    .tieneControlCambios(tieneCC)
                    .controlCambiosTexto(cc.length() > 150 ? cc.substring(0, 150) + "..." : cc)
                    .cantidadLogsHistorial(0)
                    .autorEsGenerico(autorGenerico)
                    .esDuplicadoVigente(esDuplicadoVigente)
                    .totalVersionesMismoCodigo(hermanos.size())
                    .versionesRegistradas(versionesHermanos)
                    .reportadoEnExcel(reportadoExcel)
                    .hojaExcel(hojaExcel)
                    .problemaExcelReportado(problemaExcel)
                    .estadoResolucionExcel(estadoResolucion)
                    .observacionResolucion(obsResolucion)
                    .build();

            resultados.add(dto);
        }

        // 3. Filtrado de la lista
        List<DocumentoAuditDTO> filtrados = resultados.stream().filter(dto -> {
            if (filtroId != null && !filtroId.equals(dto.getId())) return false;
            if (filtroCodigo != null && !dto.getCodigo().toUpperCase().contains(filtroCodigo.toUpperCase().trim())) return false;
            if (filtroProceso != null && (dto.getProceso() == null || !dto.getProceso().toUpperCase().contains(filtroProceso.toUpperCase().trim()))) return false;

            if ("excel".equalsIgnoreCase(filtro)) return dto.isReportadoEnExcel();
            if ("sin_archivo".equalsIgnoreCase(filtro)) return !dto.isArchivoExiste();
            if ("duplicados".equalsIgnoreCase(filtro)) return dto.isEsDuplicadoVigente();
            if ("problemas".equalsIgnoreCase(filtro)) {
                return !dto.isArchivoExiste() || dto.isEsDuplicadoVigente() || "PENDIENTE".equals(dto.getEstadoResolucionExcel());
            }
            return true;
        }).collect(Collectors.toList());

        // Paginación
        int start = Math.min(page * size, filtrados.size());
        int end = Math.min(start + size, filtrados.size());
        List<DocumentoAuditDTO> pagina = filtrados.subList(start, end);

        // 4. Construir respuesta
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("filtroAplicado", filtro);

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("totalDocumentos", todos.size());
        resumen.put("totalVigentes", totalVigentes);
        resumen.put("totalHistoricosVersion", totalHistoricos);
        resumen.put("totalObsoletos", totalObsoletos);
        resumen.put("conArchivoFisico", totalConArchivo);
        resumen.put("sinArchivoFisico", totalSinArchivo);
        resumen.put("porcentajeCoberturaArchivos", todos.isEmpty() ? "0%" : String.format("%.2f%%", (totalConArchivo * 100.0) / todos.size()));
        resumen.put("conControlCambios", totalConControlCambios);
        resumen.put("duplicadosVigentesCriticos", totalDuplicadosVigentes);
        resumen.put("totalCasosExcelCargados", excelAuditCases.size());
        resumen.put("casosExcelEncontradosEnBD", casosPorHoja.values().stream().mapToInt(Integer::intValue).sum());
        resumen.put("casosExcelResueltos", casosResueltosPorHoja.values().stream().mapToInt(Integer::intValue).sum());
        response.put("resumen", resumen);

        Map<String, Object> hojasDetalle = new LinkedHashMap<>();
        for (String hoja : List.of("25_de_Agosto", "26_de_Agosto", "27_de_Agosto", "Mapa_de_procesos", "Gestion_de_Talento_Humano")) {
            Map<String, Object> h = new LinkedHashMap<>();
            int total = casosPorHoja.getOrDefault(hoja, 0);
            int resueltos = casosResueltosPorHoja.getOrDefault(hoja, 0);
            h.put("totalDetectados", total);
            h.put("resueltos", resueltos);
            h.put("porcentaje", total > 0 ? String.format("%.1f%%", (resueltos * 100.0) / total) : "N/A");
            hojasDetalle.put(hoja, h);
        }
        response.put("desglosePorHojaExcel", hojasDetalle);

        response.put("paginacion", Map.of(
                "paginaActual", page,
                "tamanoPagina", size,
                "totalElementosFiltrados", filtrados.size(),
                "totalPaginas", (int) Math.ceil((double) filtrados.size() / size)
        ));

        response.put("documentos", pagina);

        return ResponseEntity.ok(response);
    }
}
