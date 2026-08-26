-- Flyway migration V6: Insert/Update missing Hoja de Vida for Belkys Xiomara Rojas Perez (60364649) from Kawak

INSERT INTO clinova_db.hojas_vida (
    cedula, nombres, apellidos, fecha_nacimiento, direccion, telefono, 
    contacto_emergencia, telefono_emergencia, arl, eps, afp, 
    fecha_ingreso, estado, tipo_contrato, email, sede_id, cargo_id
) VALUES (
    '60364649', 'BELKYS XIOMARA', 'ROJAS PEREZ', '1974-11-07', 
    'Conj. Palma Redonda Apto1102 TORRE B, PRODOS DEL ESTE', '3006983206', 
    'JUAN ROJAS', '3153891474', 'Positiva Compañía de Seguros S.A.', 
    'EPS037 - NUEVA EPS S.A - NUEVA EMPRESA PROMOTORA DE SALUD NUEVA EPS S.A', 
    'Administradora Colombiana de Pensiones Colpensiones', '2025-02-12', 
    'Contratado', 'Nomina', 'belrojitas7@hotmail.com', 1, 1
) ON DUPLICATE KEY UPDATE 
    nombres = VALUES(nombres),
    apellidos = VALUES(apellidos),
    fecha_nacimiento = VALUES(fecha_nacimiento),
    direccion = VALUES(direccion),
    telefono = VALUES(telefono),
    contacto_emergencia = VALUES(contacto_emergencia),
    telefono_emergencia = VALUES(telefono_emergencia),
    arl = VALUES(arl),
    eps = VALUES(eps),
    afp = VALUES(afp),
    fecha_ingreso = VALUES(fecha_ingreso),
    estado = VALUES(estado),
    tipo_contrato = VALUES(tipo_contrato),
    email = VALUES(email);
