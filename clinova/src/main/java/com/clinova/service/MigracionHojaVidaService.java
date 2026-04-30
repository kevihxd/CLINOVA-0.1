package com.clinova.service;

import com.clinova.entity.HojaVida;
import com.clinova.repository.HojaVidaRepository;
import com.clinova.util.CsvUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MigracionHojaVidaService {

    private final HojaVidaRepository hojaVidaRepository;

    @Transactional
    public String migrarHojasDeVida(MultipartFile fileHojasVida) {
        if (fileHojasVida.isEmpty()) throw new RuntimeException("El archivo CSV es obligatorio");

        try {
            List<String[]> filas = CsvUtil.parsearCsvRobusto(fileHojasVida);
            int guardados = 0;
            boolean encabezadoPasado = false;

            for (String[] datos : filas) {
                if (!encabezadoPasado) {
                    if (datos.length > 1 && datos[1].equalsIgnoreCase("Cédula")) encabezadoPasado = true;
                    continue;
                }

                if (datos.length < 15) continue;

                String cedula = CsvUtil.obtenerDato(datos, 1);
                if (cedula.isEmpty()) continue;

                HojaVida hv = hojaVidaRepository.findByCedula(cedula)
                        .orElse(HojaVida.builder().cedula(cedula).build());

                hv.setNombres(CsvUtil.obtenerDato(datos, 2));
                hv.setApellidos(CsvUtil.obtenerDato(datos, 3));

                // Manejo de NOT NULL en fechas
                LocalDate fechaNac = CsvUtil.parsearFecha(CsvUtil.obtenerDato(datos, 4));
                hv.setFechaNacimiento(fechaNac != null ? fechaNac : LocalDate.of(1900, 1, 1));

                LocalDate fechaIngreso = CsvUtil.parsearFecha(CsvUtil.obtenerDato(datos, 12));
                hv.setFechaIngreso(fechaIngreso != null ? fechaIngreso : LocalDate.now());

                hv.setDireccionResidencia(CsvUtil.obtenerDato(datos, 5));
                hv.setTelefono(CsvUtil.obtenerDato(datos, 6));
                hv.setContactoEmergencia(CsvUtil.obtenerDato(datos, 7));
                hv.setTelefonoContactoEmergencia(CsvUtil.obtenerDato(datos, 8));
                hv.setArl(CsvUtil.obtenerDato(datos, 9));
                hv.setAfp(CsvUtil.obtenerDato(datos, 10));
                hv.setEps(CsvUtil.obtenerDato(datos, 11));
                hv.setEstado(CsvUtil.obtenerDato(datos, 14));
                hv.setTipoContrato(CsvUtil.obtenerDato(datos, 15));

                if (datos.length > 22) hv.setFechaRetiro(CsvUtil.parsearFecha(CsvUtil.obtenerDato(datos, 22)));
                if (datos.length > 23) hv.setCorreoElectronico(CsvUtil.obtenerDato(datos, 23));
                if (datos.length > 24) hv.setPesv(CsvUtil.obtenerDato(datos, 24));

                hojaVidaRepository.save(hv);
                guardados++;
            }

            return "Hojas de Vida migradas/actualizadas: " + guardados;

        } catch (Exception e) {
            throw new RuntimeException("Error en la migración: " + e.getMessage(), e);
        }
    }
}