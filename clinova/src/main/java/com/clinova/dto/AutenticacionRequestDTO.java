package com.clinova.dto;

import com.clinova.entity.Persona;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AutenticacionRequestDTO {
    private String username;
    private String password;
    private String rol;
    private Persona persona;
}