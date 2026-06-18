package com.clinova.repository;

import com.clinova.entity.Acta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActaRepository extends JpaRepository<Acta, Long> {
    Optional<Acta> findByKawakId(Long kawakId);
}