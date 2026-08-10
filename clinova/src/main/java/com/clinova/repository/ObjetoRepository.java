package com.clinova.repository;

import com.clinova.entity.Objeto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ObjetoRepository extends JpaRepository<Objeto, Long> {
    Optional<Objeto> findByNombre(String nombre);
}
