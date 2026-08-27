package com.clinova.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record DocumentoListDTO(
        Long id,
        Long kawakId,
        String codigo,
        String nombre,
        String tipo,
        String proceso,
        String sede,
        String estado,
        String version,
        Integer mesesRevision,
        String metodoCreacion,
        String normas,
        String rutaArchivoLocal,
        String ubicacion,
        String ubicacionPdf,
        String fechaAprobacion,
        String fechaElaboracion,
        String fechaRevision,
        String elabora,
        String revisa,
        String aprueba,
        String controlCambios,
        String descripcion
) {
    public static String cleanUtf8(String text) {
        if (text == null) return "";
        String s = text;
        if (s.contains("Ã") || s.contains("ï¿½") || s.contains("")) {
            s = s.replace("Ã“", "Ó")
                 .replace("Ã³", "ó")
                 .replace("Ã‘", "Ñ")
                 .replace("Ã±", "ñ")
                 .replace("Ã\u0081", "Á")
                 .replace("Ã\u0089", "É")
                 .replace("Ã©", "é")
                 .replace("Ã\u008D", "Í")
                 .replace("Ã*", "í")
                 .replace("Ã\u009A", "Ú")
                 .replace("Ãº", "ú")
                 .replace("ï¿½", "")
                 .replace("", "");
        }
        return s.trim();
    }

    @JsonProperty("nombre")
    public String getNombreLimpio() {
        return cleanUtf8(nombre);
    }

    @JsonProperty("codigo")
    public String getCodigoLimpio() {
        return cleanUtf8(codigo);
    }

    @JsonProperty("proceso")
    public String getProcesoLimpio() {
        String p = cleanUtf8(proceso);
        if (p.equalsIgnoreCase("GESTIN ESTRATGICA") || p.equalsIgnoreCase("GESTION ESTRATEGICA")) return "GESTION ESTRATEGICA";
        if (p.equalsIgnoreCase("GESTION DE CALIDAD")) return "GESTION DE CALIDAD";
        return p;
    }

    @JsonProperty("sede")
    public String getSedeConDefault() {
        String s = cleanUtf8(sede);
        return (!s.isEmpty()) ? s : "IPS CLINICAL HOUSE";
    }

    @JsonProperty("normas")
    public String getNormasConDefault() {
        String n = cleanUtf8(normas);
        return (!n.isEmpty()) ? n : "1-ISO 9001:2015";
    }

    @JsonProperty("diasFaltantes")
    public Integer getDiasFaltantes() {
        String fechaBase = (fechaAprobacion != null && !fechaAprobacion.trim().isEmpty()) ? fechaAprobacion
                : (fechaRevision != null && !fechaRevision.trim().isEmpty()) ? fechaRevision
                : fechaElaboracion;
        int meses = (mesesRevision != null && mesesRevision > 0) ? mesesRevision : 12;

        LocalDate fecha = parseFlexibleDate(fechaBase);
        if (fecha != null) {
            LocalDate vencimiento = fecha.plusMonths(meses);
            return (int) ChronoUnit.DAYS.between(LocalDate.now(), vencimiento);
        }
        return null;
    }

    private static LocalDate parseFlexibleDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty() || "N/A".equalsIgnoreCase(dateStr.trim())) return null;
        String clean = dateStr.trim();
        int spaceIdx = clean.indexOf(' ');
        if (spaceIdx > 0) clean = clean.substring(0, spaceIdx);
        int tIdx = clean.indexOf('T');
        if (tIdx > 0) clean = clean.substring(0, tIdx);

        try {
            String[] parts = clean.split("[/-]");
            if (parts.length == 3) {
                int p0 = Integer.parseInt(parts[0]);
                int p1 = Integer.parseInt(parts[1]);
                int p2 = Integer.parseInt(parts[2]);

                int year, month, day;

                if (p0 > 1000) {
                    year = p0;
                    if (p1 > 12) { day = p1; month = p2; }
                    else { month = p1; day = p2; }
                } else if (p2 > 1000) {
                    year = p2;
                    if (p0 > 12) { day = p0; month = p1; }
                    else if (p1 > 12) { day = p1; month = p0; }
                    else { day = p0; month = p1; } // Default Latin America standard DD/MM/YYYY
                } else {
                    return null;
                }

                if (month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                    return LocalDate.of(year, month, day);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
