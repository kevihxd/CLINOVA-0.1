package com.clinova.service;

import com.clinova.dto.SoporteResponseDTO;
import com.clinova.entity.HojaVida;
import com.clinova.entity.Soporte;
import com.clinova.repository.HojaVidaRepository;
import com.clinova.repository.SoporteRepository;
import com.clinova.service.ai.ClasificadorDocumentoIA;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SoporteInteligenteService {

    private final SoporteRepository soporteRepository;
    private final HojaVidaRepository hojaVidaRepository;
    private final ClasificadorDocumentoIA clasificadorIA;

    private final String RUTA_BASE_SOPORTES = "uploads/soportes/";

    @Transactional
    public List<SoporteResponseDTO> procesarSoporteLote(Long hojaVidaId, MultipartFile archivoLote) {
        if (archivoLote.isEmpty() || !archivoLote.getContentType().equals("application/pdf")) {
            throw new IllegalArgumentException("El archivo debe ser un PDF válido");
        }

        HojaVida hojaVida = hojaVidaRepository.findById(hojaVidaId)
                .orElseThrow(() -> new RuntimeException("Hoja de vida no encontrada"));

        List<SoporteResponseDTO> soportesProcesados = new ArrayList<>();

        try (PDDocument documentoPrincipal = Loader.loadPDF(archivoLote.getBytes())) {
            Splitter splitter = new Splitter();
            List<PDDocument> paginas = splitter.split(documentoPrincipal);

            for (PDDocument pagina : paginas) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                pagina.save(baos);
                byte[] bytesPagina = baos.toByteArray();

                String tipoDocumentoClasificado = clasificadorIA.clasificarDocumento(bytesPagina);

                if (tipoDocumentoClasificado != null && !tipoDocumentoClasificado.equalsIgnoreCase("Desconocido")) {
                    String nombreArchivoUnico = UUID.randomUUID().toString() + ".pdf";
                    String subCarpeta = tipoDocumentoClasificado.replaceAll("[^a-zA-Z0-9]+", "_").toLowerCase();
                    Path rutaDestino = Paths.get(RUTA_BASE_SOPORTES + subCarpeta + "/" + nombreArchivoUnico);

                    Files.createDirectories(rutaDestino.getParent());
                    Files.write(rutaDestino, bytesPagina);

                    Soporte soporte = Soporte.builder()
                            .tipoDocumento(tipoDocumentoClasificado)
                            .nombreArchivo(tipoDocumentoClasificado + ".pdf")
                            .rutaArchivo(rutaDestino.toString())
                            .tamano((long) bytesPagina.length)
                            .fechaCarga(LocalDateTime.now())
                            .estado("Pendiente")
                            .hojaVida(hojaVida)
                            .build();

                    Soporte soporteGuardado = soporteRepository.save(soporte);
                    soportesProcesados.add(mapearAResponseDTO(soporteGuardado));
                }
                pagina.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al procesar el documento por lotes con IA", e);
        }

        return soportesProcesados;
    }

    private SoporteResponseDTO mapearAResponseDTO(Soporte soporte) {
        return new SoporteResponseDTO(
                soporte.getId(),
                soporte.getTipoDocumento(),
                soporte.getNombreArchivo(),
                soporte.getRutaArchivo(),
                soporte.getTamano(),
                soporte.getFechaCarga(),
                soporte.getEstado(),
                soporte.getHojaVida().getId()
        );
    }
}