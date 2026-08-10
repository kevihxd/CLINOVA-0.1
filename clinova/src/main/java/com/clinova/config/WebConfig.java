package com.clinova.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String envOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        String[] origins = (envOrigins != null && !envOrigins.trim().isEmpty())
                ? envOrigins.split(",")
                : new String[]{"*"};

        registry.addMapping("/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        Path uploadDir = Paths.get("uploads");
        String uriLocation = uploadDir.toFile().toURI().toString();
        if (!uriLocation.endsWith("/")) {
            uriLocation += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uriLocation);
        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations(uriLocation);
    }
}