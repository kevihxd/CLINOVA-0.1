# Reglas y Preferencias del Proyecto Clinova

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

## 3. Acceso a MySQL en el VPS
- **Contenedor MySQL:** `clinova-mysql` (imagen `mysql:8.0`)
- **Credenciales:** usuario `root`, contraseña `Clinova2026`
- **Base de datos:** `clinova_db`
- **Patrón para ejecutar SQL en el VPS:**
  ```bash
  sudo docker exec -i clinova-mysql mysql -u root -p'Clinova2026' clinova_db -e "SQL_AQUÍ;"
  ```
- **Nunca** ejecutar comandos SQL directamente en el shell bash del VPS (siempre dentro del contenedor Docker).

## 4. Tipos de Datos y Límite de Fila en MySQL (InnoDB 65.535 Bytes)
- Para evitar el error `1118 (Row size too large)` en la tabla `documentos`, cualquier columna de listas largas (distribución, usuarios, firmas, TRD, observaciones) DEBE definirse con `columnDefinition = "TEXT"` en JPA y `TEXT NULL` en MySQL.
- Los tipos `VARCHAR(500)` o `VARCHAR(1000)` en charset `utf8mb4` consumen hasta 4 bytes por caracter en el registro inline y están prohibidos para campos multilínea o acumulativos.

## 5. Búsqueda Dual de Documentos (ID vs Kawak ID)
- Todo endpoint o servicio que busque un documento individual (`obtenerPorId`, `actualizar`, `nuevaVersion`, `aprobar`, `obsoleto`, `restaurar`) debe buscar tanto por `id` como por `kawakId` (`repository.findById(id).or(() -> repository.findByKawakId(id))`).

## 6. Reglas de Talento Humano y Formatos
- Los usuarios con tipo de vinculación `CONTRATISTA` tienen bloqueada la opción de eliminar soportes de su hoja de vida. Toda eliminación administrativa debe quedar auditada en el log de historial.
- Los cargos institucionales siempre se deben estandarizar y registrar en MAYÚSCULAS (`UPPERCASE`).
- En la creación de formatos o registros, la opción `Archivo (Office, PDF)` y `Edición en línea` son interfaces mutuamente excluyentes en la pantalla 2 del asistente.

## 7. Rutas Locales de Repositorios y Codificación MySQL
- **Repo Frontend activo (Git -> Hostinger):** `C:\Users\HP\Desktop\kawakips-main\kawakips-main` (la carpeta `kawakips-main (1)` NO es repositorio Git).
- **Repo Backend activo (compilación local `.jar`):** `C:\Users\HP\Desktop\Clinova\clinova`.
- **Verificación de esquema antes de SQL:** Antes de sugerir o generar cualquier sentencia SQL para el VPS, confirmar SIEMPRE los nombres exactos de tablas y columnas leyendo las entidades `@Entity`/`@Table` en `C:\Users\HP\Desktop\Clinova\clinova\src\main\java\com\clinova\entity\`.
- **Tabla oficial de historial documental:** `documentos_historial` (NO `historial_documentos`). Para inserciones manuales con fecha histórica específica (ej. Control de Cambios v1), usar `accion = 'CONTROL_CAMBIOS'` y hora `12:00:00` (evita que `DocumentoHistorialService` sobrescriba la fecha con `fecha_aprobacion` y previene desfases por zona horaria UTC-5).
- **UTF-8 en VPS:** Anteponer `SET NAMES utf8mb4;` en comandos SQL que escriban tildes/ñ, y usar decodificación tolerante (`utf-8` → `latin1`/`cp1252`) en scripts Python que consuman salidas de MySQL en el VPS.

## 8. Secuencia de IDs de Documentos (`proximo-id`) — Cero Hardcoding
- **Prohibido quemar IDs** (como `3468` o `3516`) como valores por defecto o fallback en frontend o backend. Mientras la API responde, el frontend debe mostrar `"Calculando..."`.
- **Fuente única de verdad:** El próximo ID debe obtenerse exclusivamente desde el backend (`/api/v1/documentos/proximo-id`) consultando `MAX(GREATEST(COALESCE(id,0), COALESCE(kawak_id,0))) + 1` sobre la tabla completa `documentos` (ya que `/documentos` y `/documentos/obsoletos` excluyen externos, históricos y filtran por permisos).
- **Cero errores silenciosos y sincronización `AUTO_INCREMENT`:** En el backend no se deben silenciar excepciones SQL con `catch (Exception ignored) {}` en cálculos de ID; todo ajuste manual de ID (`cambiar-id` o SQL) debe mantener sincronizado `AUTO_INCREMENT` en MySQL.
- **Aridad en `Promise.all`:** Al agregar o quitar llamadas dentro de `Promise.all`, verificar que las variables desestructuradas coincidan 1:1 con el array de promesas.

## 9. Normalización de Texto, Mojibake y Categorías Documentales
- Toda función de normalización (`normalizeText`) y carga de listas maestras (`cargos`, `usuarios`, `sedes`, `grupos`, `documentos`) debe aplicar `cleanMojibake()` **antes** de eliminar diacríticos (NFD) o construir claves de agrupación.
- El prefijo institucional de políticas es **`-PO-`** (además de `-POL-`). Toda variante (`POLITICA`, `POLÍTICA`, mojibake) debe consolidarse bajo la categoría canónica única `POLÍTICA`.
- En Control de Cambios (`trazabilidadDocHelper.js`), la Versión 1 se omite solo si su comentario es vacío o genérico; si posee un comentario real explícito (como documentación inicial oficial), debe mostrarse.

## 10. Flujo de Diagnóstico Previo
- Cuando el usuario solicite revisar el porqué, estudiar un problema o entregar un plan estructurado sin avanzar hasta confirmación, realizar únicamente inspección de solo lectura (sin editar código, compilar ni hacer push) hasta recibir aprobación explícita.
