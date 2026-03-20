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

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String clasificarDocumento(byte[] pdfBytes) {
        try {
            String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

            ObjectNode rootNode = objectMapper.createObjectNode();
            ArrayNode contents = rootNode.putArray("contents");
            ObjectNode content = contents.addObject();
            ArrayNode parts = content.putArray("parts");

            // Instrucción de texto
            ObjectNode textPart = parts.addObject();
            textPart.put("text", "Analiza el siguiente documento y clasifícalo SOLO con una de estas opciones exactas, sin puntos ni texto extra: Cédula de ciudadanía, Diploma académico, Acta de grado, Certificado laboral, Certificado de EPS, Certificado de pensión (AFP), Hoja de vida (CV), Tarjeta profesional, Antecedentes, Desconocido.");

            // Documento adjunto
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
                return "Desconocido";
            }

        } catch (Exception e) {
            System.err.println("Error procesando documento con IA: " + e.getMessage());
            return "Desconocido";
        }
    }
}