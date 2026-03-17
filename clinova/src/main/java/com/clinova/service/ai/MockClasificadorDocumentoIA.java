package com.clinova.service.ai;

import org.springframework.stereotype.Service;

@Service
public class MockClasificadorDocumentoIA implements ClasificadorDocumentoIA {

    @Override
    public String clasificarDocumento(byte[] paginaPdf) {
        // TODO: Integrar aquí el SDK de AWS Textract, Google Document AI o la API de OpenAI/Gemini
        // Por ahora, simulamos que detecta la primera página como "Cédula de ciudadanía"
        return "Cédula de ciudadanía";
    }
}