package com.clinova.config;

import com.clinova.entity.Permiso;
import com.clinova.repository.PermisoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PermisoSeeder implements CommandLineRunner {

    private final PermisoRepository permisoRepository;

    @Override
    public void run(String... args) {
        String[] modulos = {
            // Configuración
            "USUARIOS", "CARGOS", "GRUPOS_DISTRIBUCION", "SEDES", "MAPA_PROCESOS_CONFIG",
            "TIPO_CONTRATO", "VACUNAS", "OPCIONES", "PLANTILLAS_CORREO", "DIAGNOSTICO_SOPORTES",
            // Talento Humano
            "HOJAS_VIDA", "INCAPACIDADES", "CURSOS", "PERFILES_CARGOS",
            "SEMAFORIZACION", "REPORTE_ESTADOS", "ORGANIGRAMA", "TIPO_DOCUMENTO_TH",
            // Hojas de Vida (Submódulos)
            "HOJAS_VIDA_NOMINA", "HOJAS_VIDA_PROVEEDORES",
            // Gestión Documental / Procesos
            "MAPA_PROCESOS", "DOCUMENTOS", "CREAR_DOCUMENTO", "CREAR_FORMATO",
            "DOCUMENTOS_OBSOLETOS", "DOCUMENTOS_EXTERNOS", "TIPOS_DOCUMENTOS",
            // Calidad
            "SOLICITAR_DOCUMENTO", "REVISION_DOCUMENTO", "REPORTES_CALIDAD",
            "PAPELERA_RECICLAJE", "DEFINICIONES",
            // Contexto de la Organización
            "ANALISIS_CONTEXTO", "PARTES_INTERESADAS", "REQUISITOS_LEGALES",
            // Contratación
            "CONTRATACION",
            // Actas e Informes
            "ACTAS", "PLANTILLAS_ACTAS", "INFORMES",
            // Apoyo Terapéutico (Módulo dedicado Kubapp / Adjuntos)
            "APOYO_TERAPEUTICO"
        };

        String[] acciones = {"CREAR", "MODIFICAR", "ELIMINAR", "VER"};

        List<Permiso> existentes = permisoRepository.findAll();
        Map<String, Permiso> porNombre = new HashMap<>();
        for (Permiso p : existentes) {
            if (p.getNombre() != null) {
                porNombre.put(p.getNombre().toUpperCase(), p);
            }
        }

        int creados = 0;
        for (String modulo : modulos) {
            for (String accion : acciones) {
                String nombre = modulo + "_" + accion;
                Permiso existing = porNombre.get(nombre.toUpperCase());

                if (existing != null) {
                    if (existing.getModulo() == null || existing.getAccion() == null
                            || !modulo.equals(existing.getModulo()) || !accion.equals(existing.getAccion())) {
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
                    Permiso saved = permisoRepository.save(nuevo);
                    porNombre.put(nombre.toUpperCase(), saved);
                    creados++;
                }
            }
        }
        log.info("Permisos validados exitosamente (total módulos: {}, nuevos permisos creados: {}).", modulos.length, creados);
    }
}
