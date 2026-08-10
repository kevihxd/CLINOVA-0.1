#!/bin/bash
set -e

echo "======================================================="
echo "🚀 DESPLEGANDO CLINOVA EN VPS (69.197.164.73)"
echo "======================================================="

# 1. Verificar/Instalar Docker y Docker Compose
if ! command -v docker &> /dev/null; then
    echo "Instalando Docker..."
    curl -fsSL https://get.docker.com -o get-docker.sh
    sh get-docker.sh
fi

# Detectar comando de docker-compose
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE="docker-compose"
elif docker compose version &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
else
    echo "Instalando docker-compose..."
    apt-get update -y && apt-get install -y docker-compose || true
    if command -v docker-compose &> /dev/null; then
        DOCKER_COMPOSE="docker-compose"
    else
        DOCKER_COMPOSE="docker compose"
    fi
fi

# 2. Descomprimir uploads si existe y no se ha extraído
if [ ! -d "uploads" ]; then
    if [ -f "uploads.tar" ]; then
        echo "Extrayendo uploads.tar..."
        tar -xf uploads.tar || true
    elif [ -f "uploads.tar.gz" ]; then
        echo "Extrayendo uploads.tar.gz..."
        tar -xzf uploads.tar.gz || tar -xf uploads.tar.gz || true
    elif [ -f "uploads_clinova.tar.gz" ]; then
        echo "Extrayendo uploads_clinova.tar.gz..."
        tar -xzf uploads_clinova.tar.gz || tar -xf uploads_clinova.tar.gz || true
    fi
else
    echo "Carpeta uploads ya existe. Omitiendo extracción."
fi

# 3. Levantar servicios
echo "Levantando MySQL y Backend de Clinova con $DOCKER_COMPOSE..."
$DOCKER_COMPOSE down || true
$DOCKER_COMPOSE up -d

echo "======================================================="
echo "✅ ¡DESPLIEGUE EXITOSO!"
echo "Backend activo en http://127.0.0.1:8080"
echo "======================================================="
