package com.clinova.service;

import com.clinova.dto.SoporteResponseDTO;
import com.clinova.entity.HojaVida;
import com.clinova.entity.Soporte;
import com.clinova.repository.HojaVidaRepository;
import com.clinova.repository.SoporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class SoporteILovePdfService {

    private final SoporteRepository soporteRepository;
    private final HojaVidaRepository hojaVidaRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    private final String PUBLIC_KEY = "project_public_faa4f9d6756326f8e8876e78452bd2d8_RvYKq435b5aff9f3bcc67e7efd23d862c2c51";
    private final String RUTA_BASE_SOPORTES = "uploads/soportes/";

    @Transactional
    public List<SoporteResponseDTO> dividirYGuardarPdf(Long hojaVidaId, MultipartFile archivoLote, String modo, String valor) {
        HojaVida hojaVida = hojaVidaRepository.findById(hojaVidaId)
                .orElseThrow(() -> new RuntimeException("Hoja de vida no encontrada"));

        List<SoporteResponseDTO> soportesProcesados = new ArrayList<>();

        try {
            // 1. Autenticación
            Map<String, String> authReq = new HashMap<>();
            authReq.put("public_key", PUBLIC_KEY);
            ResponseEntity<Map> authRes = restTemplate.postForEntity("https://api.ilovepdf.com/v1/auth", authReq, Map.class);
            String token = (String) authRes.getBody().get("token");

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);

            // 2. Iniciar Tarea de División
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<Map> startRes = restTemplate.exchange("https://api.ilovepdf.com/v1/start/split", HttpMethod.GET, requestEntity, Map.class);
            String server = (String) startRes.getBody().get("server");
            String taskId = (String) startRes.getBody().get("task");

            // 3. Subir Archivo
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("task", taskId);
            body.add("file", new ByteArrayResource(archivoLote.getBytes()) {
                @Override
                public String getFilename() { return archivoLote.getOriginalFilename() != null ? archivoLote.getOriginalFilename() : "doc.pdf"; }
            });

            HttpHeaders uploadHeaders = new HttpHeaders();
            uploadHeaders.setBearerAuth(token);
            uploadHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            ResponseEntity<Map> uploadRes = restTemplate.postForEntity("https://" + server + "/v1/upload", new HttpEntity<>(body, uploadHeaders), Map.class);
            String serverFilename = (String) uploadRes.getBody().get("server_filename");

            // 4. Configuración dinámica (Process)
            Map<String, Object> processReq = new HashMap<>();
            processReq.put("task", taskId);
            processReq.put("tool", "split");

            if ("ranges".equalsIgnoreCase(modo)) {
                processReq.put("ranges", valor);
            } else if ("remove_pages".equalsIgnoreCase(modo)) {
                processReq.put("remove_pages", valor);
            } else {
                // Default: fixed_range
                processReq.put("fixed_range", Integer.parseInt(valor));
            }

            List<Map<String, String>> files = new ArrayList<>();
            Map<String, String> fileInfo = new HashMap<>();
            fileInfo.put("server_filename", serverFilename);
            fileInfo.put("filename", "procesado.pdf");
            files.add(fileInfo);
            processReq.put("files", files);

            HttpHeaders processHeaders = new HttpHeaders();
            processHeaders.setBearerAuth(token);
            processHeaders.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity("https://" + server + "/v1/process", new HttpEntity<>(processReq, processHeaders), Map.class);

            // 5. Descargar Resultado
            ResponseEntity<byte[]> downloadRes = restTemplate.exchange("https://" + server + "/v1/download/" + taskId, HttpMethod.GET, requestEntity, byte[].class);
            byte[] downloadedData = downloadRes.getBody();

            // 6. Almacenar localmente
            if (downloadedData != null && downloadedData.length > 2 && downloadedData[0] == 0x50 && downloadedData[1] == 0x4B) {
                extraerYGuardarZip(downloadedData, hojaVida, soportesProcesados);
            } else {
                guardarSoporteIndividual(downloadedData, hojaVida, soportesProcesados);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error en comunicación con la API de iLovePDF: " + e.getMessage(), e);
        }

        return soportesProcesados;
    }

    private void extraerYGuardarZip(byte[] zipData, HojaVida hojaVida, List<SoporteResponseDTO> resultados) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
            ZipEntry zipEntry = zis.getNextEntry();
            int pageCounter = 1;
            while (zipEntry != null) {
                if (!zipEntry.isDirectory() && zipEntry.getName().toLowerCase().endsWith(".pdf")) {
                    String fileName = UUID.randomUUID().toString() + ".pdf";
                    Path destPath = Paths.get(RUTA_BASE_SOPORTES + "sin_clasificar/" + fileName);
                    Files.createDirectories(destPath.getParent());

                    try (FileOutputStream fos = new FileOutputStream(destPath.toFile())) {
                        byte[] buffer = new byte[2048];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }

                    Soporte soporte = crearSoporte(destPath, hojaVida, "Documento Extraído " + pageCounter);
                    resultados.add(mapearAResponseDTO(soporte));
                    pageCounter++;
                }
                zipEntry = zis.getNextEntry();
            }
        }
    }

    private void guardarSoporteIndividual(byte[] pdfData, HojaVida hojaVida, List<SoporteResponseDTO> resultados) throws IOException {
        String fileName = UUID.randomUUID().toString() + ".pdf";
        Path destPath = Paths.get(RUTA_BASE_SOPORTES + "sin_clasificar/" + fileName);
        Files.createDirectories(destPath.getParent());
        Files.write(destPath, pdfData);

        Soporte soporte = crearSoporte(destPath, hojaVida, "Documento Resultante");
        resultados.add(mapearAResponseDTO(soporte));
    }

    private Soporte crearSoporte(Path ruta, HojaVida hojaVida, String tituloTemp) throws IOException {
        Soporte soporte = Soporte.builder()
                .tipoDocumento("Otros Soportes")
                .nombreArchivo(tituloTemp + ".pdf")
                .rutaArchivo(ruta.toString().replace("\\", "/"))
                .tamano(Files.size(ruta))
                .fechaCarga(LocalDateTime.now())
                .estado("Pendiente")
                .hojaVida(hojaVida)
                .build();
        return soporteRepository.save(soporte);
    }

    private SoporteResponseDTO mapearAResponseDTO(Soporte soporte) {
        return new SoporteResponseDTO(
                soporte.getId(), soporte.getTipoDocumento(), soporte.getNombreArchivo(),
                soporte.getRutaArchivo(), soporte.getTamano(), soporte.getFechaCarga(),
                soporte.getEstado(), soporte.getHojaVida().getId()
        );
    }

    public List<SoporteResponseDTO> dividirYGuardarPdf(Long hojaVidaId, MultipartFile archivoLote) {
        return List.of();
    }
}