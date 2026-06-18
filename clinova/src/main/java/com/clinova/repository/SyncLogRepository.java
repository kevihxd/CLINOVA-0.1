package com.clinova.repository;

import com.clinova.entity.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {
    List<SyncLog> findTop20ByOrderByFechaEjecucionDesc();
    List<SyncLog> findByModuloOrderByFechaEjecucionDesc(String modulo);
}
