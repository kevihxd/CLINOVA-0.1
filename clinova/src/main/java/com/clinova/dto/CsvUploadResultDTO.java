package com.clinova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsvUploadResultDTO {
    private int totalFilas;
    private int procesadas;
    private int errores;
    private int actualizadas;
    private int nuevas;
    private java.util.List<String> advertencias;
}
