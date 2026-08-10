package com.clinova.repository;

import com.clinova.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    Boolean existsByUsername(String username);
    Optional<Usuario> findByPersona_NumeroDocumento(String numeroDocumento);

    @Query("SELECT DISTINCT u FROM Usuario u LEFT JOIN FETCH u.persona LEFT JOIN FETCH u.cargo LEFT JOIN FETCH u.hojaVida hv ORDER BY u.id DESC")
    List<Usuario> findAllOptimized();
}