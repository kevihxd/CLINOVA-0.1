package com.clinova.repository;

import com.clinova.entity.CursoMaestro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CursoMaestroRepository extends JpaRepository<CursoMaestro, Long> {
    List<CursoMaestro> findByEsGlobalTrue();
}