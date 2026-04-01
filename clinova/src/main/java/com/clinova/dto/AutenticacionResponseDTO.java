package com.clinova.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AutenticacionResponseDTO {
    private String token;
    private String mensaje;
    private String username;
    private String cargo;
    private List<String> permisos;
}