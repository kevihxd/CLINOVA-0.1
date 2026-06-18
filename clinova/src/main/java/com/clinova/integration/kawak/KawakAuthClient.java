package com.clinova.integration.kawak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

/**
 * Obtiene y cachea el token JWT de la API de Kawak.
 *
 * Endpoint real: POST /login
 * Body: { "email": "...", "password": "..." }
 * Respuesta: { "code": 200, "status": "success", "message": { "Authorization": "Bearer eyJ..." } }
 */
@Slf4j
@Component
public class KawakAuthClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String email;
    private final String password;

    private String cachedToken;
    private Instant tokenExpiry = Instant.EPOCH;

    public KawakAuthClient(
            RestTemplate restTemplate,
            @Value("${kawak.api.base-url}") String baseUrl,
            @Value("${kawak.api.username:}") String email,
            @Value("${kawak.api.password:}") String password) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.email = email;
        this.password = password;
    }

    public String getToken() {
        if (cachedToken == null || Instant.now().isAfter(tokenExpiry)) {
            renovarToken();
        }
        return cachedToken;
    }

    private void renovarToken() {
        log.info("Renovando token JWT de Kawak...");
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> body = Map.of("email", email, "password", password);

            ResponseEntity<KawakLoginResponse> response = restTemplate.exchange(
                    baseUrl + "/login",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    KawakLoginResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && response.getBody().getAuthorization() != null) {

                // Kawak devuelve "Bearer eyJ...", necesitamos solo el token
                String authHeader = response.getBody().getAuthorization();
                cachedToken = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

                // Expira en 23h (los tokens de Kawak son de 7 días según el JWT de ejemplo)
                tokenExpiry = Instant.now().plusSeconds(23 * 60 * 60);
                log.info("Token de Kawak renovado exitosamente");
            } else {
                throw new RuntimeException("Respuesta inválida al autenticar con Kawak");
            }
        } catch (Exception e) {
            log.error("Error al obtener token de Kawak: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo autenticar con la API de Kawak: " + e.getMessage());
        }
    }

    // ── Estructura de respuesta del login de Kawak ────────────────────────────

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KawakLoginResponse {
        // { "code": 200, "status": "success", "message": { "Authorization": "Bearer ..." } }
        @JsonProperty("message")
        private KawakAuthMessage message;

        public String getAuthorization() {
            return message != null ? message.authorization : null;
        }
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KawakAuthMessage {
        @JsonProperty("Authorization")
        private String authorization;
    }
}
