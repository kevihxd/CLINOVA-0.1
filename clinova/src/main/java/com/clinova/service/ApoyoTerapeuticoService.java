package com.clinova.service;

import com.clinova.entity.ApoyoTerapeuticoAdjunto;
import com.clinova.entity.ApoyoTerapeuticoIngreso;
import com.clinova.entity.Usuario;
import com.clinova.repository.ApoyoTerapeuticoAdjuntoRepository;
import com.clinova.repository.ApoyoTerapeuticoIngresoRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApoyoTerapeuticoService {

    public static final String TIPO_CONSENTIMIENTO = "CONSENTIMIENTO_INFORMADO";
    public static final String TIPO_PLANILLA = "PLANILLA_FIRMAS";
    public static final String TIPO_PLAN_CASERO = "PLAN_CASERO";

    private static final Set<String> TIPOS_VALIDOS = Set.of(TIPO_CONSENTIMIENTO, TIPO_PLANILLA, TIPO_PLAN_CASERO);
    private static final DateTimeFormatter FILE_TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ApoyoTerapeuticoIngresoRepository ingresoRepository;
    private final ApoyoTerapeuticoAdjuntoRepository adjuntoRepository;

    @Value("${uploads.root-path:uploads}")
    private String uploadsRootPath;

    private Path baseDir;

    @PostConstruct
    public void init() {
        this.baseDir = Paths.get(uploadsRootPath).toAbsolutePath().normalize().resolve("apoyo_terapeutico").normalize();
        try {
            Files.createDirectories(this.baseDir);
        } catch (Exception e) {
            log.warn("No se pudo crear carpeta base de Apoyo Terapéutico en {}: {}", this.baseDir, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarIngresos(String q, String estado, String filtroAlerta) {
        String queryClean = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
        String estadoClean = (estado != null && !estado.trim().isEmpty() && !"TODOS".equalsIgnoreCase(estado.trim()))
                ? estado.trim() : null;

        List<ApoyoTerapeuticoIngreso> ingresos = ingresoRepository.buscar(queryClean, estadoClean);
        if (ingresos.isEmpty()) return Collections.emptyList();

        List<Long> ids = ingresos.stream().map(ApoyoTerapeuticoIngreso::getId).collect(Collectors.toList());
        List<ApoyoTerapeuticoAdjunto> todosAdjuntos = adjuntoRepository.findByIngresoIdInOrderByFechaSubidaDescIdDesc(ids);

        Map<Long, List<ApoyoTerapeuticoAdjunto>> porIngreso = todosAdjuntos.stream()
                .collect(Collectors.groupingBy(a -> a.getIngreso().getId()));

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (ApoyoTerapeuticoIngreso ing : ingresos) {
            List<ApoyoTerapeuticoAdjunto> adjs = porIngreso.getOrDefault(ing.getId(), Collections.emptyList());
            Map<String, Object> dto = mapIngresoResumen(ing, adjs);

            if (filtroAlerta != null && !filtroAlerta.isBlank() && !"TODOS".equalsIgnoreCase(filtroAlerta)) {
                int totalCargados = (int) dto.get("totalCargados");
                String estPlan = String.valueOf(dto.get("estadoPlanCasero"));
                String fa = filtroAlerta.trim().toUpperCase();
                if ("COMPLETOS".equals(fa) && totalCargados < 3) continue;
                if ("PENDIENTES".equals(fa) && totalCargados >= 3) continue;
                if ("PLAN_CASERO_ALERTA".equals(fa) && !("VENCIDO".equals(estPlan) || "POR_VENCER".equals(estPlan) || "SIN_CARGAR".equals(estPlan))) continue;
                if ("PLAN_CASERO_VENCIDO".equals(fa) && !"VENCIDO".equals(estPlan)) continue;
            }
            resultado.add(dto);
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerDetalle(Long id) {
        ApoyoTerapeuticoIngreso ing = ingresoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ingreso no encontrado con ID: " + id));
        List<ApoyoTerapeuticoAdjunto> adjuntos = adjuntoRepository.findByIngresoIdOrderByFechaSubidaDescIdDesc(id);

        Map<String, Object> dto = mapIngresoResumen(ing, adjuntos);

        Map<String, List<Map<String, Object>>> historialPorTipo = new LinkedHashMap<>();
        historialPorTipo.put(TIPO_CONSENTIMIENTO, new ArrayList<>());
        historialPorTipo.put(TIPO_PLANILLA, new ArrayList<>());
        historialPorTipo.put(TIPO_PLAN_CASERO, new ArrayList<>());

        for (ApoyoTerapeuticoAdjunto a : adjuntos) {
            historialPorTipo.computeIfAbsent(a.getTipoAdjunto(), k -> new ArrayList<>()).add(mapAdjunto(a));
        }
        dto.put("historialPorTipo", historialPorTipo);
        dto.put("carpetaAdjuntos", "apoyo_terapeutico/ingresos/" + sanitizeSegment(ing.getNumeroIngreso()) + "/adjuntos");
        return dto;
    }

    @Transactional
    public Map<String, Object> crearIngreso(Map<String, Object> payload, Usuario usuario) {
        String numeroIngreso = strVal(payload.get("numeroIngreso"));
        String nombrePaciente = strVal(payload.get("nombrePaciente"));

        if (numeroIngreso == null || numeroIngreso.isBlank()) {
            throw new IllegalArgumentException("El número de ingreso es obligatorio.");
        }
        if (nombrePaciente == null || nombrePaciente.isBlank()) {
            throw new IllegalArgumentException("El nombre del paciente es obligatorio.");
        }

        if (ingresoRepository.findByNumeroIngresoIgnoreCase(numeroIngreso.trim()).isPresent()) {
            throw new IllegalArgumentException("Ya existe un registro para el número de ingreso: " + numeroIngreso.trim());
        }

        ApoyoTerapeuticoIngreso ingreso = ApoyoTerapeuticoIngreso.builder()
                .numeroIngreso(numeroIngreso.trim().toUpperCase())
                .tipoDocumentoPaciente(valOrDefault(strVal(payload.get("tipoDocumentoPaciente")), "CC"))
                .documentoPaciente(strVal(payload.get("documentoPaciente")))
                .nombrePaciente(nombrePaciente.trim().toUpperCase())
                .especialidad(strVal(payload.get("especialidad")))
                .sede(strVal(payload.get("sede")))
                .fechaIngreso(parseLocalDate(strVal(payload.get("fechaIngreso"))))
                .estado(valOrDefault(strVal(payload.get("estado")), "ACTIVO").toUpperCase())
                .origenDato(valOrDefault(strVal(payload.get("origenDato")), "MANUAL").toUpperCase())
                .kubappRefId(strVal(payload.get("kubappRefId")))
                .observaciones(strVal(payload.get("observaciones")))
                .creadoPor(usuario != null ? usuario.getNombreCompleto() : "Sistema")
                .build();

        ApoyoTerapeuticoIngreso guardado = ingresoRepository.save(ingreso);
        return obtenerDetalle(guardado.getId());
    }

    @Transactional
    public Map<String, Object> actualizarIngreso(Long id, Map<String, Object> payload, Usuario usuario) {
        ApoyoTerapeuticoIngreso ing = ingresoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ingreso no encontrado con ID: " + id));

        String nuevoNum = strVal(payload.get("numeroIngreso"));
        if (nuevoNum != null && !nuevoNum.isBlank() && !nuevoNum.trim().equalsIgnoreCase(ing.getNumeroIngreso())) {
            if (ingresoRepository.findByNumeroIngresoIgnoreCase(nuevoNum.trim()).isPresent()) {
                throw new IllegalArgumentException("Ya existe otro registro con el número de ingreso: " + nuevoNum.trim());
            }
            ing.setNumeroIngreso(nuevoNum.trim().toUpperCase());
        }
        if (payload.containsKey("tipoDocumentoPaciente")) ing.setTipoDocumentoPaciente(strVal(payload.get("tipoDocumentoPaciente")));
        if (payload.containsKey("documentoPaciente")) ing.setDocumentoPaciente(strVal(payload.get("documentoPaciente")));
        String nom = strVal(payload.get("nombrePaciente"));
        if (nom != null && !nom.isBlank()) ing.setNombrePaciente(nom.trim().toUpperCase());
        if (payload.containsKey("especialidad")) ing.setEspecialidad(strVal(payload.get("especialidad")));
        if (payload.containsKey("sede")) ing.setSede(strVal(payload.get("sede")));
        if (payload.containsKey("fechaIngreso")) ing.setFechaIngreso(parseLocalDate(strVal(payload.get("fechaIngreso"))));
        String est = strVal(payload.get("estado"));
        if (est != null && !est.isBlank()) ing.setEstado(est.trim().toUpperCase());
        if (payload.containsKey("observaciones")) ing.setObservaciones(strVal(payload.get("observaciones")));

        ingresoRepository.save(ing);
        return obtenerDetalle(ing.getId());
    }

    @Transactional
    public void eliminarIngreso(Long id) {
        ApoyoTerapeuticoIngreso ing = ingresoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ingreso no encontrado con ID: " + id));
        List<ApoyoTerapeuticoAdjunto> adjs = adjuntoRepository.findByIngresoIdOrderByFechaSubidaDescIdDesc(id);
        for (ApoyoTerapeuticoAdjunto a : adjs) {
            deleteFileQuietly(a.getRutaAlmacenamiento());
        }
        ingresoRepository.delete(ing);
    }

    @Transactional
    public Map<String, Object> importarIngresos(List<Map<String, Object>> filas, Usuario usuario) {
        int creados = 0;
        int actualizados = 0;
        int omitidos = 0;
        List<String> errores = new ArrayList<>();

        String actor = usuario != null ? usuario.getNombreCompleto() : "Sistema";

        if (filas != null) {
            for (int i = 0; i < filas.size(); i++) {
                Map<String, Object> fila = filas.get(i);
                try {
                    String numIng = strVal(fila.get("numeroIngreso"));
                    String nomPac = strVal(fila.get("nombrePaciente"));
                    if (numIng == null || numIng.isBlank()) {
                        omitidos++;
                        continue;
                    }
                    String numClean = numIng.trim().toUpperCase();
                    Optional<ApoyoTerapeuticoIngreso> existingOpt = ingresoRepository.findByNumeroIngresoIgnoreCase(numClean);

                    if (existingOpt.isPresent()) {
                        ApoyoTerapeuticoIngreso ing = existingOpt.get();
                        if (nomPac != null && !nomPac.isBlank()) ing.setNombrePaciente(nomPac.trim().toUpperCase());
                        String docPac = strVal(fila.get("documentoPaciente"));
                        if (docPac != null && !docPac.isBlank()) ing.setDocumentoPaciente(docPac.trim());
                        String tipoDoc = strVal(fila.get("tipoDocumentoPaciente"));
                        if (tipoDoc != null && !tipoDoc.isBlank()) ing.setTipoDocumentoPaciente(tipoDoc.trim().toUpperCase());
                        String esp = strVal(fila.get("especialidad"));
                        if (esp != null && !esp.isBlank()) ing.setEspecialidad(esp.trim());
                        String sede = strVal(fila.get("sede"));
                        if (sede != null && !sede.isBlank()) ing.setSede(sede.trim());
                        LocalDate fIng = parseLocalDate(strVal(fila.get("fechaIngreso")));
                        if (fIng != null) ing.setFechaIngreso(fIng);
                        ingresoRepository.save(ing);
                        actualizados++;
                    } else {
                        if (nomPac == null || nomPac.isBlank()) {
                            errores.add("Fila " + (i + 1) + " (Ingreso " + numClean + "): falta el nombre del paciente.");
                            omitidos++;
                            continue;
                        }
                        ApoyoTerapeuticoIngreso nuevo = ApoyoTerapeuticoIngreso.builder()
                                .numeroIngreso(numClean)
                                .tipoDocumentoPaciente(valOrDefault(strVal(fila.get("tipoDocumentoPaciente")), "CC"))
                                .documentoPaciente(strVal(fila.get("documentoPaciente")))
                                .nombrePaciente(nomPac.trim().toUpperCase())
                                .especialidad(strVal(fila.get("especialidad")))
                                .sede(strVal(fila.get("sede")))
                                .fechaIngreso(parseLocalDate(strVal(fila.get("fechaIngreso"))))
                                .estado(valOrDefault(strVal(fila.get("estado")), "ACTIVO").toUpperCase())
                                .origenDato("IMPORT_EXCEL")
                                .kubappRefId(strVal(fila.get("kubappRefId")))
                                .observaciones(strVal(fila.get("observaciones")))
                                .creadoPor(actor)
                                .build();
                        ingresoRepository.save(nuevo);
                        creados++;
                    }
                } catch (Exception e) {
                    errores.add("Fila " + (i + 1) + ": " + e.getMessage());
                    omitidos++;
                }
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("creados", creados);
        res.put("actualizados", actualizados);
        res.put("omitidos", omitidos);
        res.put("errores", errores);
        return res;
    }

    @Transactional
    public Map<String, Object> subirAdjunto(
            Long ingresoId,
            String tipoAdjuntoRaw,
            String fechaDocumentoStr,
            String observacion,
            MultipartFile archivo,
            Usuario usuario) throws IOException {

        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar un archivo para cargar.");
        }
        String tipoAdjunto = tipoAdjuntoRaw != null ? tipoAdjuntoRaw.trim().toUpperCase() : "";
        if (!TIPOS_VALIDOS.contains(tipoAdjunto)) {
            throw new IllegalArgumentException("Tipo de adjunto inválido. Permitidos: " + TIPOS_VALIDOS);
        }

        ApoyoTerapeuticoIngreso ingreso = ingresoRepository.findById(ingresoId)
                .orElseThrow(() -> new IllegalArgumentException("Ingreso no encontrado con ID: " + ingresoId));

        LocalDate fechaDoc = parseLocalDate(fechaDocumentoStr);
        if (fechaDoc == null) fechaDoc = LocalDate.now();
        LocalDate fechaVenc = TIPO_PLAN_CASERO.equals(tipoAdjunto) ? fechaDoc.plusMonths(2) : null;

        // Archivar versión anterior vigente de la misma caja (se conserva trazabilidad histórica)
        List<ApoyoTerapeuticoAdjunto> previosVigentes = adjuntoRepository
                .findByIngresoIdAndTipoAdjuntoAndVigenteTrue(ingresoId, tipoAdjunto);
        for (ApoyoTerapeuticoAdjunto prev : previosVigentes) {
            prev.setVigente(false);
            adjuntoRepository.save(prev);
        }

        // Guardar archivo físico en carpeta dedicada: apoyo_terapeutico/ingresos/{num}/adjuntos/
        String safeIngresoDir = sanitizeSegment(ingreso.getNumeroIngreso());
        Path targetDir = this.baseDir.resolve("ingresos").resolve(safeIngresoDir).resolve("adjuntos").normalize();
        if (!targetDir.startsWith(this.baseDir)) {
            throw new SecurityException("Ruta de almacenamiento inválida.");
        }
        Files.createDirectories(targetDir);

        String originalName = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo.pdf";
        String safeFileName = sanitizeFileName(originalName);
        String storedName = tipoAdjunto + "_" + LocalDateTime.now().format(FILE_TS_FMT) + "_" + safeFileName;
        Path targetFile = targetDir.resolve(storedName).normalize();
        if (!targetFile.startsWith(targetDir)) {
            throw new SecurityException("Nombre de archivo inválido.");
        }

        Files.copy(archivo.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);

        String relativePath = this.baseDir.relativize(targetFile).toString().replace("\\", "/");

        ApoyoTerapeuticoAdjunto adjunto = ApoyoTerapeuticoAdjunto.builder()
                .ingreso(ingreso)
                .tipoAdjunto(tipoAdjunto)
                .nombreArchivoOriginal(originalName)
                .rutaAlmacenamiento(relativePath)
                .contentType(archivo.getContentType())
                .tamanoBytes(archivo.getSize())
                .fechaDocumento(fechaDoc)
                .fechaVencimiento(fechaVenc)
                .vigente(true)
                .sincronizadoKubapp(false)
                .observacion(observacion != null && !observacion.isBlank() ? observacion.trim() : null)
                .subidoPor(usuario != null ? usuario.getNombreCompleto() : "Sistema")
                .fechaSubida(LocalDateTime.now())
                .build();

        adjuntoRepository.save(adjunto);
        ingreso.setFechaActualizacion(LocalDateTime.now());
        ingresoRepository.save(ingreso);

        return obtenerDetalle(ingresoId);
    }

    @Transactional(readOnly = true)
    public ApoyoTerapeuticoAdjunto obtenerAdjuntoEntidad(Long adjuntoId) {
        return adjuntoRepository.findById(adjuntoId)
                .orElseThrow(() -> new IllegalArgumentException("Adjunto no encontrado con ID: " + adjuntoId));
    }

    public Path resolverRutaArchivo(ApoyoTerapeuticoAdjunto adjunto) {
        Path resolved = this.baseDir.resolve(adjunto.getRutaAlmacenamiento()).normalize();
        if (!resolved.startsWith(this.baseDir)) {
            throw new SecurityException("Acceso denegado a ruta fuera de Apoyo Terapéutico.");
        }
        return resolved;
    }

    @Transactional
    public Map<String, Object> eliminarAdjunto(Long adjuntoId) {
        ApoyoTerapeuticoAdjunto adj = adjuntoRepository.findById(adjuntoId)
                .orElseThrow(() -> new IllegalArgumentException("Adjunto no encontrado con ID: " + adjuntoId));
        Long ingresoId = adj.getIngreso().getId();
        String tipo = adj.getTipoAdjunto();
        boolean eraVigente = Boolean.TRUE.equals(adj.getVigente());

        deleteFileQuietly(adj.getRutaAlmacenamiento());
        adjuntoRepository.delete(adj);
        adjuntoRepository.flush();

        // Si se eliminó el vigente, promover automáticamente la versión anterior más reciente (si existe)
        if (eraVigente) {
            List<ApoyoTerapeuticoAdjunto> restantes = adjuntoRepository
                    .findByIngresoIdAndTipoAdjuntoOrderByFechaSubidaDescIdDesc(ingresoId, tipo);
            if (!restantes.isEmpty()) {
                ApoyoTerapeuticoAdjunto ultimo = restantes.get(0);
                ultimo.setVigente(true);
                adjuntoRepository.save(ultimo);
            }
        }
        return obtenerDetalle(ingresoId);
    }

    @Transactional(readOnly = true)
    public byte[] generarZipAdjuntos(Long ingresoId) throws IOException {
        ApoyoTerapeuticoIngreso ingreso = ingresoRepository.findById(ingresoId)
                .orElseThrow(() -> new IllegalArgumentException("Ingreso no encontrado con ID: " + ingresoId));
        List<ApoyoTerapeuticoAdjunto> adjuntos = adjuntoRepository.findByIngresoIdOrderByFechaSubidaDescIdDesc(ingresoId);

        List<ApoyoTerapeuticoAdjunto> vigentes = adjuntos.stream()
                .filter(a -> Boolean.TRUE.equals(a.getVigente()))
                .collect(Collectors.toList());

        if (vigentes.isEmpty()) {
            throw new IllegalArgumentException("El ingreso " + ingreso.getNumeroIngreso() + " no tiene adjuntos cargados todavía.");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            Set<String> usedNames = new HashSet<>();
            for (ApoyoTerapeuticoAdjunto adj : vigentes) {
                Path filePath = resolverRutaArchivo(adj);
                if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) continue;

                String ext = "";
                String orig = adj.getNombreArchivoOriginal() != null ? adj.getNombreArchivoOriginal() : "";
                int dot = orig.lastIndexOf('.');
                if (dot >= 0) ext = orig.substring(dot);

                String cleanEntry = "adjuntos/" + adj.getTipoAdjunto() + "_" + sanitizeSegment(ingreso.getNumeroIngreso())
                        + "_" + adj.getFechaDocumento() + ext;
                int counter = 1;
                String finalEntry = cleanEntry;
                while (usedNames.contains(finalEntry)) {
                    finalEntry = "adjuntos/" + adj.getTipoAdjunto() + "_" + (++counter) + "_" + sanitizeFileName(orig);
                }
                usedNames.add(finalEntry);

                zos.putNextEntry(new ZipEntry(finalEntry));
                Files.copy(filePath, zos);
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    private Map<String, Object> mapIngresoResumen(ApoyoTerapeuticoIngreso ing, List<ApoyoTerapeuticoAdjunto> adjuntos) {
        ApoyoTerapeuticoAdjunto consentimiento = findVigente(adjuntos, TIPO_CONSENTIMIENTO);
        ApoyoTerapeuticoAdjunto planilla = findVigente(adjuntos, TIPO_PLANILLA);
        ApoyoTerapeuticoAdjunto planCasero = findVigente(adjuntos, TIPO_PLAN_CASERO);

        int totalCargados = (consentimiento != null ? 1 : 0) + (planilla != null ? 1 : 0) + (planCasero != null ? 1 : 0);

        String estadoPlanCasero = "SIN_CARGAR";
        Long diasRestantesPlanCasero = null;
        LocalDate fechaVencPlanCasero = null;
        if (planCasero != null) {
            fechaVencPlanCasero = planCasero.getFechaVencimiento() != null
                    ? planCasero.getFechaVencimiento()
                    : planCasero.getFechaDocumento().plusMonths(2);
            diasRestantesPlanCasero = ChronoUnit.DAYS.between(LocalDate.now(), fechaVencPlanCasero);
            if (diasRestantesPlanCasero < 0) {
                estadoPlanCasero = "VENCIDO";
            } else if (diasRestantesPlanCasero <= 15) {
                estadoPlanCasero = "POR_VENCER";
            } else {
                estadoPlanCasero = "VIGENTE";
            }
        }

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", ing.getId());
        dto.put("numeroIngreso", ing.getNumeroIngreso());
        dto.put("tipoDocumentoPaciente", ing.getTipoDocumentoPaciente());
        dto.put("documentoPaciente", ing.getDocumentoPaciente());
        dto.put("nombrePaciente", ing.getNombrePaciente());
        dto.put("especialidad", ing.getEspecialidad());
        dto.put("sede", ing.getSede());
        dto.put("fechaIngreso", ing.getFechaIngreso() != null ? ing.getFechaIngreso().toString() : null);
        dto.put("estado", ing.getEstado());
        dto.put("origenDato", ing.getOrigenDato());
        dto.put("kubappRefId", ing.getKubappRefId());
        dto.put("observaciones", ing.getObservaciones());
        dto.put("creadoPor", ing.getCreadoPor());
        dto.put("fechaCreacion", ing.getFechaCreacion() != null ? ing.getFechaCreacion().toString() : null);
        dto.put("fechaActualizacion", ing.getFechaActualizacion() != null ? ing.getFechaActualizacion().toString() : null);

        dto.put("consentimiento", consentimiento != null ? mapAdjunto(consentimiento) : null);
        dto.put("planillaFirmas", planilla != null ? mapAdjunto(planilla) : null);
        dto.put("planCasero", planCasero != null ? mapAdjunto(planCasero) : null);
        dto.put("totalCargados", totalCargados);
        dto.put("totalAdjuntosHistorico", adjuntos.size());
        dto.put("estadoPlanCasero", estadoPlanCasero);
        dto.put("diasRestantesPlanCasero", diasRestantesPlanCasero);
        dto.put("fechaVencimientoPlanCasero", fechaVencPlanCasero != null ? fechaVencPlanCasero.toString() : null);
        return dto;
    }

    private ApoyoTerapeuticoAdjunto findVigente(List<ApoyoTerapeuticoAdjunto> list, String tipo) {
        for (ApoyoTerapeuticoAdjunto a : list) {
            if (tipo.equals(a.getTipoAdjunto()) && Boolean.TRUE.equals(a.getVigente())) {
                return a;
            }
        }
        return null;
    }

    private Map<String, Object> mapAdjunto(ApoyoTerapeuticoAdjunto a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("ingresoId", a.getIngreso() != null ? a.getIngreso().getId() : null);
        m.put("tipoAdjunto", a.getTipoAdjunto());
        m.put("nombreArchivoOriginal", a.getNombreArchivoOriginal());
        m.put("contentType", a.getContentType());
        m.put("tamanoBytes", a.getTamanoBytes());
        m.put("fechaDocumento", a.getFechaDocumento() != null ? a.getFechaDocumento().toString() : null);
        m.put("fechaVencimiento", a.getFechaVencimiento() != null ? a.getFechaVencimiento().toString() : null);
        m.put("vigente", a.getVigente());
        m.put("sincronizadoKubapp", a.getSincronizadoKubapp());
        m.put("fechaSyncKubapp", a.getFechaSyncKubapp() != null ? a.getFechaSyncKubapp().toString() : null);
        m.put("observacion", a.getObservacion());
        m.put("subidoPor", a.getSubidoPor());
        m.put("fechaSubida", a.getFechaSubida() != null ? a.getFechaSubida().toString() : null);
        return m;
    }

    private void deleteFileQuietly(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            Path p = this.baseDir.resolve(relativePath).normalize();
            if (p.startsWith(this.baseDir) && Files.exists(p)) {
                Files.deleteIfExists(p);
            }
        } catch (Exception e) {
            log.warn("No se pudo eliminar archivo físico {}: {}", relativePath, e.getMessage());
        }
    }

    private String sanitizeSegment(String raw) {
        if (raw == null || raw.isBlank()) return "SIN_NUMERO";
        return raw.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String sanitizeFileName(String raw) {
        if (raw == null || raw.isBlank()) return "archivo.pdf";
        String base = Paths.get(raw).getFileName().toString();
        String cleaned = base.replaceAll("[^a-zA-Z0-9._-]", "_");
        return cleaned.length() > 120 ? cleaned.substring(cleaned.length() - 120) : cleaned;
    }

    private String strVal(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() || "null".equalsIgnoreCase(s) ? null : s;
    }

    private String valOrDefault(String val, String def) {
        return (val != null && !val.isBlank()) ? val : def;
    }

    private LocalDate parseLocalDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String clean = raw.trim();
        int sp = clean.indexOf(' ');
        if (sp > 0) clean = clean.substring(0, sp);
        int t = clean.indexOf('T');
        if (t > 0) clean = clean.substring(0, t);
        try {
            if (clean.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                return LocalDate.parse(clean);
            }
            if (clean.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
                String[] parts = clean.split("/");
                return LocalDate.of(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
            }
            if (clean.matches("^\\d{2}-\\d{2}-\\d{4}$")) {
                String[] parts = clean.split("-");
                return LocalDate.of(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]), Integer.parseInt(parts[0]));
            }
        } catch (Exception ignored) {}
        return null;
    }
}
