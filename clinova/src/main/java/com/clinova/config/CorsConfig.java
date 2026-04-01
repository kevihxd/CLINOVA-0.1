package com.clinova.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    // Configuración delegada exclusivamente a SecurityConfig para evitar conflictos 403 en peticiones OPTIONS preflight.
}