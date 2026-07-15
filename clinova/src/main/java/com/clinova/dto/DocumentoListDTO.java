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
        String fechaAprobacion
) {
    @com.fasterxml.jackson.annotation.JsonProperty("diasFaltantes")
    public Integer getDiasFaltantes() {
        if (fechaAprobacion != null && !fechaAprobacion.trim().isEmpty() && mesesRevision != null) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
                java.time.LocalDate fechaAprob = java.time.LocalDate.parse(fechaAprobacion, formatter);
                java.time.LocalDate vencimiento = fechaAprob.plusMonths(mesesRevision);
                return (int) java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), vencimiento);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
