-- Flyway Migration V9: Clean and standardize cargo names to match Kawak master catalog

UPDATE cargos SET nombre = 'Terapeuta ocupacional' WHERE UPPER(nombre) LIKE '%TERAPEUTA OCUPACIONAL%';
UPDATE cargos SET nombre = 'Fisioterapeuta' WHERE UPPER(nombre) LIKE '%FISIOTERAP%';
UPDATE cargos SET nombre = 'Fonoaudiólogo(a)' WHERE UPPER(nombre) LIKE '%FONOAUDIOL%';
UPDATE cargos SET nombre = 'Nutricionista dietista' WHERE UPPER(nombre) LIKE '%NUTRICION%';
UPDATE cargos SET nombre = 'Psicólogo(a)' WHERE UPPER(nombre) LIKE '%PSICOL%';
UPDATE cargos SET nombre = 'Trabajador(a) social' WHERE UPPER(nombre) LIKE '%TRABAJADOR%SOCIAL%';
UPDATE cargos SET nombre = 'Médico general' WHERE UPPER(nombre) LIKE '%MEDICO GENERAL%';
UPDATE cargos SET nombre = 'Auxiliar de enfermería' WHERE UPPER(nombre) LIKE '%AUXILIAR%ENFERM%';
UPDATE cargos SET nombre = 'Enfermera(o)' WHERE UPPER(nombre) = 'ENFERMERA' OR UPPER(nombre) = 'ENFERMERO' OR UPPER(nombre) = 'ENFERMERO(A)';
UPDATE cargos SET nombre = 'Auxiliar de Odontología' WHERE UPPER(nombre) LIKE '%AUXILIAR%ODONTO%';
UPDATE cargos SET nombre = 'Odontólogo' WHERE UPPER(nombre) = 'ODONTOLOGO' OR UPPER(nombre) = 'ODONTÓLOGO';
UPDATE cargos SET nombre = 'Regente de Farmacia' WHERE UPPER(nombre) LIKE '%REGENTE%FARMACIA%';
UPDATE cargos SET nombre = 'Auxiliar administrativo de Farmacia' WHERE UPPER(nombre) LIKE '%AUXILIAR%FARMACIA%';

-- Reasociar todos los cargos estandarizados a los 43 Grupos de Distribución de Kawak
INSERT IGNORE INTO grupo_distribucion_cargos (grupo_id, cargo_id)
SELECT g.id, c.id 
FROM grupos_distribucion g, cargos c
WHERE (g.nombre = 'Clinical House- Todos')
   OR (g.nombre = 'TERAPIA OCUPACIONAL' AND UPPER(c.nombre) LIKE '%OCUPACIONAL%')
   OR (g.nombre = 'FISIOTERAPIA' AND UPPER(c.nombre) LIKE '%FISIO%')
   OR (g.nombre = 'FONOAUDIOLOGIA' AND UPPER(c.nombre) LIKE '%FONOA%')
   OR (g.nombre = 'MEDICINA GENERAL' AND UPPER(c.nombre) LIKE '%MEDIC%')
   OR (g.nombre = 'ENFERMERIA' AND (UPPER(c.nombre) LIKE '%ENFERM%' OR UPPER(c.nombre) LIKE '%JEFE%'))
   OR (g.nombre = 'ODONTOLOGIA' AND UPPER(c.nombre) LIKE '%ODONTO%')
   OR (g.nombre = 'SERVICIO FARMACEUTICO' AND (UPPER(c.nombre) LIKE '%FARMAC%' OR UPPER(c.nombre) LIKE '%REGENTE%'))
   OR (g.nombre LIKE '%ATENCIÓN DOMICILIARIA%' AND (UPPER(c.nombre) LIKE '%DOMICILIAR%' OR UPPER(c.nombre) LIKE '%CONSULTA%' OR UPPER(c.nombre) LIKE '%PAD%'))
   OR (g.nombre = 'SEGURIDAD Y SALUD EN EL TRABAJO' AND (UPPER(c.nombre) LIKE '%SST%' OR UPPER(c.nombre) LIKE '%SEGURIDAD%'))
   OR (g.nombre LIKE '%SEGURIDAD DEL PACIENTE%' AND (UPPER(c.nombre) LIKE '%PACIENTE%' OR UPPER(c.nombre) LIKE '%CALIDAD%'))
   OR (g.nombre LIKE '%INFRAESTRUCTURA%' AND (UPPER(c.nombre) LIKE '%SISTEMA%' OR UPPER(c.nombre) LIKE '%INFRAESTRUCTURA%' OR UPPER(c.nombre) LIKE '%TECNOLOG%'))
   OR (g.nombre = 'TALENTO HUMANO' AND (UPPER(c.nombre) LIKE '%TALENTO%' OR UPPER(c.nombre) LIKE '%HUMANO%' OR UPPER(c.nombre) LIKE '%NOMINA%'))
   OR (g.nombre LIKE '%FINANCIERA%' AND (UPPER(c.nombre) LIKE '%FINANC%' OR UPPER(c.nombre) LIKE '%CONTAB%'))
   OR (g.nombre LIKE '%FACTURACIÓN%' AND (UPPER(c.nombre) LIKE '%FACTUR%' OR UPPER(c.nombre) LIKE '%CARTERA%'))
   OR (g.nombre LIKE '%COMPRAS%' AND (UPPER(c.nombre) LIKE '%COMPRA%' OR UPPER(c.nombre) LIKE '%ALMACEN%'))
   OR (g.nombre LIKE '%DOCUMENTAL%' AND (UPPER(c.nombre) LIKE '%ARCHIV%' OR UPPER(c.nombre) LIKE '%DOCUMENT%'));
