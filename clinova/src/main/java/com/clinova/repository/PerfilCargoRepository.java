package com.clinova.repository;

import com.clinova.entity.PerfilCargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PerfilCargoRepository extends JpaRepository<PerfilCargo, Long> {
    Optional<PerfilCargo> findByCargoId(Long cargoId);
}