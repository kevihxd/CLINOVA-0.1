package com.clinova.repository;

import com.clinova.entity.ApoyoTerapeuticoAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApoyoTerapeuticoAdjuntoRepository extends JpaRepository<ApoyoTerapeuticoAdjunto, Long> {

    List<ApoyoTerapeuticoAdjunto> findByIngresoIdOrderByFechaSubidaDescIdDesc(Long ingresoId);

    List<ApoyoTerapeuticoAdjunto> findByIngresoIdAndTipoAdjuntoAndVigenteTrue(Long ingresoId, String tipoAdjunto);

    List<ApoyoTerapeuticoAdjunto> findByIngresoIdAndTipoAdjuntoOrderByFechaSubidaDescIdDesc(Long ingresoId, String tipoAdjunto);

    List<ApoyoTerapeuticoAdjunto> findByIngresoIdInOrderByFechaSubidaDescIdDesc(List<Long> ingresoIds);
}
