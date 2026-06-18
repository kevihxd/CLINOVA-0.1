package com.clinova.repository;

import com.clinova.entity.ActaHistorial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActaHistorialRepository extends JpaRepository<ActaHistorial, Long> {
    List<ActaHistorial> findByActaIdOrderByFechaDesc(Long actaId);
}
