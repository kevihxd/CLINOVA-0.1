package com.clinova.repository;

import com.clinova.entity.Plantilla;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlantillaRepository extends JpaRepository<Plantilla, Long> {
}