package com.clinova.integration.kawak;

import com.clinova.integration.kawak.dto.KawakActaDTO;
import com.clinova.integration.kawak.dto.KawakDocumentoDTO;
import com.clinova.integration.kawak.dto.KawakUsuarioDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cliente HTTP para la API de Kawak.
 * Maneja paginación automática (totalPages) y agrega el Bearer token en cada request.
 *
 * Formato de respuesta Kawak:
 * {
 *   "code": 200,
 *   "status": "success",
 *   "message": {
 *     "pagination": { "totalRows": 505, "totalPages": 11, "currentPage": 5, "perPage": 50 },
 *     "data": [ {...}, {...} ]
 *   }
 * }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KawakApiClient {

    private final RestTemplate restTemplate;
    private final KawakAuthClient authClient;
    private final ObjectMapper objectMapper;

    @Value("${kawak.api.base-url}")
    private String baseUrl;

    // ── Métodos públicos por módulo ───────────────────────────────────────────

    public List<KawakActaDTO> obtenerActas() {
        return obtenerTodasLasPaginas("/api/v1/actas", new TypeReference<>() {});
    }

    public List<KawakDocumentoDTO> obtenerDocumentos() {
        return obtenerTodasLasPaginas("/api/v1/documentos", new TypeReference<>() {});
    }

    public List<KawakUsuarioDTO> obtenerUsuarios() {
        return obtenerTodasLasPaginas("/api/v1/usuarios", new TypeReference<>() {});
    }

    // ── Paginación automática ─────────────────────────────────────────────────

    private <T> List<T> obtenerTodasLasPaginas(String endpoint, TypeReference<List<T>> type) {
        List<T> todos = new ArrayList<>();
        int pagina = 1;
        int totalPaginas = 1;

        do {
            try {
                String url = baseUrl + endpoint + "?page=" + pagina;
                JsonNode respuesta = llamarGet(url);

                if (respuesta == null) break;

                // Extraer nodo de datos: message.data
                JsonNode messageNode = respuesta.path("message");
                JsonNode dataNode = messageNode.path("data");

                if (dataNode.isMissingNode() || !dataNode.isArray()) {
                    log.warn("No se encontró 'message.data' en la respuesta de {}", endpoint);
                    break;
                }

                List<T> pagina_datos = objectMapper.convertValue(dataNode, type);
                todos.addAll(pagina_datos);
                log.info("Página {}/{} de {} — {} registros", pagina, totalPaginas, endpoint, pagina_datos.size());

                // Leer paginación
                JsonNode paginacionNode = messageNode.path("pagination");
                if (!paginacionNode.isMissingNode()) {
                    totalPaginas = paginacionNode.path("totalPages").asInt(1);
                }

                pagina++;
            } catch (Exception e) {
                log.error("Error obteniendo página {} de {}: {}", pagina, endpoint, e.getMessage(), e);
                break;
            }
        } while (pagina <= totalPaginas);

        log.info("Total obtenido de {}: {} registros", endpoint, todos.size());
        return todos;
    }

    private JsonNode llamarGet(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(authClient.getToken());
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    JsonNode.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Respuesta vacía o error en {}: {}", url, response.getStatusCode());
                return null;
            }

            return response.getBody();
        } catch (Exception e) {
            log.error("Error al llamar a {}: {}", url, e.getMessage(), e);
            throw new RuntimeException("Error al conectar con Kawak en " + url + ": " + e.getMessage());
        }
    }
}
