package com.clinova.dto;

/**
 * Data Transfer Object (DTO) optimizado para listar documentos.
 * Evita la transferencia de campos pesados (como textos largos, listas)
 * mejorando drásticamente los tiempos de respuesta del API.
 */
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
        String fechaRevision
) {
    @com.fasterxml.jackson.annotation.JsonProperty("diasFaltantes")
    public Integer getDiasFaltantes() {
        String fechaBase = (fechaAprobacion != null && !fechaAprobacion.trim().isEmpty()) ? fechaAprobacion
                : (fechaRevision != null && !fechaRevision.trim().isEmpty()) ? fechaRevision
                : fechaElaboracion;
        if (fechaBase != null && !fechaBase.trim().isEmpty() && mesesRevision != null) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                java.time.LocalDate fecha = java.time.LocalDate.parse(fechaBase, formatter);
                java.time.LocalDate vencimiento = fecha.plusMonths(mesesRevision);
                return (int) java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), vencimiento);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
