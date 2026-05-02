package com.clinova.repository;

import com.clinova.entity.CategoriaSoporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaSoporteRepository extends JpaRepository<CategoriaSoporte, Long> {
}