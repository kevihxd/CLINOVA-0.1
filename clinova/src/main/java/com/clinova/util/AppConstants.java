package com.clinova.util;

public final class AppConstants {

    private AppConstants() {}

    // Estados documento
    public static final String ESTADO_EN_REVISION = "EN REVISIÓN";
    public static final String ESTADO_VIGENTE = "VIGENTE";
    public static final String ESTADO_VENCIDO = "VENCIDO";
    public static final String ESTADO_A_VENCER = "A VENCER";

    // Estados hoja de vida / empleado
    public static final String ESTADO_ACTIVO = "ACTIVO";
    public static final String ESTADO_INACTIVO = "INACTIVO";
    public static final String ESTADO_RETIRADO = "RETIRADO";

    // Respuestas estándar
    public static final String MSG_SUCCESS = "SUCCESS";
    public static final String MSG_ERROR = "ERROR";
    public static final String MSG_NOT_FOUND = "Registro no encontrado";
    public static final String MSG_CREATED = "Creado exitosamente";
    public static final String MSG_UPDATED = "Actualizado exitosamente";
    public static final String MSG_DELETED = "Eliminado exitosamente";
    public static final String MSG_UNAUTHORIZED = "No tiene permisos para esta operación";

    // Acciones para historial / auditoría
    public static final String ACCION_CREACION = "CREACION";
    public static final String ACCION_MODIFICACION = "MODIFICACION";
    public static final String ACCION_APROBACION = "APROBACION";
    public static final String ACCION_DESCARGA = "DESCARGA";
    public static final String ACCION_ELIMINACION = "ELIMINACION";
    public static final String ACCION_VISUALIZACION = "VISUALIZACION";

    // Roles
    public static final String ROL_ADMIN = "ADMIN";
    public static final String ROL_USER = "USER";

    // Sistema
    public static final String SISTEMA = "Sistema";
}
