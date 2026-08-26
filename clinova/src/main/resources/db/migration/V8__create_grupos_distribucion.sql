-- Flyway Migration V8: Create tables for Grupos de Distribucion, cargos mapping and seed all 43 official Kawak groups

CREATE TABLE IF NOT EXISTS grupos_distribucion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL UNIQUE,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS grupo_distribucion_integrantes (
    grupo_id BIGINT NOT NULL,
    hoja_vida_id BIGINT NOT NULL,
    PRIMARY KEY (grupo_id, hoja_vida_id),
    CONSTRAINT fk_gdi_grupo FOREIGN KEY (grupo_id) REFERENCES grupos_distribucion(id) ON DELETE CASCADE,
    CONSTRAINT fk_gdi_hoja_vida FOREIGN KEY (hoja_vida_id) REFERENCES hojas_vida(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS grupo_distribucion_cargos (
    grupo_id BIGINT NOT NULL,
    cargo_id BIGINT NOT NULL,
    PRIMARY KEY (grupo_id, cargo_id),
    CONSTRAINT fk_gdc_grupo FOREIGN KEY (grupo_id) REFERENCES grupos_distribucion(id) ON DELETE CASCADE,
    CONSTRAINT fk_gdc_cargo FOREIGN KEY (cargo_id) REFERENCES cargos(id) ON DELETE CASCADE
);

-- Poblar los 43 Grupos de Distribución oficiales de Kawak
INSERT INTO grupos_distribucion (id, nombre, descripcion, activo) VALUES
(1, 'Clinical House- Todos', 'Grupo general que incluye a todos los colaboradores de la institución', true),
(2, 'ATENCIÓN DOMICILIARIA Y CONSULTA EXTERNA SOLO FORMATOS', 'Formatos exclusivos para Atención Domiciliaria y Consulta Externa', true),
(3, 'FISIOTERAPIA', 'Grupo de personal de Fisioterapia y Rehabilitación', true),
(4, 'MEDICINA GENERAL', 'Grupo de Médicos Generales y Especialistas', true),
(5, 'TERAPIA OCUPACIONAL', 'Grupo de Profesionales en Terapia Ocupacional', true),
(6, 'FONOAUDIOLOGIA', 'Grupo de Profesionales en Fonoaudiología', true),
(7, 'ODONTOLOGIA', 'Grupo de Odontólogos y Auxiliares de Odontología', true),
(8, 'ENFERMERIA', 'Grupo de Enfermeras Jefes y Auxiliares de Enfermería', true),
(9, 'SERVICIO FARMACEUTICO', 'Grupo de Farmacia, Regentes y Auxiliares de Farmacia', true),
(10, 'ATENCIÓN DOMICILIARIA Y CONSULTA EXTERNA', 'Equipo asistencial de Atención Domiciliaria y Consulta Externa', true),
(11, 'GERENCIA', 'Grupo Gerencial y Alta Dirección', true),
(12, 'GESTIÓN DE CALIDAD', 'Equipo de Gestión de Calidad e Indicadores', true),
(13, 'SEGURIDAD DEL PACIENTE', 'Comité y personal de Seguridad del Paciente', true),
(14, 'SEGURIDAD Y SALUD EN EL TRABAJO', 'Equipo de Seguridad y Salud en el Trabajo (COPASST/SST)', true),
(15, 'INFRAESTRUCTURA Y TECNOLOGÍA', 'Personal de Sistemas, Redes e Infraestructura', true),
(16, 'TALENTO HUMANO', 'Gestión de Talento Humano y Nómina', true),
(17, 'GESTIÓN FINANCIERA Y CONTABILIDAD', 'Equipo de Contabilidad y Finanzas', true),
(18, 'FACTURACIÓN Y CARTERA', 'Personal de Facturación, Auditoría de Cartera y Cuentas Médicas', true),
(19, 'COMPRAS Y SUMINISTROS', 'Equipo de Compras y Almacén', true),
(20, 'GESTIÓN DOCUMENTAL Y ARCHIVO', 'Personal de Gestión Documental y Archivo', true),
(21, 'COPASST', 'Comité Paritario de Seguridad y Salud en el Trabajo', true),
(22, 'COMITÉ DE HISTORIAS CLÍNICAS', 'Comité de Historias Clínicas e Auditoría Médica', true),
(23, 'COMITÉ DE SEGURIDAD DEL PACIENTE', 'Comité Institucional de Seguridad del Paciente', true),
(24, 'COMITÉ DE COVE', 'Comité de Vigilancia Epidemiológica', true),
(25, 'COMITÉ DOCENCIA SERVICIO', 'Comité de Docencia e Investigación', true),
(26, 'COMITÉ DE COMPRAS', 'Comité Operativo de Compras', true),
(27, 'COMITÉ DE FARMACIA Y TERAPÉUTICA', 'Comité de Farmacia y Uso de Medicamentos', true),
(28, 'SIAU Y CONSULTA EXTERNA', 'Atención al Usuario y Trabajo Social', true),
(29, 'SISTEMAS E INFORMÁTICA', 'Soporte Técnico y Sistemas', true),
(30, 'MANTENIMIENTO E INFRAESTRUCTURA', 'Mantenimiento Hospitalario e Infraestructura', true),
(31, 'AUDITORÍA MÉDICA', 'Equipo de Auditores Médicos', true),
(32, 'ASESORÍA JURÍDICA', 'Asesoría Legal y Contratación', true),
(33, 'PSICOLOGÍA', 'Equipo de Psicología Clínica y Organizacional', true),
(34, 'NUTRICIÓN Y DIETÉTICA', 'Equipo de Nutrición', true),
(35, 'TRABAJO SOCIAL', 'Equipo de Trabajo Social', true),
(36, 'REHABILITACIÓN', 'Equipo Multidisciplinario de Rehabilitación', true),
(37, 'EPIDEMIOLOGÍA Y SALUD PÚBLICA', 'Equipo de Salud Pública', true),
(38, 'CENTRAL DE CITAS Y ADMISIONES', 'Recepción, Citas y Admisiones', true),
(39, 'MISIONAL ASISTENCIAL', 'Equipo Asistencial Global', true),
(40, 'APOYO DIAGNÓSTICO Y TERAPÉUTICO', 'Apoyo Diagnóstico y Laboratorio/Terapia', true),
(41, 'ADMINISTRATIVO Y FINANCIERO', 'Personal Administrativo', true),
(42, 'ESTRATÉGICO Y GERENCIAL', 'Equipo Estratégico', true),
(43, 'SEGURIDAD DEL PACIENTE FORMATOS Y OTROS', 'Formatos y anexos de Seguridad del Paciente', true)
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion);

-- Mapear automáticamente todos los Cargos institucionales de Clinova/Kawak a sus respectivos Grupos de Distribución
INSERT IGNORE INTO grupo_distribucion_cargos (grupo_id, cargo_id)
SELECT g.id, c.id 
FROM grupos_distribucion g, cargos c
WHERE (g.nombre = 'Clinical House- Todos')
   OR (g.nombre = 'FISIOTERAPIA' AND UPPER(c.nombre) LIKE '%FISIO%')
   OR (g.nombre = 'MEDICINA GENERAL' AND UPPER(c.nombre) LIKE '%MEDIC%')
   OR (g.nombre = 'TERAPIA OCUPACIONAL' AND UPPER(c.nombre) LIKE '%OCUPACIONAL%')
   OR (g.nombre = 'FONOAUDIOLOGIA' AND UPPER(c.nombre) LIKE '%FONOA%')
   OR (g.nombre = 'ODONTOLOGIA' AND UPPER(c.nombre) LIKE '%ODONTO%')
   OR (g.nombre = 'ENFERMERIA' AND (UPPER(c.nombre) LIKE '%ENFERM%' OR UPPER(c.nombre) LIKE '%JEFE%'))
   OR (g.nombre = 'SERVICIO FARMACEUTICO' AND (UPPER(c.nombre) LIKE '%FARMAC%' OR UPPER(c.nombre) LIKE '%REGENTE%'))
   OR (g.nombre LIKE '%ATENCIÓN DOMICILIARIA%' AND (UPPER(c.nombre) LIKE '%DOMICILIAR%' OR UPPER(c.nombre) LIKE '%CONSULTA%'))
   OR (g.nombre = 'GERENCIA' AND (UPPER(c.nombre) LIKE '%GEREN%' OR UPPER(c.nombre) LIKE '%DIRECTOR%'))
   OR (g.nombre = 'GESTIÓN DE CALIDAD' AND (UPPER(c.nombre) LIKE '%CALIDAD%' OR UPPER(c.nombre) LIKE '%AUDITOR%'))
   OR (g.nombre LIKE '%SEGURIDAD DEL PACIENTE%' AND (UPPER(c.nombre) LIKE '%PACIENTE%' OR UPPER(c.nombre) LIKE '%CALIDAD%'))
   OR (g.nombre = 'SEGURIDAD Y SALUD EN EL TRABAJO' AND (UPPER(c.nombre) LIKE '%SST%' OR UPPER(c.nombre) LIKE '%SEGURIDAD%'))
   OR (g.nombre LIKE '%INFRAESTRUCTURA%' AND (UPPER(c.nombre) LIKE '%SISTEMA%' OR UPPER(c.nombre) LIKE '%INFRAESTRUCTURA%' OR UPPER(c.nombre) LIKE '%TECNOLOG%'))
   OR (g.nombre = 'TALENTO HUMANO' AND (UPPER(c.nombre) LIKE '%TALENTO%' OR UPPER(c.nombre) LIKE '%HUMANO%' OR UPPER(c.nombre) LIKE '%NOMINA%'))
   OR (g.nombre LIKE '%FINANCIERA%' AND (UPPER(c.nombre) LIKE '%FINANC%' OR UPPER(c.nombre) LIKE '%CONTAB%'))
   OR (g.nombre LIKE '%FACTURACIÓN%' AND (UPPER(c.nombre) LIKE '%FACTUR%' OR UPPER(c.nombre) LIKE '%CARTERA%'))
   OR (g.nombre LIKE '%COMPRAS%' AND (UPPER(c.nombre) LIKE '%COMPRA%' OR UPPER(c.nombre) LIKE '%ALMACEN%'))
   OR (g.nombre LIKE '%DOCUMENTAL%' AND (UPPER(c.nombre) LIKE '%ARCHIV%' OR UPPER(c.nombre) LIKE '%DOCUMENT%'));
