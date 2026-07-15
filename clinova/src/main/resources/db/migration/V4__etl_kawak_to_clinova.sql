SET FOREIGN_KEY_CHECKS = 0;

INSERT IGNORE INTO clinova_db.sedes (id, nombre)
SELECT SED_ID, SED_NOMBREES
FROM kawak_backup.sed_sede
WHERE SED_ACTIVO = 1;

INSERT IGNORE INTO clinova_db.cargos (id, nombre, reporta_a_id)
SELECT
    car_id,
    CAR_NOMBREES,
    NULLIF(CAR_ID_DEPENDE_DE, 0)
FROM kawak_backup.car_cargo;

INSERT IGNORE INTO clinova_db.personas (
    id,
    numero_documento,
    primer_nombre,
    primer_apellido,
    correo_electronico,
    numero_telefono,
    direccion_residencia,
    fecha_nacimiento,
    tipo_documento
)
SELECT
    u.USU_ID,
    COALESCE(h.hdv_cedula, u.usu_identificacion, CONCAT('DOC-', u.USU_ID)),
    COALESCE(h.hdv_nombres, u.USU_NOMBRE),
    COALESCE(h.hdv_apellidos, u.USU_APELLIDO),
    u.USU_EMAIL,
    h.hdv_telefonos,
    h.hdv_direccion,
    h.hdv_fecha_nac,
    'CC'
FROM kawak_backup.usu_usuario u
         LEFT JOIN kawak_backup.hdv_hoja h ON u.USU_ID_HDV = h.hdv_id
    ON DUPLICATE KEY UPDATE numero_documento = VALUES(numero_documento);

INSERT IGNORE INTO clinova_db.usuarios (
    id,
    persona_id,
    cargo_id,
    username,
    password,
    rol
)
SELECT
    USU_ID,
    USU_ID,
    NULLIF(USU_ID_CARGO, 0),
    USU_LOGIN,
    CONCAT('{MD5}', USU_CLAVE),
    IF(USU_ADMINISTRADOR = 1, 'ADMIN', 'USER')
FROM kawak_backup.usu_usuario
WHERE USU_LOGIN IS NOT NULL AND USU_LOGIN != ''
ON DUPLICATE KEY UPDATE username = VALUES(username);

INSERT IGNORE INTO clinova_db.hojas_vida (
    kawak_id,
    usuario_id,
    cedula,
    nombres,
    apellidos,
    correo_electronico,
    fecha_ingreso,
    telefono,
    direccion_residencia,
    fecha_nacimiento,
    eps,
    arl,
    afp,
    estado
)
SELECT
    h.hdv_id,
    u.USU_ID,
    h.hdv_cedula,
    h.hdv_nombres,
    h.hdv_apellidos,
    u.USU_EMAIL,
    COALESCE(h.hdv_fecha_ingreso, CURRENT_DATE),
    h.hdv_telefonos,
    h.hdv_direccion,
    h.hdv_fecha_nac,
    (SELECT cmb_opciones FROM kawak_backup.cmb_combo WHERE cmb_valor = h.hdv_eps AND cmb_id_lista = 72 LIMIT 1),
    (SELECT cmb_opciones FROM kawak_backup.cmb_combo WHERE cmb_valor = h.hdv_arl AND cmb_id_lista = 73 LIMIT 1),
    (SELECT cmb_opciones FROM kawak_backup.cmb_combo WHERE cmb_valor = h.hdv_afp AND cmb_id_lista = 74 LIMIT 1),
    IF(h.hdv_activo = 1, 'ACTIVO', 'INACTIVO')
FROM kawak_backup.hdv_hoja h
    LEFT JOIN kawak_backup.usu_usuario u ON h.hdv_usu_vinculado = u.USU_ID
WHERE h.hdv_cedula IS NOT NULL
ON DUPLICATE KEY UPDATE cedula = VALUES(cedula);

INSERT IGNORE INTO clinova_db.documentos (
    kawak_id,
    codigo,
    nombre,
    estado,
    version,
    ubicacion,
    ruta_archivo_local,
    tipo,
    proceso,
    sede,
    fecha_elaboracion,
    fecha_revision,
    fecha_aprobacion,
    alcance,
    meses_revision
)
SELECT
    d.doc_id,
    d.doc_codigo,
    d.doc_nombre,
    IF(d.doc_activo = 1, 'VIGENTE', 'OBSOLETO'),
    d.doc_consecutivo,
    d.doc_ubicacion,
    v.ver_archivo,
    COALESCE(t.tip_nombrees, 'Documento'),
    p.pro_nombrees,
    s.SED_NOMBREES,
    DATE_FORMAT(d.doc_fecha_creacion, '%d/%m/%Y'),
    DATE_FORMAT(d.doc_fecha_creacion, '%d/%m/%Y'),
    DATE_FORMAT(d.doc_fecha_creacion, '%d/%m/%Y'),
    IF(d.doc_alcance = 1, 'A toda la organización', 'Al proceso'),
    12
