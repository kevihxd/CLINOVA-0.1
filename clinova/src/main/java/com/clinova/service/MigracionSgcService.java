package com.clinova.service;

import com.clinova.entity.Documento;
import com.clinova.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MigracionSgcService {

    private final DocumentoRepository documentoRepository;

    @Transactional
    public String migrarDocumentosSgc(MultipartFile fileUsuariosKawak, MultipartFile fileDocumentosKawak) {
        if (fileUsuariosKawak.isEmpty() || fileDocumentosKawak.isEmpty()) {
            throw new RuntimeException("Ambos archivos CSV son obligatorios");
        }

        try {
            // Diccionario: ID Kawak (ej. 15) -> Cédula (ej. "1090509969")
            Map<Long, String> diccionarioCedulas = construirDiccionarioCedulas(fileUsuariosKawak);
            List<String[]> filasDocumentos = parsearCsvRobusto(fileDocumentosKawak);

            int guardados = 0;
            boolean primeraLinea = true;

            for (String[] datos : filasDocumentos) {
                if (primeraLinea) {
                    primeraLinea = false;
                    continue;
                }

                if (datos.length < 3) continue;

                Long kawakId = parsearLong(obtenerDato(datos, 0));
                if (kawakId == null) continue;

                // Buscar si ya existe por kawak_id para no duplicar en re-ejecuciones
                Documento documento = documentoRepository.findByKawakId(kawakId)
                        .orElse(Documento.builder().kawakId(kawakId).build());

                documento.setNombre(obtenerDato(datos, 1));
                documento.setCodigo(obtenerDato(datos, 7));
                documento.setUbicacion(obtenerDato(datos, 4));
                documento.setEstado(obtenerDato(datos, 6));

                // Mapeo del Elaborador (doc_id_creador) hacia la CÉDULA
                Long idCreadorKawak = parsearLong(obtenerDato(datos, 8));
                if (idCreadorKawak != null && diccionarioCedulas.containsKey(idCreadorKawak)) {
                    documento.setElabora(diccionarioCedulas.get(idCreadorKawak)); // Guarda la cédula
                }

                documentoRepository.save(documento);
                guardados++;
            }

            return "Migración completada. Documentos procesados y enlazados por cédula: " + guardados;

        } catch (Exception e) {
            throw new RuntimeException("Error en la migración de datos SGC: " + e.getMessage());
        }
    }

    private Map<Long, String> construirDiccionarioCedulas(MultipartFile fileUsuarios) throws Exception {
        Map<Long, String> diccionario = new HashMap<>();
        List<String[]> filas = parsearCsvRobusto(fileUsuarios);
        boolean primeraLinea = true;

        for (String[] datos : filas) {
            if (primeraLinea) {
                primeraLinea = false;
                continue;
            }
            if (datos.length < 2) continue;

            // Índice 0: Id Kawak | Índice 1: Número de Identificación (Cédula)
            Long kawakId = parsearLong(obtenerDato(datos, 0));
            String cedula = obtenerDato(datos, 1).trim();

            if (kawakId != null && !cedula.isEmpty()) {
                diccionario.put(kawakId, cedula);
            }
        }
        return diccionario;
    }

    private String obtenerDato(String[] datos, int index) {
        return index < datos.length ? datos[index].replaceAll("^\"|\"$", "").trim() : "";
    }

    private Long parsearLong(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<String[]> parsearCsvRobusto(MultipartFile file) throws Exception {
        List<String[]> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            br.mark(4096);
            String primeraLinea = br.readLine();
            br.reset();

            char separador = ',';
            if (primeraLinea != null) {
                int countComas = primeraLinea.length() - primeraLinea.replace(",", "").length();
                int countPuntoComa = primeraLinea.length() - primeraLinea.replace(";", "").length();
                if (countPuntoComa > countComas) {
                    separador = ';';
                }
            }

            boolean inQuotes = false;
            StringBuilder field = new StringBuilder();
            List<String> currentRow = new ArrayList<>();

            int c;
            while ((c = br.read()) != -1) {
                char ch = (char) c;
                if (inQuotes) {
                    if (ch == '"') {
                        br.mark(1);
                        if (br.read() == '"') {
                            field.append('"');
                        } else {
                            inQuotes = false;
                            br.reset();
                        }
                    } else {
                        field.append(ch);
                    }
                } else {
                    if (ch == '"') {
                        inQuotes = true;
                    } else if (ch == separador) {
                        currentRow.add(field.toString());
                        field.setLength(0);
                    } else if (ch == '\n' || ch == '\r') {
                        if (ch == '\r') {
                            br.mark(1);
                            if (br.read() != '\n') {
                                br.reset();
                            }
                        }
                        currentRow.add(field.toString());
                        lines.add(currentRow.toArray(new String[0]));
                        currentRow.clear();
                        field.setLength(0);
                    } else {
                        field.append(ch);
                    }
                }
            }
            if (field.length() > 0 || !currentRow.isEmpty()) {
                currentRow.add(field.toString());
                lines.add(currentRow.toArray(new String[0]));
            }
        }
        return lines;
    }
}