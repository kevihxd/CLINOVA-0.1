# Clinova - Backend API (Spring Boot 3 + Java 21 + MySQL 8)

Backend y servicios REST API de la plataforma Clinova para IPS Clinical House.

---

## 🛠️ Tecnologías y Requisitos

- **Java**: JDK 21 (Eclipse Temurin, Microsoft JDK o Amazon Corretto 21).
- **IDE**: IntelliJ IDEA (recomendado) o VS Code / Eclipse.
- **Base de Datos**: MySQL 8.0.
- **Maven**: Incluye Maven Wrapper (`mvnw` / `mvnw.cmd`).
- **Docker & Docker Compose** (opcional para levantar MySQL local rápidamente).

---

## 💻 Configuración en un Nuevo PC con IntelliJ IDEA

1. **Clonar el repositorio:**
   ```bash
   git clone https://github.com/kevihxd/CLINOVA-0.1.git Clinova
   cd Clinova
   ```

2. **Abrir en IntelliJ IDEA:**
   - En IntelliJ: `File` ➔ `Open...` ➔ Seleccionar la carpeta `Clinova`.
   - IntelliJ detectará automáticamente el archivo `clinova/pom.xml` y la configuración en `.idea/misc.xml`.
   - Asegurarse de tener seleccionado **JDK 21** en:
     `File` ➔ `Project Structure` ➔ `Project` ➔ `SDK: 21`.

3. **Base de Datos Local (Opciones):**

   ### Opción A: Usar Docker (Rápido y aislado)
   Desde la raíz del repositorio (`Clinova/`):
   ```bash
   docker compose up -d clinova-mysql
   ```
   Esto levantará un contenedor MySQL 8.0 en el puerto `3306` con:
   - **Usuario**: `root`
   - **Contraseña**: `Clinova2026$$SecurePass` (o `Clinova2026`)
   - **Base de datos**: `clinova_db`

   ### Opción B: MySQL local instalado
   Crear la base de datos:
   ```sql
   CREATE DATABASE clinova_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```

4. **Variables y Configuración (`application.properties`):**
   Ubicado en `clinova/src/main/resources/application.properties`.
   Variables de entorno soportadas con valores por defecto:
   - `DB_HOST`: Host de MySQL (default `localhost`).
   - `DB_PORT`: Puerto de MySQL (default `3306`).
   - `DB_NAME`: Nombre de base de datos (default `clinova_db`).
   - `DB_USER`: Usuario de MySQL (default `root`).
   - `DB_PASSWORD`: Contraseña de MySQL.

5. **Ejecutar la Aplicación:**
   - En IntelliJ: Abrir `clinova/src/main/java/com/clinova/ClinovaApplication.java` y hacer clic en **Run (Shift + F10)**.
   - O vía terminal desde `Clinova/clinova`:
     ```powershell
     .\mvnw.cmd spring-boot:run
     ```
   El backend estará disponible en `http://localhost:8080`.

6. **Compilar el archivo `.jar` para producción:**
   ```powershell
   .\mvnw.cmd clean package -DskipTests
   ```
   Genera el archivo en `clinova/target/clinova-0.0.1-SNAPSHOT.jar`.

---

## ⚠️ Reglas Críticas de Despliegue en VPS

- **EL VPS NUNCA ESTÁ CONECTADO A NINGÚN REPOSITORIO GIT**:
  - Jamás ejecutar `git pull` en el VPS.
  - El `.jar` se compila localmente y se transfiere directamente al VPS a la ruta `/opt/clinova/clinova-backend.jar`.
  - Se reinicia el servicio con:
    ```bash
    sudo docker restart clinova-backend
    ```
- **Consultas SQL en el VPS**:
  Se ejecutan siempre dentro del contenedor:
  ```bash
  sudo docker exec -i clinova-mysql mysql -u root -p'Clinova2026' clinova_db -e "SQL_AQUÍ;"
  ```
