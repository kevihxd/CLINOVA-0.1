package com.clinova.service;

import com.clinova.dto.ActaDTO;
import com.clinova.entity.Acta;
import com.clinova.entity.Role;
import com.clinova.entity.Usuario;
import com.clinova.repository.ActaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActaService {

    private final ActaRepository actaRepository;
    private final ActaHistorialService actaHistorialService;

    @Transactional
    public ActaDTO crearActa(ActaDTO actaDTO, Usuario usuarioAutenticado) {
        String creador = usuarioAutenticado != null ? usuarioAutenticado.getUsername() : "Sistema";
        Acta acta = Acta.builder()
                .titulo(actaDTO.titulo())
                .contenidoHtml(actaDTO.contenidoHtml())
                .estado(actaDTO.estado())
                .tipo(actaDTO.tipo())
                .responsable(actaDTO.responsable() != null && !actaDTO.responsable().isEmpty() ? actaDTO.responsable() : creador)
                .fecha(actaDTO.fecha() != null ? actaDTO.fecha() : LocalDate.now())
                .proceso(actaDTO.proceso())
                .sede(actaDTO.sede())
                .fechaInicio(actaDTO.fechaInicio())
                .horaInicio(actaDTO.horaInicio())
                .fechaFin(actaDTO.fechaFin())
                .horaFin(actaDTO.horaFin())
                .lugar(actaDTO.lugar())
                .enlaceVirtual(actaDTO.enlaceVirtual())
                .quienCita(actaDTO.quienCita())
                .confidencial(actaDTO.confidencial() != null ? actaDTO.confidencial() : false)
                .elaborador(actaDTO.elaborador())
                .area(actaDTO.area())
                .palabrasClave(actaDTO.palabrasClave())
                .compromisosAprobacion(actaDTO.compromisosAprobacion())
                .convertirDocumento(actaDTO.convertirDocumento())
                .requiereAprobacionActa(actaDTO.requiereAprobacionActa())
                .build();

        acta = actaRepository.save(acta);

        // Registrar en historial de trazabilidad
        String desc = "Creación inicial del acta. Proceso: " + (acta.getProceso() != null ? acta.getProceso() : "Ninguno") + ".";
        actaHistorialService.registrarHistorial(acta.getId(), "CREACION", desc, usuarioAutenticado);

        return mapearADto(acta);
    }

    @Transactional(readOnly = true)
    public List<ActaDTO> obtenerTodas() {
        return actaRepository.findAll().stream()
                .map(this::mapearADto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ActaDTO obtenerPorId(Long id) {
        return mapearADto(buscarActaPorId(id));
    }

    @Transactional
    public ActaDTO actualizarActa(Long id, ActaDTO actaDTO, Usuario usuarioAutenticado) {
        Acta acta = buscarActaPorId(id);

        validarPermisoEdicion(acta, usuarioAutenticado);

        // Detectar cambios para la descripción de trazabilidad
        StringBuilder cambios = new StringBuilder("Modificación del acta.");
        boolean huboCambios = false;

        if (!acta.getTitulo().equals(actaDTO.titulo())) {
            cambios.append(" Título cambiado de '").append(acta.getTitulo()).append("' a '").append(actaDTO.titulo()).append("'.");
            huboCambios = true;
        }
        if (!acta.getEstado().equals(actaDTO.estado())) {
            cambios.append(" Estado cambiado de '").append(acta.getEstado()).append("' a '").append(actaDTO.estado()).append("'.");
            huboCambios = true;
            
            // También registramos un evento específico de CAMBIO_ESTADO
            actaHistorialService.registrarHistorial(
                    acta.getId(),
                    "CAMBIO_ESTADO",
                    "Estado cambiado de '" + acta.getEstado() + "' a '" + actaDTO.estado() + "'",
                    usuarioAutenticado
            );
        }
        String procesoActual = acta.getProceso() != null ? acta.getProceso() : "";
        String procesoNuevo = actaDTO.proceso() != null ? actaDTO.proceso() : "";
        if (!procesoActual.equals(procesoNuevo)) {
            cambios.append(" Proceso asociado cambiado de '").append(procesoActual.isEmpty() ? "Ninguno" : procesoActual).append("' a '").append(procesoNuevo.isEmpty() ? "Ninguno" : procesoNuevo).append("'.");
            huboCambios = true;
        }

        acta.setTitulo(actaDTO.titulo());
        acta.setContenidoHtml(actaDTO.contenidoHtml());
        acta.setEstado(actaDTO.estado());
        acta.setTipo(actaDTO.tipo());
        acta.setProceso(actaDTO.proceso());
        if (actaDTO.fecha() != null) {
            acta.setFecha(actaDTO.fecha());
        }
        acta.setSede(actaDTO.sede());
        acta.setFechaInicio(actaDTO.fechaInicio());
        acta.setHoraInicio(actaDTO.horaInicio());
        acta.setFechaFin(actaDTO.fechaFin());
        acta.setHoraFin(actaDTO.horaFin());
        acta.setLugar(actaDTO.lugar());
        acta.setEnlaceVirtual(actaDTO.enlaceVirtual());
        acta.setQuienCita(actaDTO.quienCita());
        acta.setConfidencial(actaDTO.confidencial() != null ? actaDTO.confidencial() : false);
        acta.setElaborador(actaDTO.elaborador());
        acta.setArea(actaDTO.area());
        acta.setPalabrasClave(actaDTO.palabrasClave());
        acta.setCompromisosAprobacion(actaDTO.compromisosAprobacion());
        acta.setConvertirDocumento(actaDTO.convertirDocumento());
        acta.setRequiereAprobacionActa(actaDTO.requiereAprobacionActa());

        acta = actaRepository.save(acta);

        if (huboCambios) {
            actaHistorialService.registrarHistorial(acta.getId(), "MODIFICACION", cambios.toString(), usuarioAutenticado);
        } else {
            actaHistorialService.registrarHistorial(acta.getId(), "MODIFICACION", "Modificación del contenido o metadatos sin alterar título/proceso/estado.", usuarioAutenticado);
        }

        return mapearADto(acta);
    }

    @Transactional
    public void eliminarActa(Long id, Usuario usuarioAutenticado) {
        Acta acta = buscarActaPorId(id);
        // Registrar en historial antes de eliminar para mantener registro de auditoría
        actaHistorialService.registrarHistorial(
                acta.getId(),
                "ELIMINACION",
                "Acta '" + acta.getTitulo() + "' (ID: " + id + ") eliminada permanentemente.",
                usuarioAutenticado
        );
        actaRepository.delete(acta);
    }

    private Acta buscarActaPorId(Long id) {
        return actaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Acta no encontrada con ID: " + id));
    }

    private ActaDTO mapearADto(Acta acta) {
        return new ActaDTO(
                acta.getId(),
                acta.getTitulo(),
                acta.getContenidoHtml(),
                acta.getEstado(),
                acta.getTipo(),
                acta.getResponsable(),
                acta.getFecha(),
                acta.getProceso(),
                acta.getSede(),
                acta.getFechaInicio(),
                acta.getHoraInicio(),
                acta.getFechaFin(),
                acta.getHoraFin(),
                acta.getLugar(),
                acta.getEnlaceVirtual(),
                acta.getQuienCita(),
                acta.getConfidencial(),
                acta.getElaborador(),
                acta.getArea(),
                acta.getPalabrasClave(),
                acta.getCompromisosAprobacion(),
                acta.getConvertirDocumento(),
                acta.getRequiereAprobacionActa()
        );
    }

    private void validarPermisoEdicion(Acta acta, Usuario usuarioAutenticado) {
        if (usuarioAutenticado.getRol() == Role.ADMIN) {
            return;
        }

        if (usuarioAutenticado.getRol() == Role.LIDER_DE_PROCESO) {
            boolean esPropietario = acta.getResponsable().equals(usuarioAutenticado.getUsername());
            if (!esPropietario) {
                throw new AccessDeniedException("No tienes permiso para editar un acta creada por otro líder de proceso.");
            }
        }
    }
}