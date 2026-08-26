package com.clinova.service;

import com.clinova.dto.GrupoDistribucionDTO;
import com.clinova.entity.Cargo;
import com.clinova.entity.GrupoDistribucion;
import com.clinova.entity.HojaVida;
import com.clinova.repository.CargoRepository;
import com.clinova.repository.GrupoDistribucionRepository;
import com.clinova.repository.HojaVidaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GrupoDistribucionService {

    private final GrupoDistribucionRepository grupoRepository;
    private final HojaVidaRepository hojaVidaRepository;
    private final CargoRepository cargoRepository;

    private GrupoDistribucionDTO mapToDTO(GrupoDistribucion g, List<HojaVida> todasHojas) {
        Set<HojaVida> intsDirectos = g.getIntegrantes() != null ? g.getIntegrantes() : new HashSet<>();
        Set<Cargo> cargosVinculados = g.getCargos() != null ? g.getCargos() : new HashSet<>();

        List<Long> directIds = intsDirectos.stream().map(HojaVida::getId).collect(Collectors.toList());
        List<String> directNombres = intsDirectos.stream()
                .map(h -> (h.getNombres() + " " + (h.getApellidos() != null ? h.getApellidos() : "")).trim() + " (" + (h.getCedula() != null ? h.getCedula() : "S/N") + ")")
                .collect(Collectors.toList());

        List<Long> cargosIds = cargosVinculados.stream().map(Cargo::getId).collect(Collectors.toList());
        List<String> cargosNombres = cargosVinculados.stream().map(Cargo::getNombre).collect(Collectors.toList());

        // Calcular total de colaboradores (directos + los que tengan alguno de los cargos asignados)
        Set<Long> totalColaboradoresIds = new HashSet<>(directIds);

        if ("Clinical House- Todos".equalsIgnoreCase(g.getNombre())) {
            if (todasHojas != null) {
                todasHojas.forEach(h -> totalColaboradoresIds.add(h.getId()));
            }
        } else if (todasHojas != null) {
            for (HojaVida h : todasHojas) {
                if (h.getCargos() != null && !h.getCargos().isEmpty()) {
                    boolean coincide = h.getCargos().stream().anyMatch(c -> 
                        cargosVinculados.contains(c) || coincideCargoConGrupo(c.getNombre(), g.getNombre())
                    );
                    if (coincide) {
                        totalColaboradoresIds.add(h.getId());
                    }
                }
            }
        }

        return GrupoDistribucionDTO.builder()
                .id(g.getId())
                .nombre(g.getNombre())
                .descripcion(g.getDescripcion())
                .activo(g.getActivo() == null || g.getActivo())
                .totalIntegrantes(totalColaboradoresIds.size())
                .fechaCreacion(g.getFechaCreacion())
                .integrantesIds(directIds)
                .integrantesNombres(directNombres)
                .cargosIds(cargosIds)
                .cargosNombres(cargosNombres)
                .build();
    }

    @Transactional(readOnly = true)
    public List<GrupoDistribucionDTO> obtenerTodos() {
        List<GrupoDistribucion> grupos = grupoRepository.findAll();
        List<HojaVida> todasHojas = new ArrayList<>();
        try {
            todasHojas = hojaVidaRepository.findAll();
        } catch (Exception ignored) {}

        List<HojaVida> finalTodasHojas = todasHojas;
        return grupos.stream()
                .map(g -> mapToDTO(g, finalTodasHojas))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GrupoDistribucionDTO obtenerPorId(Long id) {
        GrupoDistribucion g = grupoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grupo de distribución no encontrado"));
        List<HojaVida> todasHojas = new ArrayList<>();
        try {
            todasHojas = hojaVidaRepository.findAll();
        } catch (Exception ignored) {}
        return mapToDTO(g, todasHojas);
    }

    @Transactional
    public GrupoDistribucionDTO crear(GrupoDistribucionDTO dto) {
        if (grupoRepository.existsByNombreIgnoreCase(dto.getNombre().trim())) {
            throw new RuntimeException("Ya existe un Grupo de Distribución con ese nombre");
        }

        Set<HojaVida> integrantes = new HashSet<>();
        if (dto.getIntegrantesIds() != null && !dto.getIntegrantesIds().isEmpty()) {
            integrantes.addAll(hojaVidaRepository.findAllById(dto.getIntegrantesIds()));
        }

        Set<Cargo> cargos = new HashSet<>();
        if (dto.getCargosIds() != null && !dto.getCargosIds().isEmpty()) {
            cargos.addAll(cargoRepository.findAllById(dto.getCargosIds()));
        }

        GrupoDistribucion g = GrupoDistribucion.builder()
                .nombre(dto.getNombre().trim())
                .descripcion(dto.getDescripcion() != null ? dto.getDescripcion().trim() : "")
                .activo(dto.getActivo() != null ? dto.getActivo() : true)
                .fechaCreacion(LocalDateTime.now())
                .integrantes(integrantes)
                .cargos(cargos)
                .build();

        g = grupoRepository.save(g);
        return mapToDTO(g, new ArrayList<>());
    }

    @Transactional
    public GrupoDistribucionDTO actualizar(Long id, GrupoDistribucionDTO dto) {
        GrupoDistribucion g = grupoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grupo de distribución no encontrado"));

        if (dto.getNombre() != null && !dto.getNombre().trim().equalsIgnoreCase(g.getNombre())) {
            if (grupoRepository.existsByNombreIgnoreCase(dto.getNombre().trim())) {
                throw new RuntimeException("Ya existe un Grupo de Distribución con ese nombre");
            }
            g.setNombre(dto.getNombre().trim());
        }

        if (dto.getDescripcion() != null) {
            g.setDescripcion(dto.getDescripcion().trim());
        }

        if (dto.getActivo() != null) {
            g.setActivo(dto.getActivo());
        }

        if (dto.getIntegrantesIds() != null) {
            Set<HojaVida> nuevosIntegrantes = new HashSet<>(hojaVidaRepository.findAllById(dto.getIntegrantesIds()));
            g.setIntegrantes(nuevosIntegrantes);
        }

        if (dto.getCargosIds() != null) {
            Set<Cargo> nuevosCargos = new HashSet<>(cargoRepository.findAllById(dto.getCargosIds()));
            g.setCargos(nuevosCargos);
        }

        g = grupoRepository.save(g);
        return mapToDTO(g, new ArrayList<>());
    }

    @Transactional
    public void eliminar(Long id) {
        if (grupoRepository.existsById(id)) {
            grupoRepository.deleteById(id);
        }
    }

    private boolean coincideCargoConGrupo(String nombreCargo, String nombreGrupo) {
        if (nombreCargo == null || nombreGrupo == null) return false;
        
        String cNorm = java.text.Normalizer.normalize(nombreCargo, java.text.Normalizer.Form.NFD)
                .replaceAll("[\\u0300-\\u036f]", "").toUpperCase();
        String gNorm = java.text.Normalizer.normalize(nombreGrupo, java.text.Normalizer.Form.NFD)
                .replaceAll("[\\u0300-\\u036f]", "").toUpperCase();

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
}
