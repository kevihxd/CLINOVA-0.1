package com.clinova.config;

import com.clinova.entity.Permiso;
import com.clinova.repository.PermisoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PermisoSeeder implements CommandLineRunner {

    private final PermisoRepository permisoRepository;

    @Override
    public void run(String... args) throws Exception {
        String[] modulos = {
            "SEDES", "CARGOS", "USUARIOS", "VACUNAS", "OPCIONES", "TIPO_CONTRATO",
            "HOJAS_VIDA", "INCAPACIDADES", "CURSOS", "ACTAS", "PLANTILLAS_ACTAS",
            "INFORMES", "MAPA_PROCESOS", "PERFILES_CARGOS", "DOCUMENTOS"
        };
        
        String[] acciones = {"CREAR", "MODIFICAR", "ELIMINAR", "VER"};

        for (String modulo : modulos) {
            for (String accion : acciones) {
                String nombre = modulo + "_" + accion;

                Optional<Permiso> existingOpt = permisoRepository.findAll().stream()
                        .filter(p -> p.getNombre().equalsIgnoreCase(nombre))
                        .findFirst();
                
                if (existingOpt.isPresent()) {
                    Permiso existing = existingOpt.get();
                    if (existing.getModulo() == null || existing.getAccion() == null) {
                        existing.setModulo(modulo);
                        existing.setAccion(accion);
                        permisoRepository.save(existing);
                    }
                } else {
                    String descripcion = "Permite " + accion.toLowerCase() + " en el módulo de " + modulo.replace("_", " ").toLowerCase();
                    Permiso nuevo = Permiso.builder()
                            .nombre(nombre)
                            .descripcion(descripcion)
                            .modulo(modulo)
                            .accion(accion)
                            .build();
                    permisoRepository.save(nuevo);
                }
            }
        }
        log.info("Permisos validados exitosamente.");
    }
}
