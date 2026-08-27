package com.clinova.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class FileLocatorService {

    @Value("${uploads.root-path:uploads}")
    private String uploadsRootPath;

    private Path rootUploads;
    private final Map<String, Path> fileIndex = new ConcurrentHashMap<>();

    @PostConstruct
    public void initIndex() {
        List<Path> candidateRoots = List.of(
            Paths.get(uploadsRootPath).toAbsolutePath().normalize(),
            Paths.get("/app/uploads").toAbsolutePath().normalize(),
            Paths.get("/opt/clinova/uploads").toAbsolutePath().normalize(),
            Paths.get("/opt/clinova").toAbsolutePath().normalize(),
            Paths.get("uploads").toAbsolutePath().normalize(),
            Paths.get("..", "uploads").toAbsolutePath().normalize(),
            Paths.get(System.getProperty("user.home"), "Desktop", "Clinova", "uploads")
        );

        for (Path root : candidateRoots) {
            if (Files.exists(root) && Files.isDirectory(root)) {
                log.info("Indexando archivos desde directorio raíz: {}", root);
                if (rootUploads == null) rootUploads = root;
                try {
                    Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            if (!Files.isDirectory(file)) {
                                String fileName = file.getFileName().toString().toLowerCase();
                                fileIndex.putIfAbsent(fileName, file);

                                try {
                                    Path relativeToUploads = root.relativize(file);
                                    String relativePath = relativeToUploads.toString().replace("\\", "/").toLowerCase();
                                    fileIndex.putIfAbsent(relativePath, file);
                                } catch (Exception ignored) {}
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    log.error("Error indexando directorio {}: {}", root, e.getMessage());
                }
            }
        }
        log.info("Escaneo masivo de documentos completado. Total archivos indexados en memoria: {}", fileIndex.size());
    }

    /**
     * Busca un archivo por nombre exacto o ruta relativa.
     * Busca en todo el índice sin importar en qué subcarpeta esté.
     */
    public Path buscarArchivo(String nombreUbicacion) {
        if (nombreUbicacion == null || nombreUbicacion.isBlank()) return null;

        String key = nombreUbicacion.toLowerCase().replace("\\", "/").trim();

        // 1. Búsqueda exacta (nombre o ruta relativa)
        if (fileIndex.containsKey(key)) {
            return fileIndex.get(key);
        }

        // 2. Extraer sólo el nombre del archivo
        int lastSlash = key.lastIndexOf("/");
        String justName = (lastSlash >= 0) ? key.substring(lastSlash + 1) : key;
        if (fileIndex.containsKey(justName)) {
            return fileIndex.get(justName);
        }

        // 3. Probar variaciones de extensiones (.pdf <-> .docx <-> .xlsx)
        int dot = justName.lastIndexOf('.');
        String nameWithoutExt = (dot > 0) ? justName.substring(0, dot) : justName;
        for (String ext : List.of(".pdf", ".docx", ".xlsx", ".xls", ".doc")) {
            String candName = (nameWithoutExt + ext).toLowerCase();
            if (fileIndex.containsKey(candName)) {
                return fileIndex.get(candName);
            }
        }

        // 4. Búsqueda por sufijo
        for (Map.Entry<String, Path> entry : fileIndex.entrySet()) {
            String entryKey = entry.getKey();
            if (entryKey.endsWith(key) || entryKey.endsWith("/" + key) || entryKey.endsWith("/" + justName)) {
                return entry.getValue();
            }
        }

        log.debug("Archivo no encontrado en índice: '{}'", nombreUbicacion);
        return null;
    }

    /**
     * Registra un archivo recién subido para que esté disponible sin reiniciar.
     */
    public void registrarNuevoArchivo(Path archivoFisico) {
        if (archivoFisico == null || !Files.exists(archivoFisico)) return;
        String fileName = archivoFisico.getFileName().toString().toLowerCase();
        fileIndex.put(fileName, archivoFisico);
        log.info("Nuevo archivo registrado en índice: {}", archivoFisico);
    }
}
