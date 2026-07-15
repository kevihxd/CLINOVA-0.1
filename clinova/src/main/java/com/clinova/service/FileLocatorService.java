package com.clinova.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
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
        rootUploads = Paths.get(uploadsRootPath).toAbsolutePath().normalize();
        log.info("Iniciando escaneo recursivo de archivos en: {}", rootUploads);

        if (!Files.exists(rootUploads)) {
            log.warn("El directorio base de uploads no existe: {}", rootUploads);
            return;
        }

        try {
            Files.walkFileTree(rootUploads, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (!Files.isDirectory(file)) {
                        // Indexar por nombre de archivo exacto (minúsculas)
                        String fileName = file.getFileName().toString().toLowerCase();
                        fileIndex.putIfAbsent(fileName, file);

                        // Indexar también por ruta relativa al root de uploads
                        Path relativeToUploads = rootUploads.relativize(file);
                        String relativePath = relativeToUploads.toString().replace("\\", "/").toLowerCase();
                        fileIndex.putIfAbsent(relativePath, file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.warn("No se pudo leer el archivo: {}", file);
                    return FileVisitResult.CONTINUE;
                }
            });
            log.info("Escaneo completado. Archivos indexados en memoria: {}", fileIndex.size());
        } catch (IOException e) {
            log.error("Error al indexar archivos físicos: {}", e.getMessage(), e);
        }
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

        // 2. Si viene con ruta parcial (ej: "Documentos/15/archivo.pdf"), extraer solo el nombre
        int lastSlash = key.lastIndexOf("/");
        if (lastSlash >= 0) {
            String justName = key.substring(lastSlash + 1);
            if (fileIndex.containsKey(justName)) {
                return fileIndex.get(justName);
            }
        }

        // 3. Búsqueda parcial: recorrer índice buscando cualquier entrada que TERMINE con la clave
        for (Map.Entry<String, Path> entry : fileIndex.entrySet()) {
            if (entry.getKey().endsWith(key) || entry.getKey().endsWith("/" + key)) {
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
