package com.clinova.repository;

import com.clinova.entity.PlantillaCorreo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlantillaCorreoRepository extends JpaRepository<PlantillaCorreo, Long> {
}
