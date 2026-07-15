package com.clinova.repository;

import com.clinova.entity.ParteInteresada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParteInteresadaRepository extends JpaRepository<ParteInteresada, Long> {
}
