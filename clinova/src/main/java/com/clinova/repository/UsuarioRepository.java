package com.clinova.repository;

import com.clinova.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    Boolean existsByUsername(String username);
    Optional<Usuario> findByPersona_NumeroDocumento(String numeroDocumento);

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        LEFT JOIN FETCH u.persona
        LEFT JOIN FETCH u.cargo c
        LEFT JOIN FETCH c.permisos
        LEFT JOIN FETCH u.permisosDirectos
        WHERE u.username = :username
    """)
    Optional<Usuario> findByUsernameWithPermisos(@Param("username") String username);

    @Query("""
        SELECT DISTINCT u FROM Usuario u
        LEFT JOIN FETCH u.persona
        LEFT JOIN FETCH u.cargo c
        LEFT JOIN FETCH c.permisos
        LEFT JOIN FETCH u.permisosDirectos
        WHERE u.id = :id
    """)
    Optional<Usuario> findByIdWithPermisos(@Param("id") Long id);

    @Query("SELECT DISTINCT u FROM Usuario u LEFT JOIN FETCH u.persona LEFT JOIN FETCH u.cargo ORDER BY u.id DESC")
    List<Usuario> findAllOptimized();
}