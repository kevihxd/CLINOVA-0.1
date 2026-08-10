package com.clinova.service;

import com.clinova.dto.*;
import com.clinova.entity.*;
import com.clinova.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InformesService {

    private final UsuarioRepository usuarioRepository;
    private final HojaVidaRepository hojaVidaRepository;
    private final IncapacidadRepository incapacidadRepository;
    private final CursoAsignadoRepository cursoAsignadoRepository;
    private final DocumentoRepository documentoRepository;

    private boolean isActivo(HojaVida hv) {
        if (hv == null) return false;
        String e = hv.getEstado();
        if (e == null || e.trim().isEmpty()) return true;
        String upper = e.trim().toUpperCase();
        return !upper.contains("INACTIVO") && !upper.contains("RETIRO") && !upper.contains("DESCARTA");
    }

    private boolean isActivoUsuario(Usuario u) {
        if (u == null) return false;
        HojaVida hv = hojaVidaRepository.findByUsuario_Id(u.getId()).orElse(null);
        return isActivo(hv);
    }

    @Transactional(readOnly = true)
    public List<ReporteVacunacionDTO> generarReporteVacunacion() {
        return hojaVidaRepository.findAll().stream()
                .filter(this::isActivo)
                .map(this::mapearVacunacion)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReporteTalentoHumanoDTO> generarReporteTalentoHumano() {
        return hojaVidaRepository.findAll().stream()
                .filter(this::isActivo)
                .map(this::mapearTalentoHumano)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReporteIncapacidadDTO> generarReporteIncapacidades() {
        return incapacidadRepository.findAll().stream()
                .filter(inc -> isActivoUsuario(inc.getUsuario()))
                .map(this::mapearIncapacidad)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReporteCursoDTO> generarReporteCursos() {
        return cursoAsignadoRepository.findAll().stream()
                .filter(ca -> isActivoUsuario(ca.getUsuario()))
                .map(this::mapearCurso)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReporteDocumentoDTO> generarReporteDocumentos() {
        return documentoRepository.findAll().stream()
                .map(this::mapearDocumento)
                .collect(Collectors.toList());
    }

    private String primerCargo(HojaVida hv) {
        if (hv == null || hv.getCargos() == null || hv.getCargos().isEmpty()) return "";
        return hv.getCargos().get(0).getNombre();
    }

    private String primeraSede(HojaVida hv) {
        if (hv == null || hv.getSedes() == null || hv.getSedes().isEmpty()) return "";
        return hv.getSedes().get(0).getNombre();
    }

    private String todosLosCargos(HojaVida hv) {
        if (hv == null || hv.getCargos() == null || hv.getCargos().isEmpty()) return "";
        return hv.getCargos().stream().map(Cargo::getNombre).collect(Collectors.joining(", "));
    }

    private String todasLasSedes(HojaVida hv) {
        if (hv == null || hv.getSedes() == null || hv.getSedes().isEmpty()) return "";
        return hv.getSedes().stream().map(Sede::getNombre).collect(Collectors.joining(", "));
    }

    private ReporteTalentoHumanoDTO mapearTalentoHumano(HojaVida hv) {
        return new ReporteTalentoHumanoDTO(
                nvl(hv.getCedula()),
                nvl(hv.getNombres()),
                nvl(hv.getApellidos()),
                todosLosCargos(hv),
                todasLasSedes(hv),
                nvl(hv.getTipoContrato()),
                nvl(hv.getEstado()),
                nvl(hv.getArl()),
                nvl(hv.getEps()),
                nvl(hv.getAfp()),
                nvl(hv.getCajaCompensacion()),
                hv.getSalario(),
                nvl(hv.getSubsidioTransporte()),
                hv.getFechaIngreso(),
                hv.getFechaRetiro(),
                nvl(hv.getMotivoRetiro()),
                nvl(hv.getTelefono()),
                nvl(hv.getCorreoElectronico()),
                nvl(hv.getDireccionResidencia()),
                nvl(hv.getContactoEmergencia()),
                nvl(hv.getTelefonoContactoEmergencia())
        );
    }

    private ReporteVacunacionDTO mapearVacunacion(HojaVida hv) {
        String detalle = nvl(hv.getDetalleVacunas());
        if (detalle.isEmpty()) detalle = "Sin registro";

        return new ReporteVacunacionDTO(
                nvl(hv.getCedula()),
                nvl(hv.getNombres()),
                nvl(hv.getApellidos()),
                todosLosCargos(hv),
                todasLasSedes(hv),
                nvl(hv.getArl()),
                nvl(hv.getEps()),
                nvl(hv.getPerfilVacunacion()),
                detalle,
                determinarSemaforo(detalle)
        );
    }

    private ReporteCursoDTO mapearCurso(CursoAsignado ca) {
        Usuario u = ca.getUsuario();
        HojaVida hv = hojaVidaRepository.findByUsuario_Id(u.getId()).orElse(null);

        String cedula = hv != null ? nvl(hv.getCedula()) : String.valueOf(u.getId());
        String nombres = hv != null ? nvl(hv.getNombres()) : nvl(buscarValorGlobal(u, "nombres", "nombre"));
        String apellidos = hv != null ? nvl(hv.getApellidos()) : nvl(buscarValorGlobal(u, "apellidos", "apellido"));

        CursoMaestro cm = ca.getCursoMaestro();

        return new ReporteCursoDTO(
                cedula,
                nombres,
                apellidos,
                todosLosCargos(hv),
                todasLasSedes(hv),
                cm.getNombre(),
                nvl(cm.getDescripcion()),
                cm.getMesesVigencia(),
                nvl(ca.getEstado()),
                ca.getFechaRealizacion(),
                ca.getFechaExpiracion(),
                cm.getFechaLimiteGlobal(),
                nvl(ca.getCertificadoUrl())
        );
    }

    private ReporteIncapacidadDTO mapearIncapacidad(Incapacidad inc) {
        Usuario u = inc.getUsuario();
        String cedula = buscarValorGlobal(u, "cedula", "numerodocumento", "identificacion");
        String tipoDoc = buscarValorGlobal(u, "tipodocumento");
        String nombres = buscarValorGlobal(u, "nombres", "nombre");
        String apellidos = buscarValorGlobal(u, "apellidos", "apellido");
        String cargo = buscarValorGlobal(u, "cargo", "perfil");

        HojaVida hv = hojaVidaRepository.findByUsuario_Id(u.getId()).orElse(null);
        if (hv != null) {
            if (!nvl(hv.getCedula()).isEmpty()) cedula = hv.getCedula();
            if (!nvl(hv.getNombres()).isEmpty()) nombres = hv.getNombres();
            if (!nvl(hv.getApellidos()).isEmpty()) apellidos = hv.getApellidos();
            if (!todosLosCargos(hv).isEmpty()) cargo = todosLosCargos(hv);
        }

        if (cedula.isEmpty()) cedula = String.valueOf(u.getId());
        if (tipoDoc.isEmpty()) tipoDoc = "CC";

        String nombreCompleto = nombres + (apellidos.isEmpty() ? "" : " " + apellidos);
        String[] cartera = inc.calcularCartera();

        return new ReporteIncapacidadDTO(
                nombreCompleto, tipoDoc, cedula, cargo,
                inc.getEpsArl(), inc.getTipoIncapacidad(), inc.getCodigo(), inc.getDx(),
                inc.getFechaInicio(), inc.getFechaFin(), inc.getDiasOtorgados(), inc.getDiasAprobados(),
                inc.getFechaReporteTH(), inc.getFechaRadicado(), inc.getEstado(), inc.getNumeroRadicacion(),
                inc.getIbc(), inc.getDiasPagadosIps(), inc.getValorLiquidadoIps(),
                inc.getDiasPagadosEps(), inc.getValorLiquidadoEps(), inc.getDiasPagadosArl(),
                cartera[0], cartera[1], cartera[2], cartera[3],
                inc.getObservaciones(),
                inc.getRutaArchivo() != null ? "Soporte Adjunto" : "",
                inc.getValorPago(), inc.getFechaPago(), inc.getNumeroComprobantePago()
        );
    }

    private ReporteDocumentoDTO mapearDocumento(Documento doc) {
        return new ReporteDocumentoDTO(
                doc.getId(),
                nvl(doc.getCodigo()),
                nvl(doc.getNombre()),
                nvl(doc.getTipo()),
                nvl(doc.getProceso()),
                nvl(doc.getSede()),
                nvl(doc.getVersion()),
                nvl(doc.getEstado()),
                nvl(doc.getFechaElaboracion()),
                nvl(doc.getFechaRevision()),
                nvl(doc.getFechaAprobacion()),
                nvl(doc.getElabora()),
                nvl(doc.getRevisa()),
                nvl(doc.getAprueba()),
                nvl(doc.getMetodoCreacion()),
                nvl(doc.getAlcance()),
                nvl(doc.getConfidencialidad()),
                nvl(doc.getNormas()),
                doc.getMesesRevision(),
                nvl(doc.getVisualizacion())
        );
    }

    private String nvl(String val) {
        return val != null ? val : "";
    }

    private String buscarValorGlobal(Object obj, String... keywords) {
        if (obj == null) return "";
        try {
            for (Method m : obj.getClass().getMethods()) {
                if (esMetodoValido(m)) {
                    for (String kw : keywords) {
                        if (m.getName().toLowerCase().contains(kw)) {
                            Object val = m.invoke(obj);
                            if (esValorValido(val)) return String.valueOf(val);
                        }
                    }
                }
            }
            for (Method m : obj.getClass().getMethods()) {
                if (esMetodoValido(m)) {
                    Object relObj = m.invoke(obj);
                    if (relObj != null && relObj.getClass().getName().startsWith("com.clinova.entity")) {
                        for (Method relM : relObj.getClass().getMethods()) {
                            if (esMetodoValido(relM)) {
                                for (String kw : keywords) {
                                    if (relM.getName().toLowerCase().contains(kw)) {
                                        Object val = relM.invoke(relObj);
                                        if (esValorValido(val)) return String.valueOf(val);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private boolean esMetodoValido(Method m) {
        return (m.getName().startsWith("get") || m.getName().startsWith("is")) && m.getParameterCount() == 0;
    }

    private boolean esValorValido(Object val) {
        return val != null && (val instanceof String || val instanceof Number || val instanceof Boolean);
    }

    private String determinarSemaforo(String detalle) {
        if (detalle == null || detalle.trim().isEmpty() || detalle.equalsIgnoreCase("Sin registro")) return "ROJO";
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(detalle);
            if (root.isArray()) {
                if (root.isEmpty()) return "ROJO";
                boolean pendiente = false;
                for (com.fasterxml.jackson.databind.JsonNode vac : root) {
                    int dosisReq = vac.has("dosisRequeridas") ? vac.get("dosisRequeridas").asInt() : 0;
                    int dosisAplicadas = 0;
                    if (vac.has("fechas") && vac.get("fechas").isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode f : vac.get("fechas")) {
                            if (f != null && !f.asText().trim().isEmpty()) dosisAplicadas++;
                        }
                    }
                    if (dosisAplicadas < dosisReq) pendiente = true;
                    boolean reqRefuerzo = vac.has("requiereRefuerzo") && vac.get("requiereRefuerzo").asBoolean();
                    String fechaRef = vac.has("fechaRefuerzo") ? vac.get("fechaRefuerzo").asText() : "";
                    if (reqRefuerzo && (fechaRef == null || fechaRef.trim().isEmpty())) pendiente = true;
                }
                return pendiente ? "AMARILLO" : "VERDE";
            }
        } catch (Exception e) {
            String d = detalle.toLowerCase();
            if (d.contains("vencid")) return "ROJO";
            if (d.contains("pendiente") || d.contains("incompleto")) return "AMARILLO";
        }
        return "VERDE";
    }
}