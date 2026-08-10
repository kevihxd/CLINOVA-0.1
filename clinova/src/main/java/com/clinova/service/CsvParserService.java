package com.clinova.service;

import com.clinova.dto.CursoCsvRowDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsabilidad única: parsear el CSV y devolver una lista limpia de DTOs.
 * No toca la base de datos. No calcula estados.
 */
@Slf4j
@Service
public class CsvParserService {

    private static final DateTimeFormatter[] SUPPORTED_FORMATS = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
    };

    /**
     * Parsea el archivo CSV y devuelve una lista de DTOs validados.
     *
     * @param file MultipartFile recibido del controller
     * @return Lista de filas parseadas correctamente
     * @throws IllegalArgumentException si el archivo es inválido o está vacío
     */
    public List<CursoCsvRowDTO> parse(MultipartFile file) {
        validateFile(file);

        List<CursoCsvRowDTO> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("El archivo CSV está vacío o no tiene encabezados.");
            }

            // Detectar separador (coma o punto y coma)
            char separator = headerLine.contains(";") ? ';' : ',';
            String[] headers = splitCsvLine(headerLine, separator);
            
            int colDocumento = findColumn(headers, "documento_empleado", "documento", "cedula", "identificacion");
            int colCurso = findColumn(headers, "nombre_curso", "curso", "nombre");
            int colFechaRealizacion = findColumn(headers, "fecha_realizacion", "fecha_emision", "realizado");
            int colFechaVencimiento = findColumn(headers, "fecha_vencimiento_fija", "fecha_vencimiento", "vencimiento");

            if (colDocumento == -1 || colCurso == -1 || colFechaRealizacion == -1) {
                throw new IllegalArgumentException(
                        "Columnas obligatorias no encontradas. Se requiere: documento_empleado, nombre_curso, fecha_realizacion. " +
                        "Encabezados detectados: " + String.join(", ", headers));
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;

                try {
                    String[] fields = splitCsvLine(line, separator);

                    String documento = cleanField(safeGet(fields, colDocumento));
                    String curso = cleanField(safeGet(fields, colCurso));
                    String fechaRealizacionStr = cleanField(safeGet(fields, colFechaRealizacion));
                    String fechaVencimientoStr = colFechaVencimiento != -1
                            ? cleanField(safeGet(fields, colFechaVencimiento)) : null;

                    if (documento.isEmpty() || curso.isEmpty()) {
                        errors.add("Línea " + lineNumber + ": documento o curso vacío, se omite.");
                        continue;
                    }

                    // Limpiar documento (quitar decimales si vienen como 123456.0)
                    if (documento.contains(".")) {
                        documento = documento.substring(0, documento.indexOf('.'));
                    }

                    LocalDate fechaRealizacion = parseDate(fechaRealizacionStr);
                    LocalDate fechaVencimientoFija = parseDate(fechaVencimientoStr);

                    results.add(CursoCsvRowDTO.builder()
                            .documentoEmpleado(documento)
                            .nombreCurso(curso.toUpperCase().trim())
                            .fechaRealizacion(fechaRealizacion)
                            .fechaVencimientoFija(fechaVencimientoFija)
                            .lineaArchivo(lineNumber)
                            .build());

                } catch (Exception e) {
                    errors.add("Línea " + lineNumber + ": " + e.getMessage());
                }
            }

            if (!errors.isEmpty()) {
                log.warn("CSV parseado con {} advertencias: {}", errors.size(), errors);
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al leer el archivo CSV: " + e.getMessage(), e);
        }

        if (results.isEmpty()) {
            throw new IllegalArgumentException("El CSV no contiene filas válidas para procesar.");
        }

        log.info("CSV parseado exitosamente: {} filas válidas", results.size());
        return results;
    }

    // --- Métodos privados ---

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo es requerido y no puede estar vacío.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".csv") && !filename.endsWith(".CSV"))) {
            throw new IllegalArgumentException("Solo se aceptan archivos con extensión .csv");
        }
    }

    private int findColumn(String[] headers, String... possibleNames) {
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].toLowerCase().trim().replaceAll("[\"']", "");
            for (String name : possibleNames) {
                if (h.equals(name.toLowerCase()) || h.contains(name.toLowerCase())) {
                    return i;
                }
            }
        }
        return -1;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank() || dateStr.equalsIgnoreCase("n/a")
                || dateStr.equalsIgnoreCase("nan") || dateStr.equals("-")) {
            return null;
        }
        for (DateTimeFormatter fmt : SUPPORTED_FORMATS) {
            try {
                return LocalDate.parse(dateStr.trim(), fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        log.warn("No se pudo parsear la fecha: '{}'", dateStr);
        return null;
    }

    private String cleanField(String field) {
        if (field == null) return "";
        return field.trim().replaceAll("^\"|\"$", "").trim();
    }

    private String safeGet(String[] arr, int index) {
        return (index >= 0 && index < arr.length) ? arr[index] : "";
    }

    private String[] splitCsvLine(String line, char separator) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == separator && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
