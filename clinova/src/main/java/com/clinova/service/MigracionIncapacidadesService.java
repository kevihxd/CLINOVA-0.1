package com.clinova.service;

import com.clinova.entity.Incapacidad;
import com.clinova.entity.Usuario;
import com.clinova.repository.IncapacidadRepository;
import com.clinova.repository.UsuarioRepository;
import com.clinova.util.CsvUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MigracionIncapacidadesService {

    private final IncapacidadRepository incapacidadRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public String migrarIncapacidades(MultipartFile fileCsv) {
        if (fileCsv.isEmpty()) throw new RuntimeException("El archivo CSV es obligatorio");

        try {
            List<String[]> filas = CsvUtil.parsearCsvRobusto(fileCsv);
            int guardados = 0;
            int usuariosNoEncontrados = 0;
            int filaCorta = 0;
            int documentoVacio = 0;
            for (int i = 0; i < filas.size(); i++) {
                String[] datos = filas.get(i);


                if (datos.length < 3) {
                    filaCorta++;
                    continue;
                }

                String documento = CsvUtil.obtenerDato(datos, 2);
                if (documento.isEmpty()) {
                    documentoVacio++;
                    continue;
                }

                Usuario usuario = usuarioRepository.findByPersona_NumeroDocumento(documento).orElse(null);
                if (usuario == null) {
                    usuariosNoEncontrados++;
                    continue;
                }

                Incapacidad inc = new Incapacidad();
                inc.setUsuario(usuario);
                inc.setEpsArl(CsvUtil.obtenerDato(datos, 4));
                inc.setTipoIncapacidad(CsvUtil.obtenerDato(datos, 5));
                inc.setCodigo(CsvUtil.obtenerDato(datos, 6));
                inc.setDx(CsvUtil.obtenerDato(datos, 7));
                inc.setFechaInicio(parsearFechaFlexible(CsvUtil.obtenerDato(datos, 8)));
                inc.setFechaFin(parsearFechaFlexible(CsvUtil.obtenerDato(datos, 9)));
                inc.setDiasOtorgados(parsearInteger(CsvUtil.obtenerDato(datos, 10)));
                inc.setDiasAprobados(parsearInteger(CsvUtil.obtenerDato(datos, 11)));
                inc.setFechaReporteTH(parsearFechaFlexible(CsvUtil.obtenerDato(datos, 12)));
                inc.setFechaRadicado(parsearFechaFlexible(CsvUtil.obtenerDato(datos, 13)));
                inc.setEstado(CsvUtil.obtenerDato(datos, 14));
                inc.setNumeroRadicacion(CsvUtil.obtenerDato(datos, 15));
                inc.setIbc(parsearBigDecimal(CsvUtil.obtenerDato(datos, 16)));
                inc.setDiasPagadosIps(parsearInteger(CsvUtil.obtenerDato(datos, 17)));
                inc.setValorLiquidadoIps(parsearBigDecimal(CsvUtil.obtenerDato(datos, 18)));
                inc.setDiasPagadosEps(parsearInteger(CsvUtil.obtenerDato(datos, 19)));
                inc.setValorLiquidadoEps(parsearBigDecimal(CsvUtil.obtenerDato(datos, 20)));
                inc.setDiasPagadosArl(parsearInteger(CsvUtil.obtenerDato(datos, 21)));
                inc.setCampo30(CsvUtil.obtenerDato(datos, 22));
                inc.setCampo60(CsvUtil.obtenerDato(datos, 23));
                inc.setCampo90(CsvUtil.obtenerDato(datos, 24));
                inc.setCampo180(CsvUtil.obtenerDato(datos, 25));
                inc.setObservaciones(CsvUtil.obtenerDato(datos, 26));
                inc.setValorPago(parsearBigDecimal(CsvUtil.obtenerDato(datos, 28)));
                inc.setFechaPago(parsearFechaFlexible(CsvUtil.obtenerDato(datos, 29)));
                inc.setNumeroComprobantePago(CsvUtil.obtenerDato(datos, 30));

                incapacidadRepository.save(inc);
                guardados++;
            }

            return "RESULTADO: \n" +
                   "- Guardados: " + guardados + "\n" +
                   "- Filas vacías/cortas omitidas: " + filaCorta + "\n" +
                   "- Filas sin documento omitidas: " + documentoVacio + "\n" +
                   "- Documentos no existentes en DB (o es el encabezado): " + usuariosNoEncontrados + "\n" +
                   "- Total filas leídas del archivo: " + filas.size();

        } catch (Exception e) {
            throw new RuntimeException("Error en la migración de incapacidades: " + e.getMessage(), e);
        }
    }

    private LocalDate parsearFechaFlexible(String fechaStr) {
        if (fechaStr == null || fechaStr.isBlank()) return null;
        fechaStr = fechaStr.trim().split(" ")[0]; // Quitar hora si la hay
        
        try { return LocalDate.parse(fechaStr, DateTimeFormatter.ofPattern("dd/MM/yyyy")); } catch (DateTimeParseException ignored) {}
        try { return LocalDate.parse(fechaStr, DateTimeFormatter.ofPattern("yyyy-MM-dd")); } catch (DateTimeParseException ignored) {}
        try { return LocalDate.parse(fechaStr, DateTimeFormatter.ofPattern("MM/dd/yyyy")); } catch (DateTimeParseException ignored) {}
        
        return null;
    }

    private Integer parsearInteger(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return (int) Double.parseDouble(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parsearBigDecimal(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            // Limpiar signos de dolar y puntos/comas de miles
            valor = valor.replace("$", "").replace(".", "").replace(",", ".").trim();
            return new BigDecimal(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
