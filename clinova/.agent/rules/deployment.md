---
trigger: always_on
---

# Reglas de Despliegue y Arquitectura Clinova

## 1. Arquitectura y Despliegue en VPS y Hosting
- **EL VPS NUNCA ESTÁ CONECTADO A NINGÚN REPOSITORIO GIT**:
  - Jamás sugerir ni ejecutar `git pull` en el VPS bajo ninguna circunstancia.
  - El backend se actualiza y compila localmente generando el archivo `.jar` (`clinova-backend.jar`).
  - Solo se transfiere el archivo `.jar` directamente al VPS (vía SCP / SFTP / FileZilla) a la ruta `/opt/clinova/clinova-backend.jar` y se reinicia el contenedor Docker:
    `sudo docker restart clinova-backend`
  - El repositorio GitHub del backend (`https://github.com/kevihxd/CLINOVA-0.1.git`) es estrictamente para control de versiones (commit y push desde el entorno local).

- **EL FRONTEND SE DESPLIEGA DIRECTAMENTE POR HOSTINGER**:
  - El frontend está conectado al repositorio GitHub (`https://github.com/sistemasipsch/clinova.git`).
  - Al hacer `git commit` y `git push origin main`, Hostinger realiza el despliegue automático directo.
  - Se mantiene generado `dist.tar.gz` en el Escritorio como respaldo si se requiere.

## 2. Convenciones de IDs y Kawak
- Los documentos con `id <= 3458` corresponden a documentos históricos migrados desde Kawak y tienen `kawak_id = id`.
- Los documentos con `id > 3458` son documentos nuevos creados o actualizados.
- Al pasar un documento a OBSOLETO, su ID queda bloqueado para siempre (no se reutiliza ni se reasigna a otro documento).
