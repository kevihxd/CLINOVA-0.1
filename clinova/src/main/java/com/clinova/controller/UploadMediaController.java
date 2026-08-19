package com.clinova.controller;

import com.clinova.service.FileLocatorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UploadMediaController {

    private final FileLocatorService fileLocator;
    private final Path rootUploads = Paths.get("uploads").toAbsolutePath().normalize();

    @GetMapping({"/uploads/**", "/api/uploads/**", "/api/v1/uploads/**"})
    public ResponseEntity<Resource> serveMedia(HttpServletRequest request) {
        try {
            String uri = request.getRequestURI();
            String relative = uri.replaceAll("^(/api/v1)?/uploads/?", "");

            // 1. Probar ruta directa en rootUploads (ej. uploads/fotos/..., uploads/files/...)
            Path direct = rootUploads.resolve(relative).normalize();
            if (Files.exists(direct) && Files.isReadable(direct) && !Files.isDirectory(direct)) {
                return streamFile(direct);
            }

            // 2. Probar ruta directa en /app/uploads (Docker VPS)
            Path dockerDirect = Paths.get("/app/uploads").resolve(relative).normalize();
            if (Files.exists(dockerDirect) && Files.isReadable(dockerDirect) && !Files.isDirectory(dockerDirect)) {
                return streamFile(dockerDirect);
            }

            // 3. Búsqueda inteligente en el índice de memoria (resuelve cualquier subcarpeta)
            Path indexed = fileLocator.buscarArchivo(relative);
            if (indexed != null && Files.exists(indexed) && Files.isReadable(indexed) && !Files.isDirectory(indexed)) {
                return streamFile(indexed);
            }

            // 4. Fallback: Búsqueda solo por nombre de archivo
            String filename = Paths.get(relative).getFileName().toString();
            Path filenameMatch = fileLocator.buscarArchivo(filename);
            if (filenameMatch != null && Files.exists(filenameMatch) && Files.isReadable(filenameMatch) && !Files.isDirectory(filenameMatch)) {
                return streamFile(filenameMatch);
            }

            // 5. Fallback en subcarpetas conocidas
            String[] subcarpetas = {"fotos", "files/thu_talento/hdv_fotos", "documentos", "files/Formatos/1", "files/Documentos/1", "soportes"};
            for (String sub : subcarpetas) {
                Path subPath = rootUploads.resolve(sub).resolve(filename).normalize();
                if (Files.exists(subPath) && Files.isReadable(subPath) && !Files.isDirectory(subPath)) {
                    return streamFile(subPath);
                }
                Path dockerSub = Paths.get("/app/uploads").resolve(sub).resolve(filename).normalize();
                if (Files.exists(dockerSub) && Files.isReadable(dockerSub) && !Files.isDirectory(dockerSub)) {
                    return streamFile(dockerSub);
                }
            }

            log.warn("Archivo multimedia no encontrado: {}", uri);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al servir multimedia [URI={}]: {}", request.getRequestURI(), e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    private ResponseEntity<Resource> streamFile(Path path) throws Exception {
        Resource resource = new UrlResource(path.toUri());
        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName().toString() + "\"")
                .body(resource);
    }
}
