package com.clinova.repository;

import com.clinova.entity.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    Optional<Documento> findByKawakId(Long kawakId);
    Optional<Documento> findByCodigo(String codigo);
}