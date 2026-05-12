package com.clinova.service;

import com.clinova.entity.CursoAsignado;
import com.clinova.repository.CursoAsignadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import com.clinova.dto.CursoAsignadoDTO;
import com.clinova.dto.CursoMaestroDTO;
import com.clinova.entity.CursoMaestro;
import com.clinova.repository.CursoMaestroRepository;
import com.clinova.repository.UsuarioRepository;
import com.clinova.entity.Usuario;

@Service
@RequiredArgsConstructor
public class CursosService {

    private final CursoAsignadoRepository cursoAsignadoRepository;
    private final SoporteService soporteService;
    private final CursoMaestroRepository cursoMaestroRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<CursoMaestroDTO> obtenerCursosMaestros() {
        return cursoMaestroRepository.findAll().stream()
                .map(cm -> new CursoMaestroDTO(cm.getId(), cm.getNombre(), cm.getDescripcion(), cm.getFechaLimiteGlobal(), cm.getEsGlobal(), cm.getMesesVigencia()))
                .collect(Collectors.toList());
    }

    @Transactional
    public CursoMaestroDTO crearCursoMaestro(CursoMaestroDTO request) {
        CursoMaestro cm = CursoMaestro.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .fechaLimiteGlobal(request.fechaLimiteGlobal())
                .esGlobal(request.esGlobal() != null ? request.esGlobal() : true)
                .mesesVigencia(request.mesesVigencia() != null ? request.mesesVigencia() : 12)
                .build();
        CursoMaestro guardado = cursoMaestroRepository.save(cm);
        if (guardado.getEsGlobal() != null && guardado.getEsGlobal()) {
            asignarMasivo(guardado.getId());
        }
        return new CursoMaestroDTO(guardado.getId(), guardado.getNombre(), guardado.getDescripcion(), guardado.getFechaLimiteGlobal(), guardado.getEsGlobal(), guardado.getMesesVigencia());
    }

    @Transactional
    public CursoMaestroDTO actualizarCursoMaestro(Long id, CursoMaestroDTO request) {
        CursoMaestro curso = cursoMaestroRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Curso maestro no encontrado"));
        curso.setNombre(request.nombre());
        curso.setDescripcion(request.descripcion());
        curso.setFechaLimiteGlobal(request.fechaLimiteGlobal());
        if (request.esGlobal() != null) curso.setEsGlobal(request.esGlobal());
        if (request.mesesVigencia() != null) curso.setMesesVigencia(request.mesesVigencia());
        CursoMaestro guardado = cursoMaestroRepository.save(curso);
        return new CursoMaestroDTO(guardado.getId(), guardado.getNombre(), guardado.getDescripcion(), guardado.getFechaLimiteGlobal(), guardado.getEsGlobal(), guardado.getMesesVigencia());
    }

    @Transactional
    public void eliminarCursoMaestro(Long id) {
        // Find all asignaciones and delete them first
        List<CursoAsignado> asignaciones = cursoAsignadoRepository.findAll().stream()
                .filter(ca -> ca.getCursoMaestro().getId().equals(id))
                .collect(Collectors.toList());
        cursoAsignadoRepository.deleteAll(asignaciones);
        cursoMaestroRepository.deleteById(id);
    }

    @Transactional
    public void asignarMasivo(Long cursoMaestroId) {
        CursoMaestro curso = cursoMaestroRepository.findById(cursoMaestroId)
                .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

        List<Usuario> usuarios = usuarioRepository.findAll();
        for (Usuario u : usuarios) {
            boolean yaAsignado = cursoAsignadoRepository.findByUsuarioId(u.getId()).stream()
                    .anyMatch(ca -> ca.getCursoMaestro().getId().equals(cursoMaestroId));
            if (!yaAsignado) {
                CursoAsignado ca = CursoAsignado.builder()
                        .usuario(u)
                        .cursoMaestro(curso)
                        .estado("PENDIENTE")
                        .build();
                cursoAsignadoRepository.save(ca);
            }
        }
    }