FROM kawak_backup.doc_documento d
         LEFT JOIN kawak_backup.tip_tipo_documento t ON d.doc_id_tipo = t.tip_id
         LEFT JOIN kawak_backup.pro_proceso p ON d.doc_id_proceso = p.pro_id
         LEFT JOIN kawak_backup.sed_sede s ON d.doc_id_sede = s.SED_ID
         LEFT JOIN (
            SELECT ver_id_documento, ver_archivo
            FROM kawak_backup.ver_version_documento v1
            WHERE ver_version = (
                SELECT MAX(ver_version)
                FROM kawak_backup.ver_version_documento v2
                WHERE v1.ver_id_documento = v2.ver_id_documento
            )
         ) v ON d.doc_id = v.ver_id_documento
    ON DUPLICATE KEY UPDATE kawak_id = VALUES(kawak_id);

INSERT IGNORE INTO clinova_db.soportes (
    hoja_vida_id,
    nombre_archivo,
    ruta_archivo,
    tipo_documento,
    fecha_carga,
    tamano,
    estado
)
SELECT
    hv.id,
    COALESCE(hd.hdv_doc_nombre, hd.hdv_doc_archivo),
    hd.hdv_doc_archivo,
    'SOPORTE_MIGRADO',
    CURRENT_TIMESTAMP,
    0,
    'ACTIVO'
FROM kawak_backup.hdv_documento hd
         INNER JOIN clinova_db.hojas_vida hv ON hd.hdv_doc_hoja = hv.kawak_id
WHERE hd.hdv_doc_archivo IS NOT NULL AND hd.hdv_doc_archivo != '';

INSERT IGNORE INTO clinova_db.actas (
    kawak_id,
    titulo,
    contenido_html,
    estado,
    fecha,
    fecha_creacion,
    responsable,
    tipo,
    proceso,
    sede,
    lugar,
    hora_inicio,
    hora_fin
)
SELECT
    d.drv_id,
    COALESCE(d.drv_nombre, 'ACTA SIN TITULO'),
    COALESCE(d.drv_contenido, '<p>Documento migrado sin contenido HTML</p>'),
    IF(d.drv_activo = 1, 'ACTIVO', 'INACTIVO'),
    COALESCE(DATE(d.drv_fecha_hora_inicio), CURRENT_DATE),
    COALESCE(d.drv_fecha_creacion, CURRENT_TIMESTAMP),
    COALESCE(u.USU_NOMBRE, 'SISTEMA'),
    'Acta de Reunión',
    p.pro_nombrees,
    s.SED_NOMBREES,
    d.drv_lugar,
    TIME(d.drv_fecha_hora_inicio),
    TIME(d.drv_fecha_hora_final)
FROM kawak_backup.drv_documento_revision d
    LEFT JOIN kawak_backup.usu_usuario u ON d.drv_id_responsable = u.USU_ID
    LEFT JOIN kawak_backup.pro_proceso p ON d.drv_id_proceso = p.pro_id
    LEFT JOIN kawak_backup.sed_sede s ON d.drv_id_sede = s.SED_ID
    ON DUPLICATE KEY UPDATE kawak_id = VALUES(kawak_id);

INSERT IGNORE INTO clinova_db.incapacidades (
    usuario_id,
    fecha_inicio,
    fecha_fin,
    dias_otorgados,
    observaciones,
    ruta_archivo,
    nombre_archivo,
    estado
)
SELECT
    u.id,
    DATE(a.aus_per_ini),
    DATE(a.aus_per_fin),
    a.aus_dur_dias,
    a.aus_info_adicional,
    ax.axa_archivo,
    COALESCE(ax.axa_nombre, ax.axa_archivo),
    IF(a.aus_activo = 1, 'ACTIVO', 'INACTIVO')
FROM kawak_backup.aus_ausentismo a
    INNER JOIN clinova_db.usuarios u ON a.aus_id_colaborador = u.id
    LEFT JOIN kawak_backup.axa_adjunto_x_ausentismo ax ON a.aus_id = ax.axa_id_aus;

SET FOREIGN_KEY_CHECKS = 1;
