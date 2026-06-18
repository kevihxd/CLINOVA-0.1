package com.clinova.integration.kawak;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

/**
 * Cliente HTTP para acceder al servidor de Backups de Kawak y descargar archivos físicos.
 * Usa Autenticación Básica (HTTP Basic Auth).
 */
@Slf4j
@Component
public class KawakBackupClient {

    private final RestTemplate restTemplate;
    private final String backupUrl;
    private final String authHeader;
    private final String localUploadDir;

    // Extensiones comunes que Kawak usa para documentos
    private static final List<String> EXTENSIONES_SOPORTADAS = List.of(".pdf", ".docx", ".doc", ".xlsx", ".xls", ".pptx");

    public KawakBackupClient(
            RestTemplate restTemplate,
            @Value("${kawak.backup.url:https://www.kawak.com.co/backup/clinicalhouse/DATA/files/Documentos/}") String backupUrl,
            @Value("${kawak.backup.username:clinicalhouse}") String username,
            @Value("${kawak.backup.password:}") String password,
            @Value("${app.upload.dir:uploads/documentos/}") String localUploadDir) {
        this.restTemplate = restTemplate;
        this.backupUrl = backupUrl.endsWith("/") ? backupUrl : backupUrl + "/";
        this.localUploadDir = localUploadDir;
        
        // Preparar HTTP Basic Auth
        String creds = username + ":" + password;
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(creds.getBytes());
        
        // Crear directorio local si no existe
        try {
            Files.createDirectories(Paths.get(this.localUploadDir));
        } catch (Exception e) {
            log.error("No se pudo crear el directorio local de subidas: {}", localUploadDir, e);
        }
    }

    /**
     * Intenta descargar el archivo físico de un documento dado su ID de Kawak.
     * Prueba varias extensiones hasta encontrar el archivo correcto.
     * 
     * @param kawakId El ID del documento en Kawak (ej. 1000)
     * @return DownloadResult con la ruta local y extensión, o null si no se encontró.
     */
    public DownloadResult descargarDocumento(Long kawakId) {
        if (kawakId == null) return null;

        // Kawak agrupa los archivos en subcarpetas. A veces es /1/, /2/, etc. 
        // Según lo observado, todos los documentos podrían estar en /1/ o en la raíz, 
        // pero la prueba manual mostró "Documentos/1/". Vamos a probar en Documentos/1/ primero.
        // Si no existe, probamos en Documentos/ raíz.
        List<String> subCarpetas = List.of("1/", "");

        for (String subCarpeta : subCarpetas) {
            for (String ext : EXTENSIONES_SOPORTADAS) {
                String fileName = kawakId + ext;
                String fileUrl = backupUrl + subCarpeta + fileName;

                log.debug("Intentando descargar archivo: {}", fileUrl);
                
                try {
                    DownloadResult result = ejecutarDescarga(fileUrl, kawakId, ext);
                    if (result != null) {
                        return result; // ¡Archivo encontrado y descargado!
                    }
                } catch (Exception e) {
                    log.error("Error al intentar descargar {}: {}", fileUrl, e.getMessage());
                }
            }
        }

        log.warn("No se encontró archivo para el documento con kawakId = {}", kawakId);
        return null;
    }

    private DownloadResult ejecutarDescarga(String url, Long kawakId, String extension) {
        RequestCallback requestCallback = request -> request.getHeaders().add("Authorization", authHeader);

        ResponseExtractor<DownloadResult> responseExtractor = response -> {
            if (response.getStatusCode() == HttpStatus.OK) {
                Path localPath = Paths.get(localUploadDir, kawakId + extension);
                try (FileOutputStream out = new FileOutputStream(localPath.toFile())) {
                    StreamUtils.copy(response.getBody(), out);
                }
                log.info("Archivo descargado exitosamente: {}", localPath.toAbsolutePath());
                return new DownloadResult(localPath.toString(), extension);
            }
            return null; // 404 Not Found u otro error (extensión incorrecta)
        };

        try {
            return restTemplate.execute(url, HttpMethod.GET, requestCallback, responseExtractor);
        } catch (Exception e) {
            // Un 404 lanzará excepción en RestTemplate a menos que configuremos ErrorHandler
            // Atrapamos la excepción y devolvemos null para que siga intentando con la siguiente extensión
            return null; 
        }
    }

    public record DownloadResult(String localPath, String extension) {}
}
