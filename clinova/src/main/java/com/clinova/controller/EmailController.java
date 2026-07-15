package com.clinova.controller;

import com.clinova.entity.Documento;
import com.clinova.repository.DocumentoRepository;
import com.clinova.service.EmailService;
import com.clinova.service.FileLocatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;
    private final FileLocatorService fileLocatorService;
    private final DocumentoRepository documentoRepository;

    @Value("${uploads.root-path:uploads}")
    private String uploadsRootPath;

    private Path root;

    @PostConstruct
    public void init() {
        root = Paths.get(uploadsRootPath).toAbsolutePath().normalize();
    }

    // Usa exactamente la misma lógica que DocumentoController.buscarArchivoFisico()
    private Path buscarArchivo(String nombreArchivo) {
        // 1. FileLocatorService (índice en memoria)
        Path fromService = fileLocatorService.buscarArchivo(nombreArchivo);
        if (fromService != null && Files.exists(fromService)) return fromService;

        // 2. Subcarpeta documentos
        Path path = root.resolve("documentos").resolve(nombreArchivo).normalize();
        if (Files.exists(path)) return path;

        // 3. Raíz de uploads
        path = root.resolve(nombreArchivo).normalize();
        if (Files.exists(path)) return path;

        // 4. Otras subcarpetas frecuentes
        for (String dir : new String[]{"soportes/otros_soportes", "soportes/sin_clasificar", "certificados", "fotos", "soportes"}) {
            path = root.resolve(dir).resolve(nombreArchivo).normalize();
            if (Files.exists(path)) return path;
        }

        return null;
    }

    @PostMapping("/enviar-documento-servidor")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> enviarDocumentoServidor(
            @RequestParam("to") List<String> to,
            @RequestParam("subject") String subject,
            @RequestParam("text") String text,
            @RequestParam("documentoId") Long documentoId) {

        return documentoRepository.findById(documentoId).map(doc -> {
            // Misma prioridad que DocumentoController: rutaArchivoLocal primero, luego ubicacion
            String rutaArchivo = doc.getRutaArchivoLocal();
            if (rutaArchivo == null || rutaArchivo.isBlank()) {
                rutaArchivo = doc.getUbicacion();
            }

            if (rutaArchivo == null || rutaArchivo.isBlank()
                    || rutaArchivo.equals("KAWAK") || rutaArchivo.equals("SIN_ARCHIVO")) {
                return ResponseEntity.badRequest().body("Este documento no tiene archivo físico asociado.");
            }

            Path path = buscarArchivo(rutaArchivo);
            if (path == null) {
                return ResponseEntity.badRequest().body("El archivo no se encontró en el servidor: " + rutaArchivo);
            }

            emailService.sendEmailWithFile(to, subject, text, path.toFile());
            return ResponseEntity.ok("Correo enviado exitosamente.");

        }).orElse(ResponseEntity.badRequest().body("Documento no encontrado con id: " + documentoId));
    }

    @PostMapping("/enviar-archivo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> enviarArchivo(
            @RequestParam("to") List<String> to,
            @RequestParam("subject") String subject,
            @RequestParam("text") String text,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        emailService.sendEmailWithAttachment(to, subject, text, file);
        return ResponseEntity.ok("Correo enviado exitosamente.");
    }
}
