package com.clinova.service;

import com.clinova.entity.*;
import com.clinova.repository.GrupoDistribucionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentoPermissionEvaluator {

    private final GrupoDistribucionRepository grupoRepository;

    /**
     * Evalúa si un usuario tiene un permiso específico (visualizacion, impresion, descargaOriginal, descargaPdf)
     * basándose en la lista CSV de grupos, cargos o usuarios almacenada en el campo.
     */
    public boolean hasPermission(String permissionField, Usuario usuario) {
        if (usuario == null) {
            // Usuario anónimo / sin sesión: solo si es explícitamente público
            return isPublic(permissionField);
        }

        // 1. Superadmin o Rol ADMIN / Gestor de Calidad siempre tiene acceso total
        if (isAdmin(usuario)) {
            return true;
        }

        // 2. Si el campo está vacío o es nulo -> por defecto no tiene acceso a menos que sea un documento público
        if (permissionField == null || permissionField.trim().isEmpty()) {
            return false;
        }

        String fieldTrimmed = permissionField.trim();

        // 3. Casos globales de Kawak
        if (isPublic(fieldTrimmed)) {
            return true;
        }
        if (fieldTrimmed.equalsIgnoreCase("NO") || fieldTrimmed.equalsIgnoreCase("FALSE")) {
            return false;
        }
        if (fieldTrimmed.equalsIgnoreCase("SI") || fieldTrimmed.equalsIgnoreCase("TRUE")) {
            return true;
        }

        // 4. Obtener descriptores del usuario: nombres, cédula, cargo y grupos a los que pertenece
        Set<String> userDescriptors = getUserDescriptors(usuario);

        // 5. Comparar descriptores contra la lista requerida en permissionField (separada por comas)
        String[] tokens = fieldTrimmed.split(",");
        for (String token : tokens) {
            String normToken = normalize(token);
            if (normToken.isEmpty()) continue;

            if (normToken.equals("PUBLICO") || normToken.equals("CLINICAL HOUSE- TODOS") || normToken.equals("TODOS")) {
                return true;
            }

            for (String desc : userDescriptors) {
                if (normToken.equals(desc) || normToken.contains(desc) || desc.contains(normToken)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean canView(String visualizacion, Usuario usuario) {
        return hasPermission(visualizacion, usuario);
    }

    public boolean canPrint(String impresion, Usuario usuario) {
        return hasPermission(impresion, usuario);
    }

    public boolean canDownloadOriginal(String descargaOriginal, Usuario usuario) {
        return hasPermission(descargaOriginal, usuario);
    }

    public boolean canDownloadPdf(String descargaPdf, Usuario usuario) {
        return hasPermission(descargaPdf, usuario);
    }

    private boolean isPublic(String field) {
        if (field == null) return false;
        String norm = normalize(field);
        return norm.contains("PUBLICO") || norm.contains("CLINICAL HOUSE- TODOS") || norm.equals("TODOS");
    }

    public boolean isAdmin(Usuario usuario) {
        if (usuario == null) return false;
        if (usuario.getRol() == Role.ADMIN) return true;

        if (usuario.getCargo() != null && usuario.getCargo().getNombre() != null) {
            String cNorm = normalize(usuario.getCargo().getNombre());
            if (cNorm.contains("CALIDAD") || cNorm.contains("GEREN") || cNorm.contains("ADMINISTRADOR")) {
                return true;
            }
        }

        if (usuario.getAuthorities() != null) {
            for (var auth : usuario.getAuthorities()) {
                String a = auth.getAuthority();
                if ("ROLE_ADMIN".equalsIgnoreCase(a) || "ADMIN".equalsIgnoreCase(a) || "GESTION_DOCUMENTAL_ADMIN".equalsIgnoreCase(a)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Set<String> getUserDescriptors(Usuario usuario) {
        Set<String> descriptors = new HashSet<>();
        if (usuario == null) return descriptors;

        // Username
        if (usuario.getUsername() != null) {
            descriptors.add(normalize(usuario.getUsername()));
        }

        // Persona (Nombres y Apellidos)
        if (usuario.getPersona() != null) {
            String fullName = usuario.getPersona().getNombreCompleto();
            if (!fullName.isEmpty()) descriptors.add(normalize(fullName));
            if (usuario.getPersona().getNumeroDocumento() != null) {
                descriptors.add(normalize(usuario.getPersona().getNumeroDocumento()));
            }
        }

        // Hoja de Vida
        if (usuario.getHojaVida() != null) {
            String fullName = ((usuario.getHojaVida().getNombres() != null ? usuario.getHojaVida().getNombres() : "") + " " + 
                               (usuario.getHojaVida().getApellidos() != null ? usuario.getHojaVida().getApellidos() : "")).trim();
            if (!fullName.isEmpty()) descriptors.add(normalize(fullName));
            if (usuario.getHojaVida().getCedula() != null) {
                descriptors.add(normalize(usuario.getHojaVida().getCedula()));
            }
        }

        // Cargo
        String cargoNombre = null;
        if (usuario.getCargo() != null && usuario.getCargo().getNombre() != null) {
            cargoNombre = usuario.getCargo().getNombre();
            descriptors.add(normalize(cargoNombre));
        }

        // Grupos de Distribución (Directos e Influidos por Cargo)
        try {
            List<GrupoDistribucion> todosGrupos = grupoRepository.findAll();
            Long hojaVidaId = usuario.getHojaVida() != null ? usuario.getHojaVida().getId() : null;
            Long cargoId = usuario.getCargo() != null ? usuario.getCargo().getId() : null;

            for (GrupoDistribucion g : todosGrupos) {
                if (g.getActivo() != null && !g.getActivo()) continue;
                String gNorm = normalize(g.getNombre());
                if (gNorm.equals("CLINICAL HOUSE- TODOS")) {
                    descriptors.add(gNorm);
                    continue;
                }

                // Vinculación por integrante
                boolean esIntegrante = hojaVidaId != null && g.getIntegrantes() != null &&
                        g.getIntegrantes().stream().anyMatch(h -> Objects.equals(h.getId(), hojaVidaId));

                // Vinculación por cargo
                boolean esCargo = cargoId != null && g.getCargos() != null &&
                        g.getCargos().stream().anyMatch(c -> Objects.equals(c.getId(), cargoId));

                // Coincidencia semántica de cargo
                boolean coincideCargo = cargoNombre != null && coincideCargoConGrupo(cargoNombre, g.getNombre());

                if (esIntegrante || esCargo || coincideCargo) {
                    descriptors.add(gNorm);
                }
            }
        } catch (Exception e) {
            log.warn("Error al resolver grupos para usuario {}: {}", usuario.getUsername(), e.getMessage());
        }

        return descriptors;
    }

    private boolean coincideCargoConGrupo(String nombreCargo, String nombreGrupo) {
        if (nombreCargo == null || nombreGrupo == null) return false;
        String cNorm = normalize(nombreCargo);
        String gNorm = normalize(nombreGrupo);

        if (gNorm.equals("CLINICAL HOUSE- TODOS")) return true;
        if (gNorm.equals("TERAPIA OCUPACIONAL") && cNorm.contains("OCUPACIONAL")) return true;
        if (gNorm.equals("FISIOTERAPIA") && (cNorm.contains("FISIO") || cNorm.contains("REHABILITA"))) return true;
        if (gNorm.equals("FONOAUDIOLOGIA") && cNorm.contains("FONOA")) return true;
        if (gNorm.equals("MEDICINA GENERAL") && (cNorm.contains("MEDIC") || cNorm.contains("DOCTOR"))) return true;
        if (gNorm.equals("ENFERMERIA") && (cNorm.contains("ENFERM") || cNorm.contains("JEFE") || cNorm.contains("PAD"))) return true;
        if (gNorm.equals("ODONTOLOGIA") && cNorm.contains("ODONTO")) return true;
        if (gNorm.equals("SERVICIO FARMACEUTICO") && (cNorm.contains("FARMAC") || cNorm.contains("REGENTE"))) return true;
        if (gNorm.contains("ATENCION DOMICILIARIA") && (cNorm.contains("DOMICILIAR") || cNorm.contains("CONSULTA") || cNorm.contains("PAD"))) return true;
        if (gNorm.equals("SEGURIDAD Y SALUD EN EL TRABAJO") && (cNorm.contains("SST") || cNorm.contains("SEGURIDAD") || cNorm.contains("OCUPACIONAL"))) return true;
        if (gNorm.equals("SEGURIDAD DEL PACIENTE") && (cNorm.contains("PACIENTE") || cNorm.contains("CALIDAD") || cNorm.contains("SEGURIDAD"))) return true;
        if (gNorm.contains("INFRAESTRUCTURA") && (cNorm.contains("SISTEMA") || cNorm.contains("INFRAESTRUCTURA") || cNorm.contains("TECNOLOG") || cNorm.contains("MANTENIMIENTO"))) return true;
        if (gNorm.equals("TALENTO HUMANO") && (cNorm.contains("TALENTO") || cNorm.contains("HUMANO") || cNorm.contains("NOMINA") || cNorm.contains("PERSONAL"))) return true;
        if (gNorm.contains("FINANCIERA") && (cNorm.contains("FINANC") || cNorm.contains("CONTAB"))) return true;
        if (gNorm.contains("FACTURACION") && (cNorm.contains("FACTUR") || cNorm.contains("CARTERA") || cNorm.contains("CUENTAS"))) return true;
        if (gNorm.contains("COMPRAS") && (cNorm.contains("COMPRA") || cNorm.contains("ALMACEN") || cNorm.contains("SUMINISTRO"))) return true;
        if (gNorm.contains("DOCUMENTAL") && (cNorm.contains("DOCUMENT") || cNorm.contains("ARCHIV"))) return true;
        if (gNorm.equals("GERENCIA") && (cNorm.contains("GEREN") || cNorm.contains("DIRECTOR") || cNorm.contains("ASAMBLEA"))) return true;

        return false;
    }

    private String normalize(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("[\\u0300-\\u036f]", "")
                .trim()
                .toUpperCase();
    }
}
