package com.clinova.repository;

import com.clinova.entity.RegistroVacuna;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RegistroVacunaRepository extends JpaRepository<RegistroVacuna, Long> {
}