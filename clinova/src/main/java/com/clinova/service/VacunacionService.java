package com.clinova.service;

import com.clinova.entity.Persona;
import com.clinova.entity.RegistroVacuna;
import com.clinova.entity.Vacuna;
import com.clinova.repository.PersonaRepository;
import com.clinova.repository.RegistroVacunaRepository;
import com.clinova.repository.VacunaRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class VacunacionService {

    private final PersonaRepository personaRepository;
    private final VacunaRepository vacunaRepository;
    private final RegistroVacunaRepository registroVacunaRepository;

    @Transactional
    public void procesarExcelVacunas(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheet("RegistroDosis");
            if (sheet == null) sheet = workbook.getSheetAt(2); // O el índice 2 si está en esa posición

            boolean isFirstRow = true;

            for (Row row : sheet) {
                if (isFirstRow) {
                    isFirstRow = false;
                    continue; // Saltar encabezados
                }

                String documento = getCellValueAsString(row.getCell(0));
                String nombreVacuna = getCellValueAsString(row.getCell(1));
                String detalleDosis = getCellValueAsString(row.getCell(2));
                LocalDate fechaAplicacion = getCellValueAsDate(row.getCell(3));

                if (documento.isBlank() || nombreVacuna.isBlank() || fechaAplicacion == null) {
                    continue;
                }

                Persona persona = personaRepository.findByNumeroDocumento(documento).orElse(null);
                if (persona == null) continue;

                Vacuna vacuna = vacunaRepository.findByNombre(nombreVacuna)
                        .orElseGet(() -> vacunaRepository.save(Vacuna.builder().nombre(nombreVacuna).build()));

                RegistroVacuna registro = RegistroVacuna.builder()
                        .persona(persona)
                        .vacuna(vacuna)
                        .detalleDosis(detalleDosis)
                        .fechaAplicacion(fechaAplicacion)
                        .build();

                registroVacunaRepository.save(registro);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error procesando el archivo Excel: " + e.getMessage());
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long) cell.getNumericCellValue());
        return cell.getStringCellValue().trim();
    }

    private LocalDate getCellValueAsDate(Cell cell) {
        if (cell == null) return null;
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return LocalDate.parse(cell.getStringCellValue().trim()); //  formato YYYY-MM-DD
    }
}