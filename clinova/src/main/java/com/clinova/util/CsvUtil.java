package com.clinova.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class CsvUtil {

    private CsvUtil() {
        throw new IllegalStateException("Clase Utilitaria - No instanciar");
    }

    public static List<String[]> parsearCsvRobusto(MultipartFile file) throws Exception {
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
                        currentRow.add(field.toString().trim());
                        field.setLength(0);
                    } else if (ch == '\n' || ch == '\r') {
                        if (ch == '\r') {
                            br.mark(1);
                            if (br.read() != '\n') {
                                br.reset();
                            }
                        }
                        currentRow.add(field.toString().trim());
                        lines.add(currentRow.toArray(new String[0]));
                        currentRow.clear();
                        field.setLength(0);
                    } else {
                        field.append(ch);
                    }
                }
            }
            if (field.length() > 0 || !currentRow.isEmpty()) {
                currentRow.add(field.toString().trim());
                lines.add(currentRow.toArray(new String[0]));
            }
        }
        return lines;
    }

    public static String obtenerDato(String[] datos, int index) {
        if (index >= datos.length) return "";
        String dato = datos[index];
        if (dato == null || dato.isBlank() || dato.equals(".")) return "";
        return dato.replaceAll("^\"|\"$", "").trim();
    }

    public static Long parsearLong(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static LocalDate parsearFecha(String fechaStr) {
        if (fechaStr == null || fechaStr.isBlank()) return null;
        try {
            return LocalDate.parse(fechaStr.split(" ")[0], DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}