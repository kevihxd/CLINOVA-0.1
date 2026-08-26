package com.clinova.config;

import com.clinova.entity.Sede;
import com.clinova.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SedeSeeder implements CommandLineRunner {

    private final SedeRepository sedeRepository;

    @Override
    public void run(String... args) throws Exception {
        List<String> sedesIniciales = List.of(
            "PAMI",
            "PRINCIPAL",
            "CAOBOS 2",
            "IPS CLINICAL HOUSE SEDE CAOBOS II",
            "REMOTO",
            "PRESENCIAL",
            "SIN SEDE",
            "CAOBOS I",
            "CAOBOS LL",
            "CAOBOS",
            "NA",
            "IPS CLINICAL HOUSE",
            "FUNDACION CORAZON SOLIDARIO",
            "IPS CLINICAL HOUSE SEDE PRINCIPAL"
        );

        for (String nombreSede : sedesIniciales) {
            String nombreUpper = nombreSede.trim().toUpperCase();
            if (sedeRepository.findByNombre(nombreUpper).isEmpty()) {
                Sede nueva = Sede.builder()
                        .nombre(nombreUpper)
                        .activo(true)
                        .build();
                sedeRepository.save(nueva);
                log.info("Sede creada por seeder: {}", nombreUpper);
            }
        }
    }
}
