package com.clinova.service;

import com.clinova.entity.Documento;
import com.clinova.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final DocumentoRepository documentoRepository;

    @Transactional(readOnly = true)
    public List<Documento> obtenerTodos() {
        return documentoRepository.findAll();
    }

    @Transactional
    public Documento crear(Documento documento) {
        return documentoRepository.save(documento);
    }

    @Transactional
    public void eliminar(Long id) {
        if (documentoRepository.existsById(id)) {
            documentoRepository.deleteById(id);
        }
    }
}