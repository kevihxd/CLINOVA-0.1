package com.clinova.repository;

import com.clinova.entity.AnalisisContexto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalisisContextoRepository extends JpaRepository<AnalisisContexto, Long> {
}
