package com.clinova.service;

import com.clinova.entity.Documento;
import com.clinova.repository.DocumentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DocumentoService {

    @Autowired
    private DocumentoRepository documentoRepository;

    public List<Documento> listarTodos() {
        return documentoRepository.findAll();
    }

    public Documento guardar(Documento documento) {
        if(documento.getCodigo() == null || documento.getCodigo().isEmpty()){
            documento.setCodigo("DOC-" + System.currentTimeMillis());
        }
        if(documento.getVersion() == null || documento.getVersion().isEmpty()){
            documento.setVersion("1");
        }
        return documentoRepository.save(documento);
    }

    public void eliminar(Long id) {
        documentoRepository.deleteById(id);
    }
}