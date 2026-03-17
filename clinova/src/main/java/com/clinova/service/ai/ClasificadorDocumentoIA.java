package com.clinova.service.ai;

public interface ClasificadorDocumentoIA {
    /**
     * Integración con proveedor de IA (AWS Textract, Google Document AI, OpenAI, Gemini).
     * Recibe los bytes de una página PDF y retorna el tipo de documento clasificado
     * según la lista permitida (Ej: "Cédula de ciudadanía", "Acta de grado bachiller", etc.).
     */
    String clasificarDocumento(byte[] paginaPdf);
}