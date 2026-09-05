package com.clinova.repository;

import com.clinova.entity.ReporteSistema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteSistemaRepository extends JpaRepository<ReporteSistema, Long> {
    List<ReporteSistema> findAllByOrderByFechaCreacionDesc();
}
