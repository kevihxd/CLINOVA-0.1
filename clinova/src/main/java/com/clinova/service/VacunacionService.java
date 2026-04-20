package com.clinova.service;

import com.clinova.dto.PersonaStatusDTO;
import com.clinova.dto.RegistroVacunaDTO;
import com.clinova.entity.Persona;
import com.clinova.entity.RegistroVacuna;
import com.clinova.entity.Vacuna;
import com.clinova.repository.PersonaRepository;
import com.clinova.repository.RegistroVacunaRepository;
import com.clinova.repository.VacunaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VacunacionService {

    private final PersonaRepository personaRepository;
    private final RegistroVacunaRepository registroVacunaRepository;
    private final VacunaRepository vacunaRepository;

    public List<Vacuna> listarCatalogoVacunas() {
        return vacunaRepository.findAll();
    }

    @Transactional
    public Vacuna crearVacuna(Vacuna vacuna) {
        return vacunaRepository.save(vacuna);
    }

    @Transactional
    public Vacuna editarVacuna(Long id, Vacuna datos) {
        Vacuna vacuna = vacunaRepository.findById(id).orElseThrow(() -> new RuntimeException("Vacuna no encontrada"));
        vacuna.setNombre(datos.getNombre());
        vacuna.setPerfil(datos.getPerfil());
        vacuna.setDosisRequeridas(datos.getDosisRequeridas());
        vacuna.setRequiereRefuerzo(datos.getRequiereRefuerzo());
        return vacunaRepository.save(vacuna);
    }

    @Transactional
    public void eliminarVacuna(Long id) {
        if (registroVacunaRepository.existsByVacunaId(id)) {
            throw new RuntimeException("No se puede eliminar porque ya existen colaboradores con dosis registradas.");
        }
        vacunaRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PersonaStatusDTO buscarPorDocumento(String documento) {
        Persona p = personaRepository.findByNumeroDocumento(documento)
                .orElseThrow(() -> new RuntimeException("Personal no encontrado"));

        List<Vacuna> todasLasVacunas = vacunaRepository.findAll();
        List<RegistroVacuna> registros = registroVacunaRepository.findByPersonaId(p.getId());

        List<PersonaStatusDTO.RequisitoDetalleDTO> requisitos = todasLasVacunas.stream().map(v -> {
            RegistroVacuna reg = registros.stream()
                    .filter(r -> r.getVacuna().getId().equals(v.getId()))
                    .findFirst().orElse(null);

            boolean vencido = reg != null && reg.getFechaVencimiento() != null && reg.getFechaVencimiento().isBefore(LocalDate.now());

            return PersonaStatusDTO.RequisitoDetalleDTO.builder()
                    .registroId(reg != null ? reg.getId() : null)
                    .vacunaId(v.getId())
                    .nombre(v.getNombre())
                    .completado(reg != null)
                    .detalleDosis(reg != null ? reg.getDetalleDosis() : null)
                    .fechaAplicacion(reg != null ? reg.getFechaAplicacion() : null)
                    .fechaVencimiento(reg != null ? reg.getFechaVencimiento() : null)
                    .vencido(vencido)
                    .build();
        }).collect(Collectors.toList());

        return PersonaStatusDTO.builder()
                .personaId(p.getId())
                .nombreCompleto(p.getPrimerNombre() + " " + p.getPrimerApellido())
                .numeroDocumento(p.getNumeroDocumento())
                .perfil(p.getUsuario() != null && p.getUsuario().getCargo() != null ? p.getUsuario().getCargo().getNombre() : "N/A")
                .requisitos(requisitos)
                .build();
    }

    @Transactional
    public void registrarDosis(RegistroVacunaDTO dto) {
        Persona p = personaRepository.findById(dto.getPersonaId()).orElseThrow();
        Vacuna v = vacunaRepository.findByNombre(dto.getNombreVacuna()).orElseThrow();

        RegistroVacuna registro = RegistroVacuna.builder()
                .persona(p)
                .vacuna(v)
                .detalleDosis(dto.getDetalleDosis())
                .fechaAplicacion(dto.getFechaAplicacion())
                .fechaVencimiento(dto.getFechaVencimiento())
                .build();

        registroVacunaRepository.save(registro);
    }

    @Transactional
    public void editarDosis(Long id, RegistroVacunaDTO dto) {
        RegistroVacuna registro = registroVacunaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Registro no encontrado"));

        registro.setDetalleDosis(dto.getDetalleDosis());
        registro.setFechaAplicacion(dto.getFechaAplicacion());
        registro.setFechaVencimiento(dto.getFechaVencimiento());

        registroVacunaRepository.save(registro);
    }

    @Transactional
    public void eliminarDosis(Long id) {
        registroVacunaRepository.deleteById(id);
    }
}