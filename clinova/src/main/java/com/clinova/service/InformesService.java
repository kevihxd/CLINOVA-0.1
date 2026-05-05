package com.clinova.service;

import com.clinova.dto.ReporteTalentoHumanoDTO;
import com.clinova.dto.ReporteVacunacionDTO;
import com.clinova.dto.ReporteIncapacidadDTO;
import com.clinova.entity.Usuario;
import com.clinova.entity.Incapacidad;
import com.clinova.repository.UsuarioRepository;
import com.clinova.repository.HojaVidaRepository;
import com.clinova.repository.IncapacidadRepository;
import com.clinova.entity.HojaVida;
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

    @Transactional(readOnly = true)
    public List<ReporteVacunacionDTO> generarReporteVacunacion() {
        return usuarioRepository.findAll().stream()
                .map(this::mapearVacunacion)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReporteTalentoHumanoDTO> generarReporteTalentoHumano() {
        return usuarioRepository.findAll().stream()
                .map(this::mapearTalentoHumano)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReporteIncapacidadDTO> generarReporteIncapacidades() {
        return incapacidadRepository.findAll().stream()
                .map(this::mapearIncapacidad)
                .collect(Collectors.toList());
    }

    private ReporteVacunacionDTO mapearVacunacion(Usuario u) {
        String cedula = buscarValorGlobal(u, "cedula", "numerodocumento", "identificacion");
        String nombres = buscarValorGlobal(u, "nombres", "nombre");
        String apellidos = buscarValorGlobal(u, "apellidos", "apellido");
        String perfil = "";
        String detalle = "";

        HojaVida hv = hojaVidaRepository.findByUsuario_Id(u.getId()).orElse(null);
        if (hv != null) {
            if (hv.getCedula() != null && !hv.getCedula().isEmpty()) cedula = hv.getCedula();
            if (hv.getNombres() != null && !hv.getNombres().isEmpty()) nombres = hv.getNombres();
            if (hv.getApellidos() != null && !hv.getApellidos().isEmpty()) apellidos = hv.getApellidos();
            if (hv.getPerfilVacunacion() != null && !hv.getPerfilVacunacion().isEmpty()) perfil = hv.getPerfilVacunacion();
            if (hv.getDetalleVacunas() != null && !hv.getDetalleVacunas().isEmpty()) detalle = hv.getDetalleVacunas();
        }

        if (perfil.isEmpty() && u.getPersona() != null && u.getPersona().getPerfilVacunacion() != null) {
            perfil = u.getPersona().getPerfilVacunacion();
        }
        
        if (perfil.isEmpty()) perfil = buscarValorGlobal(u, "cargo", "rol");

        if (cedula.isEmpty()) cedula = String.valueOf(u.getId());
        if (nombres.isEmpty()) nombres = "Usuario " + u.getId();
        if (detalle.isEmpty()) detalle = "Sin registro";
        if (perfil.isEmpty()) perfil = "No Asignado";

        return new ReporteVacunacionDTO(
                cedula, nombres, apellidos, perfil, detalle, determinarSemaforo(detalle)
        );
    }

    private ReporteTalentoHumanoDTO mapearTalentoHumano(Usuario u) {
        String cedula = buscarValorGlobal(u, "cedula", "numerodocumento", "identificacion");
        String nombres = buscarValorGlobal(u, "nombres", "nombre");
        String apellidos = buscarValorGlobal(u, "apellidos", "apellido");
        String cargo = buscarValorGlobal(u, "cargo", "perfil");
        String contrato = "";
        String estado = buscarValorGlobal(u, "estado", "activo");

        HojaVida hv = hojaVidaRepository.findByUsuario_Id(u.getId()).orElse(null);
        if (hv != null) {
            if (hv.getCedula() != null && !hv.getCedula().isEmpty()) cedula = hv.getCedula();
            if (hv.getNombres() != null && !hv.getNombres().isEmpty()) nombres = hv.getNombres();
            if (hv.getApellidos() != null && !hv.getApellidos().isEmpty()) apellidos = hv.getApellidos();
            if (hv.getTipoContrato() != null && !hv.getTipoContrato().isEmpty()) contrato = hv.getTipoContrato();
            if (hv.getEstado() != null && !hv.getEstado().isEmpty()) estado = hv.getEstado();
        }

        if (contrato.isEmpty()) contrato = buscarValorGlobal(u, "contrato", "tipo");

        if (cedula.isEmpty()) cedula = String.valueOf(u.getId());
        if (nombres.isEmpty()) nombres = "Usuario " + u.getId();
        if (cargo.isEmpty()) cargo = "No Definido";
        if (contrato.isEmpty()) contrato = "No Definido";
        if (estado.isEmpty() || estado.equalsIgnoreCase("true")) estado = "ACTIVO";
        if (estado.equalsIgnoreCase("false")) estado = "INACTIVO";

        return new ReporteTalentoHumanoDTO(
                cedula, nombres, apellidos, cargo, contrato, estado.toUpperCase()
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
            if (hv.getCedula() != null && !hv.getCedula().isEmpty()) cedula = hv.getCedula();
            if (hv.getNombres() != null && !hv.getNombres().isEmpty()) nombres = hv.getNombres();
            if (hv.getApellidos() != null && !hv.getApellidos().isEmpty()) apellidos = hv.getApellidos();
        }

        if (cedula.isEmpty()) cedula = String.valueOf(u.getId());
        if (tipoDoc.isEmpty()) tipoDoc = "CC";
        
        String nombreCompleto = nombres + (apellidos.isEmpty() ? "" : " " + apellidos);
        String[] cartera = inc.calcularCartera();

        return new ReporteIncapacidadDTO(
                nombreCompleto,
                tipoDoc,
                cedula,
                cargo,
                inc.getEpsArl(),
                inc.getTipoIncapacidad(),
                inc.getCodigo(),
                inc.getDx(),
                inc.getFechaInicio(),
                inc.getFechaFin(),
                inc.getDiasOtorgados(),
                inc.getDiasAprobados(),
                inc.getFechaReporteTH(),
                inc.getFechaRadicado(),
                inc.getEstado(),
                inc.getNumeroRadicacion(),
                inc.getIbc(),
                inc.getDiasPagadosIps(),
                inc.getValorLiquidadoIps(),
                inc.getDiasPagadosEps(),
                inc.getValorLiquidadoEps(),
                inc.getDiasPagadosArl(),
                cartera[0], // 30
                cartera[1], // 60
                cartera[2], // 90
                cartera[3], // 180
                inc.getObservaciones(),
                inc.getRutaArchivo() != null ? "Soporte Adjunto" : "",
                inc.getValorPago(),
                inc.getFechaPago(),
                inc.getNumeroComprobantePago()
        );
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
        if (detalle == null || detalle.trim().isEmpty() || detalle.equalsIgnoreCase("Sin registro")) {
            return "ROJO";
        }
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
                            if (f != null && !f.asText().trim().isEmpty()) {
                                dosisAplicadas++;
                            }
                        }
                    }
                    if (dosisAplicadas < dosisReq) {
                        pendiente = true;
                    }
                    boolean reqRefuerzo = vac.has("requiereRefuerzo") && vac.get("requiereRefuerzo").asBoolean();
                    String fechaRef = vac.has("fechaRefuerzo") ? vac.get("fechaRefuerzo").asText() : "";
                    if (reqRefuerzo && (fechaRef == null || fechaRef.trim().isEmpty())) {
                        pendiente = true;
                    }
                }
                return pendiente ? "AMARILLO" : "VERDE";
            }
        } catch (Exception e) {
            // Fallback al análisis de texto si no es JSON válido
            String detLower = detalle.toLowerCase();
            if (detLower.contains("vencid")) return "ROJO";
            if (detLower.contains("pendiente") || detLower.contains("incompleto")) return "AMARILLO";
        }
        return "VERDE";
    }
}