package com.clinova.service;

import com.clinova.dto.SemaforizacionReporteDTO;
import com.clinova.entity.Usuario;
import com.clinova.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SemaforizacionService {

    private final JdbcTemplate jdbcTemplate;

    public List<SemaforizacionReporteDTO> obtenerReporteSemaforizacionGlobal(String area) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                hv.id                                          AS usuario_id,
                CONCAT(hv.nombres, ' ', hv.apellidos)         AS nombre_completo,
                hv.cedula                                      AS numero_documento,
                COALESCE(c.nombre, 'Sin Cargo')                AS cargo,
                cm.nombre                                      AS curso_requerido,
                cm.meses_vigencia,
                ca.fecha_realizacion,
                ca.fecha_expiracion                            AS fecha_vencimiento,
                ca.certificado_url,
                ca.estado                                      AS estado_asignado,
                hv.estado                                      AS estado_empleado,
                CASE WHEN ccm.cargo_id IS NOT NULL THEN 1 ELSE 0 END AS is_required
            FROM hojas_vida hv
            LEFT JOIN hojas_vida_cargos hvc ON hv.id = hvc.hoja_vida_id
            LEFT JOIN cargos c              ON hvc.cargo_id = c.id
        """);

        boolean filterByArea = area != null && !area.trim().isEmpty() && !"TODOS".equalsIgnoreCase(area);

        if (filterByArea) {
            sql.append("""
                JOIN cargos_cursos_maestros ccm ON ccm.cargo_id = c.id
                JOIN curso_maestro cm ON ccm.curso_maestro_id = cm.id
            """);
        } else {
            sql.append("""
                JOIN  curso_maestro cm          ON 1=1
                LEFT JOIN cargos_cursos_maestros ccm ON ccm.cargo_id = c.id AND ccm.curso_maestro_id = cm.id
            """);
        }

        sql.append("""
            LEFT JOIN curso_asignado ca     ON ca.hoja_vida_id = hv.id AND ca.curso_maestro_id = cm.id
        """);

        Object[] params;
        if (filterByArea) {
            sql.append(" WHERE c.area_semaforizacion = ? ");
            params = new Object[]{area.trim()};
        } else {
            params = new Object[]{};
        }

        sql.append(" ORDER BY hv.apellidos, hv.nombres, cm.nombre ");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            LocalDate realizacion = rs.getDate("fecha_realizacion") != null
                    ? rs.getDate("fecha_realizacion").toLocalDate() : null;
            LocalDate vencimiento = rs.getDate("fecha_vencimiento") != null
                    ? rs.getDate("fecha_vencimiento").toLocalDate() : null;

            int mesesVigencia = rs.getInt("meses_vigencia");

            if (realizacion != null && mesesVigencia > 0) {
                LocalDate vencimientoCalculado = realizacion.plusMonths(mesesVigencia);
                if (vencimiento == null || !vencimiento.equals(vencimientoCalculado)) {
                    vencimiento = vencimientoCalculado;
                }
            }

            boolean isRequired = rs.getInt("is_required") == 1;
            String estadoAsignado = rs.getString("estado_asignado");
            
            String estado = "FALTANTE";
            
            if (!isRequired) {
                estado = "NO_APLICA";
            } else if (vencimiento != null) {
                long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), vencimiento);
                if (diasRestantes < 0)        estado = "VENCIDO";
                else if (diasRestantes <= 30) estado = "POR_VENCER";
                else                          estado = "VIGENTE";
            } else if (mesesVigencia == 0 && ("COMPLETADO".equalsIgnoreCase(estadoAsignado) || realizacion != null || rs.getString("certificado_url") != null)) {
                estado = "VIGENTE";
            }

            return SemaforizacionReporteDTO.builder()
                    .usuarioId(rs.getLong("usuario_id"))
                    .nombreCompleto(rs.getString("nombre_completo"))
                    .documento(rs.getString("numero_documento"))
                    .cargo(rs.getString("cargo"))
                    .cursoRequerido(rs.getString("curso_requerido"))
                    .fechaRealizacion(realizacion)
                    .fechaVencimiento(vencimiento)
                    .soporteUrl(rs.getString("certificado_url"))
                    .estadoCurso(estado)
                    .estadoEmpleado(rs.getString("estado_empleado"))
                    .build();
        });
    }
}