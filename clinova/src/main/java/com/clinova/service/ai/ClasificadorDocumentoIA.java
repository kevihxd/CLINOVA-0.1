package com.clinova.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

@Service
public class ClasificadorDocumentoIA {

    @Value("${gemini.api.key}")
    private String apiKey;

    // Se fuerza el modelo 1.5-flash que es el único con soporte nativo para PDF inline
    private final String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String clasificarDocumento(byte[] pdfBytes) {
        try {
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

            ObjectNode rootNode = objectMapper.createObjectNode();
            ArrayNode contents = rootNode.putArray("contents");
            ObjectNode content = contents.addObject();
            ArrayNode parts = content.putArray("parts");

            String categorias = "Acta de grado Profesional, Acta grado de Bachiller, Acta grado Título Especialista, Afiliación ARL, Afiliación EPS, Afiliación Pensión, Antecedentes, Caja de compensación, Carnet vacunación, Cédula de ciudadanía, Certificación Bancaria, Certificado de curso básico de reanimación cardiopulmonar, Certificado de Formación, Certificado Experiencia Laboral, Cesantías, Contrato, Convalidación, Diploma Bachiller, Exámenes Medico Ocupacional, Formato de requisitos para hoja de vida y contratación, Fundación, Incapacidades, Libreta Militar, Paz y salvo, PESV, Póliza de responsabilidad Civil, Preaviso, Procesos disciplinarios, Resolución expedida por Instituto departamental de salud, RUT, Soportes contables, Tarjeta profesional, Título de profesional, Título Especialista, Vacaciones, Varios y/o anexos, Verificación en Rethus, Otros Soportes";

            ObjectNode textPart = parts.addObject();
            textPart.put("text", "Eres un auditor experto de Recursos Humanos. Analiza este documento y clasifícalo SOLO con UNA de estas opciones exactas (sin puntos finales ni comillas): " + categorias + ". Si el documento no coincide con ninguna o no estás seguro, responde exactamente 'Otros Soportes'.");

            ObjectNode inlineDataPart = parts.addObject();
            ObjectNode inlineData = inlineDataPart.putObject("inline_data");
            inlineData.put("mime_type", "application/pdf");
            inlineData.put("data", base64Pdf);

            String requestBody = objectMapper.writeValueAsString(rootNode);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode responseJson = objectMapper.readTree(response.body());
                String clasificacion = responseJson.path("candidates")
                        .get(0).path("content").path("parts")
                        .get(0).path("text").asText().trim();

                return clasificacion.replace("\n", "").replace("\r", "");
            } else {
                System.err.println("Error de Gemini API: " + response.body());
                return "Otros Soportes";
            }

        } catch (Exception e) {
            System.err.println("Error procesando documento con IA: " + e.getMessage());
            return "Otros Soportes";
        }
    }
}