    @Transactional
    public void asignarCurso(Long usuarioId, Long cursoMaestroId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        CursoMaestro curso = cursoMaestroRepository.findById(cursoMaestroId)
                .orElseThrow(() -> new RuntimeException("Curso no encontrado"));

        boolean yaAsignado = cursoAsignadoRepository.findByUsuarioId(usuarioId).stream()
                .anyMatch(ca -> ca.getCursoMaestro().getId().equals(cursoMaestroId));

        if (!yaAsignado) {
            CursoAsignado ca = CursoAsignado.builder()
                    .usuario(u)
                    .cursoMaestro(curso)
                    .estado("PENDIENTE")
                    .build();
            cursoAsignadoRepository.save(ca);
        }
    }

    @Transactional
    public void eliminarAsignacion(Long asignacionId) {
        CursoAsignado ca = cursoAsignadoRepository.findById(asignacionId)
                .orElseThrow(() -> new RuntimeException("Asignación no encontrada"));
        if (ca.getCursoMaestro().getEsGlobal() != null && ca.getCursoMaestro().getEsGlobal()) {
            throw new RuntimeException("No se puede eliminar la asignación de un curso global.");
        }
        cursoAsignadoRepository.deleteById(asignacionId);
    }

    public List<CursoAsignadoDTO> obtenerCursosPorUsuario(Long usuarioId) {
        return cursoAsignadoRepository.findByUsuarioId(usuarioId).stream()
                .map(ca -> {
                    boolean permiteCarga = true;
                    LocalDate limite = ca.getCursoMaestro().getFechaLimiteGlobal();
                    if (limite != null && LocalDate.now().isAfter(limite)) {
                        permiteCarga = false;
                    }
                    String usuarioNombre = ca.getUsuario() != null && ca.getUsuario().getPersona() != null
                            ? ca.getUsuario().getPersona().getPrimerNombre() + " " + ca.getUsuario().getPersona().getPrimerApellido()
                            : (ca.getUsuario() != null ? ca.getUsuario().getUsername() : "Desconocido");

                    return new CursoAsignadoDTO(
                            ca.getId(),
                            ca.getCursoMaestro().getNombre(),
                            ca.getCursoMaestro().getDescripcion(),
                            usuarioNombre,
                            ca.getEstado(),
                            ca.getFechaRealizacion(),
                            ca.getFechaExpiracion(),
                            limite,
                            ca.getCertificadoUrl(),
                            permiteCarga,
                            ca.getCursoMaestro().getEsGlobal() != null ? ca.getCursoMaestro().getEsGlobal() : false
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void subirCertificado(Long asignacionId, MultipartFile archivo) {
        CursoAsignado ca = cursoAsignadoRepository.findById(asignacionId)
                .orElseThrow(() -> new RuntimeException("Asignación no encontrada"));

        LocalDate limite = ca.getCursoMaestro().getFechaLimiteGlobal();
        if (limite != null && LocalDate.now().isAfter(limite)) {
            throw new RuntimeException("El plazo para subir certificados ha vencido");
        }

        String url = soporteService.guardarArchivo(archivo, "certificados");
        ca.setCertificadoUrl(url);
        ca.setEstado("COMPLETADO");
        ca.setFechaRealizacion(LocalDate.now());
        Integer meses = ca.getCursoMaestro().getMesesVigencia() != null ? ca.getCursoMaestro().getMesesVigencia() : 12;
        ca.setFechaExpiracion(LocalDate.now().plusMonths(meses));

        cursoAsignadoRepository.save(ca);
    }

    @Transactional
    public void asignarCursosGlobalesAUsuario(Usuario usuario) {
        List<CursoMaestro> globales = cursoMaestroRepository.findByEsGlobalTrue();
        for (CursoMaestro curso : globales) {
            boolean yaAsignado = cursoAsignadoRepository.findByUsuarioId(usuario.getId()).stream()
                    .anyMatch(ca -> ca.getCursoMaestro().getId().equals(curso.getId()));
            if (!yaAsignado) {
                CursoAsignado ca = CursoAsignado.builder()
                        .usuario(usuario)
                        .cursoMaestro(curso)
                        .estado("PENDIENTE")
                        .build();
                cursoAsignadoRepository.save(ca);
            }
        }
    }

    @Transactional
    public void actualizarEstado(Long id, String estado) {
        CursoAsignado ca = cursoAsignadoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Asignación no encontrada"));
        ca.setEstado(estado);
        cursoAsignadoRepository.save(ca);
    }
}