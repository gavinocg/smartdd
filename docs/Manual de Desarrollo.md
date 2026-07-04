# Manual de Desarrollo — SMARTDD

**Versión:** 1.0
**Fecha:** 2026-06-29

---

## Índice

1. [Preparación del servidor Ubuntu 26.04](#1-preparación-del-servidor-ubuntu-2604)
2. [Configuración de Docker y Docker Compose](#2-configuración-de-docker-y-docker-compose)
3. [Configuración de Cloudflare Tunnel](#3-configuración-de-cloudflare-tunnel)
4. [Inicio de la infraestructura Docker](#4-inicio-de-la-infraestructura-docker)
5. [Inicialización del backend (Signaling Server)](#5-inicialización-del-backend-signaling-server)
6. [Verificación de funcionamiento](#6-verificación-de-funcionamiento)
7. [Desarrollo de la app Android — Fase 1: Core](#7-desarrollo-de-la-app-android--fase-1-core)
8. [Desarrollo de la app Android — Fase 2: QR y RA](#8-desarrollo-de-la-app-android--fase-2-qr-y-ra)
9. [Desarrollo de la app Android — Fase 3: Comunicación](#9-desarrollo-de-la-app-android--fase-3-comunicación)
10. [Desarrollo del Admin Panel](#10-desarrollo-del-admin-panel)
11. [Integración de pagos (Google Play Billing)](#11-integración-de-pagos-google-play-billing)
12. [Notificaciones Push (Firebase Cloud Messaging)](#12-notificaciones-push-firebase-cloud-messaging)
13. [Pruebas y depuración](#13-pruebas-y-depuración)
14. [Empaquetado y publicación](#14-empaquetado-y-publicación)
15. [Mantenimiento y monitoreo](#15-mantenimiento-y-monitoreo)

---

### Convenciones de código usadas en este manual

| Convención | Significado |
|---|---|
| `// TODO(nombre): descripción` | Tarea pendiente de implementar, asignada a `nombre` |
| `// ─── ───` | Separador de secciones dentro de un archivo |
| `/** Documentación */` | Comentario de documentación kdoc |
| `suspend fun` | Función asíncrona que debe llamarse desde corrutina |
| `->` | Comunicación unidireccional: emisor → receptor |
| `↔` | Comunicación bidireccional |
| `<...>` | Placeholder a reemplazar con valor real |

---

## 1. Preparación del servidor Ubuntu 26.04

### 1.1 Requisitos mínimos del servidor

| Recurso | Mínimo | Recomendado |
|---|---|---|
| CPU | 2 núcleos | 4 núcleos |
| RAM | 4 GB | 8 GB |
| Disco | 20 GB SSD | 40 GB SSD |
| Internet | 10 Mbps simétrico | 50 Mbps simétrico |
| Puerto UDP | 10000 abierto | Para Jitsi Video Bridge |

### 1.2 Instalación del sistema

```bash
# Actualizar paquetes del sistema
sudo apt update
sudo apt upgrade -y

# Instalar herramientas esenciales
sudo apt install -y \
    curl \
    wget \
    git \
    nano \
    ufw \
    htop \
    net-tools \
    ca-certificates \
    gnupg \
    lsb-release

# Verificar versión de Ubuntu
lsb_release -a
# Debe mostrar: Distributor ID: Ubuntu, Release: 26.04
```

### 1.3 Configurar hostname (opcional)

```bash
sudo hostnamectl set-hostname smartdd-server
```

### 1.4 Configurar firewall (UFW)

```bash
# Habilitar UFW
sudo ufw default deny incoming
sudo ufw default allow outgoing

# Permitir SSH
sudo ufw allow 22/tcp

# Puerto para Jitsi Video Bridge (UDP)
sudo ufw allow 10000/udp

# Si tienes Jitsi configurado con TCP, permitir también:
# sudo ufw allow 443/tcp

# Habilitar
sudo ufw enable

# Verificar estado
sudo ufw status verbose
```

### 1.5 Configurar rangos de Cloudflare (opcional, para mayor seguridad)

Si quieres que solo Cloudflare pueda acceder a tu servidor HTTP/HTTPS:

```bash
# Obtener rangos actualizados de Cloudflare
curl -s https://www.cloudflare.com/ips-v4 > /tmp/cf-ipv4.txt
curl -s https://www.cloudflare.com/ips-v6 > /tmp/cf-ipv6.txt

# Permitir HTTP/HTTPS solo desde Cloudflare
while read ip; do
    sudo ufw allow from $ip to any port 80 proto tcp
    sudo ufw allow from $ip to any port 443 proto tcp
done < /tmp/cf-ipv4.txt
done < /tmp/cf-ipv6.txt
```

### 1.6 Verificar puerto UDP para Jitsi

```bash
# Verificar que el puerto 10000 UDP está accesible
# Desde tu máquina LOCAL:
nc -zvup <server-ip> 10000

# O usando un servicio online:
# https://www.yougetsignal.com/tools/open-ports/
```

---

## 2. Configuración de Docker y Docker Compose

### 2.1 Instalar Docker

```bash
# Método recomendado: script oficial
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Alternativa: instalación manual desde repositorio
# sudo apt install -y docker-ce docker-ce-cli containerd.io

# Verificar instalación
docker --version
# Debe mostrar: Docker version 27.x.x
```

### 2.2 Configurar Docker para usuario no root

```bash
sudo usermod -aG docker $USER

# IMPORTANTE: Cerrar sesión y volver a entrar para que el cambio surta efecto
# O ejecutar:
newgrp docker

# Verificar que funciona sin sudo
docker ps
```

### 2.3 Habilitar Docker al inicio

```bash
sudo systemctl enable docker
sudo systemctl start docker

# Verificar estado
sudo systemctl status docker
```

### 2.4 Instalar Docker Compose

```bash
# Docker Compose v2 viene incluido con Docker 24+
docker compose version
# Debe mostrar: Docker Compose version v2.x.x

# Si no está instalado:
# sudo apt install -y docker-compose-plugin
```

### 2.5 Verificar instalación completa

```bash
# Ejecutar contenedor de prueba
docker run hello-world

# Deberías ver:
# Hello from Docker!
# This message shows that your installation appears to be working correctly.
```

---

## 3. Configuración de Cloudflare Tunnel

### 3.1 Requisitos previos

- Una cuenta en Cloudflare (gratuita)
- Un dominio apuntando a Cloudflare (ej: `smartdd.com`)
- Los DNS del dominio deben estar gestionados por Cloudflare

### 3.2 Instalar cloudflared en tu máquina LOCAL

```bash
# En tu máquina de desarrollo (Windows con WSL, o Linux local)
# Descargar cloudflared
curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 -o cloudflared
chmod +x cloudflared
sudo mv cloudflared /usr/local/bin/

# Verificar
cloudflared version
```

### 3.3 Autenticar cloudflared con Cloudflare

```bash
cloudflared tunnel login

# Esto abre un navegador web. Inicia sesión en Cloudflare y selecciona el dominio.
# Se crea el archivo: ~/.cloudflared/cert.pem
```

### 3.4 Crear el tunnel

```bash
cloudflared tunnel create smartdd

# Output esperado:
# Created tunnel smartdd with id <tunnel-id>
# Credentials file created at: ~/.cloudflared/<tunnel-id>.json

# Guardar el tunnel ID (lo necesitarás después)
# Ejemplo: 123e4567-e89b-12d3-a456-426614174000
```

### 3.5 Configurar DNS en Cloudflare Dashboard

Abre el navegador y ve a Cloudflare Dashboard → Tu dominio → DNS.

Agrega estos 3 registros CNAME:

| Tipo | Nombre | Destino |
|---|---|---|
| CNAME | `api` | `{tunnel-id}.cfargotunnel.com` |
| CNAME | `admin` | `{tunnel-id}.cfargotunnel.com` |
| CNAME | `jitsi` | `{tunnel-id}.cfargotunnel.com` |

Reemplaza `{tunnel-id}` con el ID del paso anterior.

### 3.6 Copiar credenciales al servidor Ubuntu

```bash
# Desde tu máquina LOCAL
scp ~/.cloudflared/<tunnel-id>.json usuario@<server-ip>:~/smartdd/infra/cloudflared/credentials.json

# Renombrar el archivo a credentials.json (ya está configurado en docker-compose)
```

### 3.7 Verificar config.yml

El archivo `infra/cloudflared/config.yml` ya está creado. Debe verse así:

```yaml
tunnel: smartdd
credentials-file: /etc/cloudflared/credentials.json

ingress:
  - hostname: api.smartdd.com
    service: http://nginx:80
  - hostname: admin.smartdd.com
    service: http://nginx:80
  - hostname: jitsi.smartdd.com
    service: http://nginx:80
  - service: http_status:404
```

### 3.8 Probar el tunnel localmente (opcional)

```bash
# Probar que el tunnel funciona apuntando a un servidor local
cloudflared tunnel --config infra/cloudflared/config.yml run

# Deberías ver el tunnel conectado. Deja esto corriendo en una terminal
# y prueba acceder a https://api.smartdd.com desde el navegador
# (debería dar error 502 si nginx no está corriendo, pero el tunnel
# está funcionando si ves el error)
```

---

## 4. Inicio de la infraestructura Docker

### 4.1 Preparar el proyecto en el servidor

```bash
# Conectarse al servidor por SSH
ssh usuario@<server-ip>

# Crear directorio del proyecto
mkdir -p ~/smartdd
cd ~/smartdd
```

### 4.2 Copiar archivos al servidor

Desde tu máquina LOCAL:

```bash
# Método 1: Si tienes el proyecto en una carpeta local
scp -r C:\DEV\smartdd\* usuario@<server-ip>:~/smartdd/

# Método 2: Si usas git (recomendado)
# En tu máquina local:
cd C:\DEV\smartdd
git init
git add .
git commit -m "Initial commit: SRS, infra, backend"
# Luego en el servidor:
# git clone <url-del-repo>
```

### 4.3 Configurar variables de entorno

```bash
cd ~/smartdd/infra

# Copiar el archivo de ejemplo
cp .env.example .env

# Editar con tus valores reales
nano .env
```

Variables a modificar obligatoriamente:

| Variable | Valor | Dónde obtenerlo |
|---|---|---|
| `JWT_SECRET` | String aleatorio seguro | `openssl rand -base64 32` |
| `JWT_REFRESH_SECRET` | Otro string aleatorio | `openssl rand -base64 32` |
| `JICOFO_COMPONENT_SECRET` | String aleatorio | `openssl rand -base64 16` |
| `FCM_SERVER_KEY` | Clave del servidor Firebase | Firebase Console → Cloud Messaging |
| `ADMIN_EMAIL` | Email del admin | El que quieras |
| `ADMIN_PASSWORD` | Contraseña segura | La que quieras |

El resto de variables pueden quedar con los valores por defecto.

### 4.4 Descargar imágenes Docker

```bash
cd ~/smartdd/infra

# Descargar todas las imágenes
docker compose pull

# Esto descarga: postgres:16-alpine, redis:7-alpine, node:22-alpine,
# jitsi/web, jitsi/prosody, jitsi/jicofo, jitsi/jvb,
# nginx:alpine, cloudflare/cloudflared

# Si internet es lento, puede tomar varios minutos la primera vez
```

### 4.5 Iniciar todos los servicios

```bash
# Iniciar en modo detached (background)
docker compose up -d

# Verificar que todos los servicios están corriendo
docker compose ps

# Deberías ver todos los contenedores con estado "Up"
```

### 4.6 Verificar logs de cada servicio

```bash
# Ver todos los logs en tiempo real
docker compose logs -f

# Ver logs de un servicio específico
docker compose logs -f postgres
docker compose logs -f redis
docker compose logs -f signaling
docker compose logs -f nginx
docker compose logs -f jitsi-jvb
docker compose logs -f cloudflared
```

### 4.7 Comandos de diagnóstico

```bash
# Ver el estado de todos los contenedores
docker ps -a

# Ver uso de recursos
docker stats

# Entrar a un contenedor
docker compose exec postgres psql -U smartdd -d smartdd

# Ver red interna
docker network ls
docker network inspect smartdd_smartdd-net
```

### 4.8 Solución de problemas comunes al iniciar

**Problema: `jitsi-jvb` no arranca**
```bash
# Verificar que el puerto UDP 10000 no está ocupado
sudo lsof -i :10000

# Verificar logs
docker compose logs jitsi-jvb

# Posible causa: falta de memoria
# Solución: aumentar RAM del servidor o reducir configuración JVB
```

**Problema: `cloudflared` no conecta**
```bash
docker compose logs cloudflared

# Verificar que el archivo credentials.json existe y es correcto
cat ~/smartdd/infra/cloudflared/credentials.json

# Verificar que el tunnel ID en config.yml coincide
```

**Problema: `postgres` no arranca**
```bash
docker compose logs postgres

# Posible causa: el volumen pgdata está corrupto
# Solución: detener y eliminar volúmenes (CUIDADO: borra datos)
# docker compose down -v
# docker compose up -d
```

---

## 5. Inicialización del backend (Signaling Server)

### 5.1 Instalar Node.js y npm (si desarrollas fuera del contenedor)

```bash
# Instalar Node.js 22 LTS
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt install -y nodejs

# Verificar
node --version  # 22.x.x
npm --version   # 10.x.x
```

### 5.2 Instalar dependencias

```bash
cd ~/smartdd/backend

# Instalar dependencias
npm install

# Esto instala: express, prisma, @prisma/client, bcryptjs, jsonwebtoken,
# ws, ioredis, qrcode, zod, uuid, helmet, cors, compression, etc.
```

### 5.3 Generar Prisma Client

```bash
npx prisma generate

# Esto genera el cliente TypeScript basado en el schema
# Debe mostrar: ✔ Generated Prisma Client
```

### 5.4 Ejecutar migraciones

```bash
# Crear la migración inicial
npx prisma migrate dev --name init

# Esto:
# 1. Crea la carpeta prisma/migrations/
# 2. Ejecuta SQL CREATE TABLE para todos los modelos
# 3. Genera el Prisma Client

# Verificar que las tablas se crearon
docker compose exec postgres psql -U smartdd -d smartdd -c "\dt"
# Deberías ver: User, QrCode, RingSession, Device, UserConfig, AdminLog
```

### 5.5 Poblar la base de datos con datos de prueba

```bash
# Ejecutar seed
npx tsx src/seed.ts

# Esto crea:
# - Admin: admin@smartdd.com / admin123456
# - Usuario test: test@smartdd.com / test123456
# - Configuraciones por defecto
```

### 5.6 Verificar las tablas con datos

```bash
docker compose exec postgres psql -U smartdd -d smartdd -c "SELECT id, name, email, plan, role FROM \"User\";"

# Deberías ver 2 usuarios (admin + test)
```

### 5.7 Iniciar el signaling server (desarrollo)

```bash
# Desde ~/smartdd/backend
npm run dev

# Deberías ver:
# [DB] PostgreSQL conectado
# [Redis] Conexión establecida
# [WS] WebSocket server inicializado
# [Server] SMARTDD Signaling corriendo en puerto 3000
```

### 5.8 Probar endpoints básicos

Abre otra terminal y prueba:

```bash
# Health check
curl http://localhost:3000/api/v1/health
# → { "status": "ok", "timestamp": "..." }

# Login como test
curl -X POST http://localhost:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@smartdd.com","password":"test123456"}'

# Guarda el token que devuelve para las siguientes pruebas
```

### 5.9 Probar el flujo completo (API)

```bash
# 1. Login (guardar token)
TOKEN=$(curl -s -X POST http://localhost:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@smartdd.com","password":"test123456"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

echo $TOKEN

# 2. Generar QR
curl -X POST http://localhost:3000/api/v1/qr \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"lat":19.4326,"lng":-99.1332,"radius":100}'

# 3. Obtener QR por UUID (cambiar UUID por el que devolvió)
curl http://localhost:3000/api/v1/qr/<uuid>

# 4. Validar geolocalización
curl -X POST http://localhost:3000/api/v1/qr/<uuid>/validate \
  -H "Content-Type: application/json" \
  -d '{"lat":19.4327,"lng":-99.1331}'

# 5. Ver configuración
curl http://localhost:3000/api/v1/user/config \
  -H "Authorization: Bearer $TOKEN"

# 6. Ver plan
curl http://localhost:3000/api/v1/user/plan \
  -H "Authorization: Bearer $TOKEN"
```

### 5.10 Probar WebSocket (con wscat)

```bash
# En otra terminal, instalar wscat
npm install -g wscat

# Conectar al WebSocket (con el token del paso anterior)
wscat -c "ws://localhost:3000/ws"

# Una vez conectado, enviar:
{"type":"auth","token":"<TOKEN>"}

# Deberías recibir:
# {"type":"authenticated","userId":"<id>"}
```

---

## 6. Verificación de funcionamiento

### 6.1 Verificar que el tunnel Cloudflare funciona

```bash
# Desde cualquier máquina con internet
curl https://api.smartdd.com/api/v1/health
# → { "status": "ok", "timestamp": "..." }

# Si esto funciona, el tunnel está operativo y nginx está sirviendo
```

### 6.2 Verificar Jitsi Meet

```bash
# Abrir en navegador:
https://jitsi.smartdd.com

# Deberías ver la interfaz web de Jitsi Meet
# (No se usará directamente, pero confirma que Jitsi está funcionando)
```

### 6.3 Verificar Admin Panel

```bash
# Abrir en navegador:
https://admin.smartdd.com

# Deberías ver la página del panel de administración
# (Una vez desarrollado el frontend)
```

### 6.4 Verificar conectividad WebRTC

Para probar que Jitsi Video Bridge funciona:

1. Abre `https://jitsi.smartdd.com` en dos pestañas del navegador
2. En ambas, únete a la misma sala (ej: `test123`)
3. Verifica que el audio y video funcionan entre ambas pestañas

Si el video/audio funciona, el JVB está configurado correctamente.

### 6.5 Verificar que los contenedores se reinician automáticamente

```bash
# Detener un contenedor
docker stop smartdd-signaling

# Esperar 5 segundos
docker ps

# Debería haberse reiniciado automáticamente (restart: unless-stopped)
```

---

## 7. Desarrollo de la app Android — Fase 1: Core

### 7.1 Requisitos del entorno de desarrollo

| Herramienta | Versión mínima |
|---|---|
| Android Studio | Hedgehog (2023.1.1) |
| JDK | 17 |
| Android SDK | API 26 (Android 8.0) |
| Gradle | 8.x |
| Kotlin | 1.9.x |
| Jetpack Compose | 1.7.x |

### 7.2 Crear el proyecto Android

**Opción A: Usar Android Studio**

1. Abre Android Studio → "New Project"
2. Template: "Empty Compose Activity"
3. Nombre: `SmartDD`
4. Package: `com.smartdd.app`
5. Language: Kotlin
6. Minimum SDK: API 26 (Android 8.0)
7. Build configuration: Kotlin DSL (build.gradle.kts)

**Opción B: Manual (recomendado para este proyecto)**

Crea la estructura de carpetas manualmente:

```
android/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/smartdd/app/
│           │   ├── SmartDDApp.kt
│           │   ├── MainActivity.kt
│           │   ├── navigation/
│           │   ├── di/
│           │   ├── data/
│           │   │   ├── remote/
│           │   │   │   ├── api/          # Retrofit interfaces
│           │   │   │   └── websocket/    # WebSocket client
│           │   │   ├── local/
│           │   │   │   ├── db/           # Room database
│           │   │   │   └── preferences/  # EncryptedSharedPreferences
│           │   │   └── repository/
│           │   ├── domain/
│           │   │   ├── model/
│           │   │   └── repository/
│           │   └── presentation/
│           │       ├── auth/
│           │       ├── home/
│           │       ├── qr/
│           │       └── settings/
│           └── res/
│               ├── values/
│               │   ├── strings.xml
│               │   └── themes.xml
│               └── drawable/
├── build.gradle.kts        # Proyecto raíz
├── settings.gradle.kts
└── gradle.properties
```

### 7.3 Configurar build.gradle.kts (proyecto)

```kotlin
// android/build.gradle.kts
plugins {
    id("com.android.application") version "8.7.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("com.google.dagger.hilt.android") version "2.51" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```

### 7.4 Configurar build.gradle.kts (app)

```kotlin
// android/app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    kotlin("kapt")
}

android {
    namespace = "com.smartdd.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smartdd.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // URL del servidor
        buildConfigField("String", "SERVER_URL", "\"https://api.smartdd.com\"")
        buildConfigField("String", "JITSI_DOMAIN", "\"jitsi.smartdd.com\"")
        buildConfigField("String", "WS_URL", "\"wss://api.smartdd.com/ws\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Compose
    val composeVersion = "1.7.0"
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Hilt (DI)
    val hiltVersion = "2.51"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")

    // Camera + QR
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // 3D / AR Simulada
    implementation("io.github.sceneview:sceneview:2.2.2")

    // Jitsi Meet SDK
    implementation("org.jitsi.react:jitsi-meet-sdk:12.1.3")

    // Firebase / FCM
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-messaging")

    // Google Play Billing
    implementation("com.android.billingclient:billing:7.1.1")

    // Almacenamiento seguro
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Room (chat offline)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Permisos
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // Corrutinas
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
```

### 7.5 Modelos de datos (DTOs para la API)

```kotlin
// data/remote/model/AuthModels.kt
package com.smartdd.app.data.remote.model

import com.google.gson.annotations.SerializedName

// ─── Request ────────────────────────────────────────
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RefreshRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

// ─── Response ───────────────────────────────────────
data class RegisterResponse(
    val user: UserDTO,
    val token: String,
    @SerializedName("refreshToken") val refreshToken: String
)

data class LoginResponse(
    val user: UserDTO,
    val token: String,
    @SerializedName("refreshToken") val refreshToken: String
)

data class RefreshResponse(
    val token: String
)

data class UserResponse(
    val user: UserDTO
)

data class UserDTO(
    val id: String,
    val name: String,
    val email: String,
    val plan: String,            // "FREE" | "PRO" | "B2P"
    val role: String,            // "user" | "admin" | "business"
    val active: Boolean?,
    val createdAt: String?,
    val config: UserConfigDTO?
)

data class AuthErrorResponse(
    val error: String,
    val details: List<ValidationDetail>? = null
)

data class ValidationDetail(
    val message: String,
    val path: List<String>?
)
```

```kotlin
// data/remote/model/QRModels.kt
package com.smartdd.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class CreateQRRequest(
    val lat: Double,
    val lng: Double,
    val radius: Int? = 50
)

data class CreateQRResponse(
    val qr: QRDTO
)

data class QRDTO(
    val id: String,
    val uuid: String,
    val lat: Double,
    val lng: Double,
    @SerializedName("radiusMeters") val radiusMeters: Int,
    val active: Boolean,
    val createdAt: String,
    @SerializedName("imageUrl") val imageUrl: String?
)

data class QRListResponse(
    val qrs: List<QRDTO>
)

data class QRDetailResponse(
    val qr: QRDTO
)

data class ValidateQRRequest(
    val lat: Double,
    val lng: Double
)

data class ValidateQRResponse(
    val valid: Boolean,
    val distance: Double,
    @SerializedName("radiusMeters") val radiusMeters: Int,
    val message: String
)
```

```kotlin
// data/remote/model/RingModels.kt
package com.smartdd.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class RingRequest(
    @SerializedName("qrId") val qrId: String,
    @SerializedName("emisorName") val emisorName: String? = null
)

data class RingResponse(
    val session: RingSessionDTO
)

data class RingSessionDTO(
    val id: String,
    @SerializedName("roomId") val roomId: String,
    val status: String,          // "PENDING" | "PREVIEW" | "ACTIVE" | "COMPLETED" | "REJECTED" | "TIMEOUT"
    val mode: String?            // "CHAT" | "AUDIO" | "VIDEO" | null
)

data class RespondRequest(
    @SerializedName("sessionId") val sessionId: String,
    val action: String,          // "accept" | "reject"
    val mode: String?            // "chat" | "audio" | "video" (solo si action=accept)
)

data class RespondResponse(
    val success: Boolean,
    val status: String?,
    val session: RingSessionDTO?
)

data class SessionResponse(
    val session: SessionDetailDTO
)

data class SessionDetailDTO(
    val id: String,
    val uuid: String,
    val qrId: String,
    val emisorId: String,
    val emisorName: String?,
    val receptorId: String,
    val status: String,
    val responseMode: String?,
    val previewStartedAt: String?,
    val respondedAt: String?,
    val createdAt: String,
    val qr: QRSimpleDTO?
)

data class QRSimpleDTO(
    val uuid: String
)
```

```kotlin
// data/remote/model/ConfigModels.kt
package com.smartdd.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class ConfigResponse(
    val config: UserConfigDTO
)

data class UserConfigDTO(
    val id: String?,
    val userId: String?,
    @SerializedName("defaultMode") val defaultMode: String,        // "CHAT" | "AUDIO" | "VIDEO"
    @SerializedName("chatEnabled") val chatEnabled: Boolean,
    @SerializedName("audioEnabled") val audioEnabled: Boolean,
    @SerializedName("videoEnabled") val videoEnabled: Boolean,
    @SerializedName("timeoutSeconds") val timeoutSeconds: Int?
)

data class UpdateConfigRequest(
    @SerializedName("defaultMode") val defaultMode: String? = null,
    @SerializedName("chatEnabled") val chatEnabled: Boolean? = null,
    @SerializedName("audioEnabled") val audioEnabled: Boolean? = null,
    @SerializedName("videoEnabled") val videoEnabled: Boolean? = null,
    @SerializedName("timeoutSeconds") val timeoutSeconds: Int? = null
)
```

```kotlin
// data/remote/model/PlanModels.kt
package com.smartdd.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class PlanResponse(
    val plan: String,
    @SerializedName("qrLimit") val qrLimit: Int,
    @SerializedName("deviceLimit") val deviceLimit: Int,
    @SerializedName("currentQRs") val currentQRs: Int,
    @SerializedName("currentDevices") val currentDevices: Int
)

data class UpgradeRequest(
    @SerializedName("purchaseToken") val purchaseToken: String,
    @SerializedName("productId") val productId: String
)

data class UpgradeResponse(
    val user: UserDTO,
    val message: String
)
```

```kotlin
// data/remote/model/AdminModels.kt
package com.smartdd.app.data.remote.model

data class AdminStatsResponse(
    val totalUsers: Int,
    val freeUsers: Int,
    val proUsers: Int,
    val b2pUsers: Int,
    val todayQRs: Int,
    val activeSessions: Int,
    val totalQRs: Int,
    val totalSessions: Int
)
```

### 7.6 TokenManager con EncryptedSharedPreferences

```kotlin
// data/local/preferences/TokenManager.kt
package com.smartdd.app.data.local.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestión segura de tokens JWT usando EncryptedSharedPreferences.
 * Los datos se cifran con AES256-GCM a nivel de archivo.
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "smartdd_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PLAN = "user_plan"
        private const val KEY_USER_ROLE = "user_role"
    }

    // ─── Tokens ───────────────────────────────────────
    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    // ─── Info de usuario ──────────────────────────────
    fun saveUserInfo(id: String, name: String, email: String, plan: String, role: String = "user") {
        prefs.edit()
            .putString(KEY_USER_ID, id)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_PLAN, plan)
            .putString(KEY_USER_ROLE, role)
            .apply()
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getUserPlan(): String? = prefs.getString(KEY_USER_PLAN, null)
    fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, null)

    // ─── Estado ───────────────────────────────────────
    fun isLoggedIn(): Boolean = getAccessToken() != null && getUserId() != null

    fun isAdmin(): Boolean = getUserRole() == "admin"

    fun clear() {
        prefs.edit().clear().apply()
    }
}
```

### 7.7 Resultado genérico para la capa de datos

```kotlin
// domain/model/Result.kt
package com.smartdd.app.domain.model

/**
 * Tipo sellado para resultados de operaciones.
 * Reemplaza el uso de try-catch en los ViewModels.
 *
 * Uso:
 *   val result = repository.login(email, pass)
 *   when (result) {
 *       is Result.Success -> mostrar pantalla
 *       is Result.Error -> mostrar error
 *   }
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int? = null) : Result<Nothing>()
}
```

### 7.8 Repositorio de autenticación

```kotlin
// data/repository/AuthRepository.kt
package com.smartdd.app.data.repository

import com.smartdd.app.data.local.preferences.TokenManager
import com.smartdd.app.data.remote.api.SmartDDApi
import com.smartdd.app.data.remote.model.*
import com.smartdd.app.domain.model.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: SmartDDApi,
    private val tokenManager: TokenManager
) {
    suspend fun register(name: String, email: String, password: String): Result<RegisterResponse> {
        return try {
            val response = api.register(RegisterRequest(name, email, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenManager.saveTokens(body.token, body.refreshToken)
                tokenManager.saveUserInfo(body.user.id, body.user.name, body.user.email, body.user.plan, body.user.role)
                Result.Success(body)
            } else {
                val errorJson = response.errorBody()?.string()
                val msg = try { /* parse error */ } catch (e: Exception) { "Error de registro" }
                Result.Error(msg ?: "Error desconocido", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión. Verifica tu internet.")
        }
    }

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenManager.saveTokens(body.token, body.refreshToken)
                tokenManager.saveUserInfo(body.user.id, body.user.name, body.user.email, body.user.plan, body.user.role)
                Result.Success(body)
            } else {
                val msg = if (response.code() == 401) "Credenciales inválidas"
                          else "Error del servidor (${response.code()})"
                Result.Error(msg, response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión. Verifica tu internet.")
        }
    }

    suspend fun refreshToken(): Result<String> {
        return try {
            val refreshToken = tokenManager.getRefreshToken()
                ?: return Result.Error("Sesión expirada. Inicia sesión nuevamente.")

            val response = api.refreshToken(RefreshRequest(refreshToken))
            if (response.isSuccessful && response.body() != null) {
                val newToken = response.body()!!.token
                tokenManager.saveTokens(newToken, refreshToken)
                Result.Success(newToken)
            } else {
                tokenManager.clear()
                Result.Error("Sesión expirada. Inicia sesión nuevamente.")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error al renovar sesión")
        }
    }

    fun logout() {
        tokenManager.clear()
    }

    fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()

    fun getCurrentUserInfo() = mapOf(
        "id" to tokenManager.getUserId(),
        "name" to tokenManager.getUserName(),
        "plan" to tokenManager.getUserPlan()
    )
}
```

### 7.9 Repositorio de QR

```kotlin
// data/repository/QRRepository.kt
package com.smartdd.app.data.repository

import com.smartdd.app.data.remote.api.SmartDDApi
import com.smartdd.app.data.remote.model.*
import com.smartdd.app.domain.model.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QRRepository @Inject constructor(
    private val api: SmartDDApi
) {
    suspend fun createQR(lat: Double, lng: Double, radius: Int = 50): Result<CreateQRResponse> {
        return try {
            val response = api.createQR(CreateQRRequest(lat, lng, radius))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                val msg = response.errorBody()?.string() ?: "Error al crear QR"
                Result.Error(msg, response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun listQRs(): Result<QRListResponse> {
        return try {
            val response = api.listQRs()
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Error al listar QRs")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun getQR(uuid: String): Result<QRDetailResponse> {
        return try {
            val response = api.getQR(uuid)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("QR no encontrado", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun validateQR(uuid: String, lat: Double, lng: Double): Result<ValidateQRResponse> {
        return try {
            val response = api.validateQR(uuid, ValidateQRRequest(lat, lng))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Error al validar QR")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun deleteQR(uuid: String): Result<Unit> {
        return try {
            val response = api.deleteQR(uuid)
            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Error al eliminar QR")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }
}
```

### 7.10 Repositorio de Ring/Sesión

```kotlin
// data/repository/RingRepository.kt
package com.smartdd.app.data.repository

import com.smartdd.app.data.remote.api.SmartDDApi
import com.smartdd.app.data.remote.model.*
import com.smartdd.app.domain.model.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RingRepository @Inject constructor(
    private val api: SmartDDApi
) {
    suspend fun ring(qrId: String, emisorName: String? = null): Result<RingResponse> {
        return try {
            val response = api.ring(RingRequest(qrId, emisorName))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Error al enviar timbre", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun respond(sessionId: String, action: String, mode: String? = null): Result<RespondResponse> {
        return try {
            val response = api.respond(RespondRequest(sessionId, action, mode))
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error(response.errorBody()?.string() ?: "Error al responder", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    suspend fun getSession(sessionId: String): Result<SessionResponse> {
        return try {
            val response = api.getSession(sessionId)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("Sesión no encontrada", response.code())
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }
}
```

### 7.11 Interceptor de autenticación OkHttp (refresh automático)

```kotlin
// data/remote/api/AuthInterceptor.kt
package com.smartdd.app.data.remote.api

import com.smartdd.app.data.local.preferences.TokenManager
import com.smartdd.app.data.repository.AuthRepository
import com.smartdd.app.domain.model.Result
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor que:
 * 1. Añade token JWT a todas las peticiones autenticadas
 * 2. Detecta 401 y refresca el token automáticamente
 * 3. Si el refresh falla, limpia la sesión
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository
) : Interceptor {

    companion object {
        // Rutas que no requieren token JWT
        private val PUBLIC_PATHS = listOf(
            "auth/login", "auth/register", "auth/refresh",
            "qr/",  // GET público
            "validate", "health"
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // Saltar rutas públicas
        if (PUBLIC_PATHS.any { path.contains(it) } && request.method == "GET") {
            return chain.proceed(request)
        }

        // Añadir token
        val token = tokenManager.getAccessToken()
        val authenticatedRequest = if (token != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        val response = chain.proceed(authenticatedRequest)

        // Si 401, intentar refresh automático
        if (response.code == 401 && token != null) {
            response.close()

            val newToken = runBlocking {
                when (val result = authRepository.refreshToken()) {
                    is Result.Success -> result.data
                    is Result.Error -> null
                }
            }

            if (newToken != null) {
                return chain.proceed(
                    request.newBuilder()
                        .header("Authorization", "Bearer $newToken")
                        .build()
                )
            }
        }

        return response
    }
}
```

### 7.12 ViewModel de Login

```kotlin
// presentation/auth/LoginViewModel.kt
package com.smartdd.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartdd.app.data.repository.AuthRepository
import com.smartdd.app.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChanged(email: String) {
        _state.value = _state.value.copy(email = email, error = null)
    }

    fun onPasswordChanged(password: String) {
        _state.value = _state.value.copy(password = password, error = null)
    }

    fun login() {
        val current = _state.value
        if (current.email.isBlank()) {
            _state.value = current.copy(error = "Ingresa tu email")
            return
        }
        if (current.password.length < 6) {
            _state.value = current.copy(error = "La contraseña debe tener al menos 6 caracteres")
            return
        }

        viewModelScope.launch {
            _state.value = current.copy(isLoading = true, error = null)

            when (val result = authRepository.login(current.email, current.password)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(isLoading = false, isLoggedIn = true)
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun checkSession() {
        if (authRepository.isLoggedIn()) {
            _state.value = _state.value.copy(isLoggedIn = true)
        }
    }
}
```

### 7.13 ViewModel de Home (Receptor)

```kotlin
// presentation/home/HomeViewModel.kt
package com.smartdd.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartdd.app.data.remote.model.QRDTO
import com.smartdd.app.data.repository.AuthRepository
import com.smartdd.app.data.repository.QRRepository
import com.smartdd.app.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isReceptor: Boolean = false,         // true = dueño de QR, false = visitante
    val qrList: List<QRDTO> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val qrLimit: Int = 1,
    val currentQRCount: Int = 0,
    val userName: String = ""
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val qrRepository: QRRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    fun loadUserInfo() {
        val info = authRepository.getCurrentUserInfo()
        val name = info["name"] as? String ?: ""
        val plan = info["plan"] as? String ?: "FREE"
        _state.value = _state.value.copy(
            userName = name,
            qrLimit = if (plan == "FREE") 1 else Int.MAX_VALUE
        )
    }

    fun loadQRs() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            when (val result = qrRepository.listQRs()) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        qrList = result.data.qrs,
                        isLoading = false,
                        currentQRCount = result.data.qrs.count { it.active }
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun setRole(isReceptor: Boolean) {
        _state.value = _state.value.copy(isReceptor = isReceptor)
        if (isReceptor) {
            loadQRs()
        }
    }

    fun deleteQR(uuid: String) {
        viewModelScope.launch {
            qrRepository.deleteQR(uuid)
            loadQRs() // Refrescar lista
        }
    }
}
```

### 7.14 ViewModel de Configuración del Receptor

```kotlin
// presentation/settings/SettingsViewModel.kt
package com.smartdd.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartdd.app.data.remote.api.SmartDDApi
import com.smartdd.app.data.remote.model.UpdateConfigRequest
import com.smartdd.app.data.remote.model.UserConfigDTO
import com.smartdd.app.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val defaultMode: String = "CHAT",        // "CHAT" | "AUDIO" | "VIDEO"
    val chatEnabled: Boolean = true,
    val audioEnabled: Boolean = true,
    val videoEnabled: Boolean = true,
    val timeoutSeconds: Int = 60,
    val isLoading: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val api: SmartDDApi
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun loadConfig() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val response = api.getConfig()
                if (response.isSuccessful && response.body() != null) {
                    val config = response.body()!!.config
                    _state.value = SettingsUiState(
                        defaultMode = config.defaultMode,
                        chatEnabled = config.chatEnabled,
                        audioEnabled = config.audioEnabled,
                        videoEnabled = config.videoEnabled,
                        timeoutSeconds = config.timeoutSeconds ?: 60
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun setDefaultMode(mode: String) {
        _state.value = _state.value.copy(defaultMode = mode, saved = false)
    }

    fun toggleChat(checked: Boolean) {
        _state.value = _state.value.copy(chatEnabled = checked, saved = false)
        // Validar que al menos una opción esté activa
        if (!checked && !_state.value.audioEnabled && !_state.value.videoEnabled) {
            _state.value = _state.value.copy(error = "Debes tener al menos una opción activa", chatEnabled = true)
        }
    }

    fun toggleAudio(checked: Boolean) {
        _state.value = _state.value.copy(audioEnabled = checked, saved = false)
        if (!checked && !_state.value.chatEnabled && !_state.value.videoEnabled) {
            _state.value = _state.value.copy(error = "Debes tener al menos una opción activa", audioEnabled = true)
        }
    }

    fun toggleVideo(checked: Boolean) {
        _state.value = _state.value.copy(videoEnabled = checked, saved = false)
        if (!checked && !_state.value.chatEnabled && !_state.value.audioEnabled) {
            _state.value = _state.value.copy(error = "Debes tener al menos una opción activa", videoEnabled = true)
        }
    }

    fun save() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val response = api.updateConfig(UpdateConfigRequest(
                    defaultMode = _state.value.defaultMode,
                    chatEnabled = _state.value.chatEnabled,
                    audioEnabled = _state.value.audioEnabled,
                    videoEnabled = _state.value.videoEnabled,
                    timeoutSeconds = _state.value.timeoutSeconds
                ))
                if (response.isSuccessful) {
                    _state.value = _state.value.copy(isLoading = false, saved = true)
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Error al guardar")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
```

### 7.15 ViewModel de Incoming Call (Receptor)

```kotlin
// presentation/ring/receiver/IncomingCallViewModel.kt
package com.smartdd.app.presentation.ring.receiver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartdd.app.data.remote.api.SmartDDApi
import com.smartdd.app.data.remote.model.RespondRequest
import com.smartdd.app.data.remote.model.UserConfigDTO
import com.smartdd.app.data.remote.websocket.WebSocketClient
import com.smartdd.app.data.remote.websocket.WSMessage
import com.smartdd.app.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IncomingCallUiState(
    val sessionId: String = "",
    val roomId: String = "",
    val emisorName: String = "Visitante",
    val previewActive: Boolean = false,      // true mientras el Emisor envía video
    val config: UserConfigDTO? = null,
    val isResponding: Boolean = false,       // true mientras se procesa la respuesta
    val error: String? = null,
    val responseSent: Boolean = false,       // true cuando se respondió
    val responseAction: String? = null,      // "accept" | "reject"
    val responseMode: String? = null         // "chat" | "audio" | "video"
)

@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    private val api: SmartDDApi
) : ViewModel() {

    private val _state = MutableStateFlow(IncomingCallUiState())
    val state: StateFlow<IncomingCallUiState> = _state.asStateFlow()

    fun initialize(sessionId: String, roomId: String, emisorName: String?, webSocket: WebSocketClient) {
        _state.value = _state.value.copy(
            sessionId = sessionId,
            roomId = roomId,
            emisorName = emisorName ?: "Visitante",
            previewActive = true
        )

        // Cargar configuración
        loadConfig()

        // Escuchar eventos del WebSocket
        webSocket.addListener(object : WebSocketClient.Listener {
            override fun onMessage(message: WSMessage) {
                when (message.type) {
                    "ring_cancelled" -> {
                        if (message.data["sessionId"] == sessionId) {
                            _state.value = _state.value.copy(
                                error = "El visitante canceló la solicitud",
                                previewActive = false
                            )
                        }
                    }
                    "ring_timeout" -> {
                        if (message.data["sessionId"] == sessionId) {
                            _state.value = _state.value.copy(
                                error = "La solicitud expiró",
                                previewActive = false
                            )
                        }
                    }
                }
            }

            override fun onConnectionStateChanged(state: WebSocketClient.ConnectionState) {
                // Manejar reconexión
            }
        })
    }

    private fun loadConfig() {
        viewModelScope.launch {
            try {
                val response = api.getConfig()
                if (response.isSuccessful && response.body() != null) {
                    _state.value = _state.value.copy(config = response.body()!!.config)
                }
            } catch (e: Exception) {
                // Usar config por defecto
            }
        }
    }

    fun respond(webSocket: WebSocketClient, action: String, mode: String? = null) {
        if (_state.value.isResponding) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isResponding = true, error = null)
            try {
                val response = api.respond(RespondRequest(
                    sessionId = _state.value.sessionId,
                    action = action,
                    mode = mode
                ))
                if (response.isSuccessful) {
                    _state.value = _state.value.copy(
                        isResponding = false,
                        responseSent = true,
                        responseAction = action,
                        responseMode = mode
                    )
                } else {
                    _state.value = _state.value.copy(
                        isResponding = false,
                        error = "Error al enviar respuesta"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isResponding = false,
                    error = e.message
                )
            }
        }
    }
}
```

### 7.16 Pantalla de Login (Compose UI)

```kotlin
// presentation/auth/LoginScreen.kt
package com.smartdd.app.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current

    // Redirigir si ya está logueado
    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) onLoginSuccess()
    }

    // Verificar sesión existente
    LaunchedEffect(Unit) {
        viewModel.checkSession()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo / Título
            Text(
                text = "SMARTDD",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Smart Ding Dong",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Email
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChanged,
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChanged,
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.login()
                    }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading
            )

            // Error
            if (state.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón Login
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.login()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Iniciar sesión", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Registro
            TextButton(onClick = onRegisterClick) {
                Text("¿No tienes cuenta? Regístrate")
            }
        }
    }
}
```

### 7.17 MainActivity con manejo de navegación y permisos

```kotlin
// MainActivity.kt
package com.smartdd.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.smartdd.app.navigation.SmartDDNavGraph
import com.smartdd.app.ui.theme.SmartDDTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permisos básicos al inicio
        requestInitialPermissions()

        setContent {
            SmartDDTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    SmartDDNavGraph(navController = navController)
                }
            }
        }
    }

    private fun requestInitialPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS
        )
        ActivityCompat.requestPermissions(this, permissions, 100)
    }
}
```
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.VIBRATE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <application
        android:name=".SmartDDApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="SMARTDD"
        android:theme="@style/Theme.SmartDD">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".data.remote.fcm.SmartDDFirebaseService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

### 7.18 Implementar la clase Application con Hilt

```kotlin
// SmartDDApp.kt
package com.smartdd.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartDDApp : Application()
```

### 7.19 Configurar Retrofit (API client)

```kotlin
// data/remote/api/ApiClient.kt
package com.smartdd.app.data.remote.api

import com.smartdd.app.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(): Interceptor = Interceptor { chain ->
        val request = chain.request()

        // Aquí se puede añadir el token JWT automáticamente
        // val token = TokenManager.getToken()
        // if (token != null) {
        //     request = request.newBuilder()
        //         .addHeader("Authorization", "Bearer $token")
        //         .build()
        // }
        chain.proceed(request)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.SERVER_URL + "/api/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): SmartDDApi {
        return retrofit.create(SmartDDApi::class.java)
    }
}
```

### 7.20 Definir la interfaz de la API

```kotlin
// data/remote/api/SmartDDApi.kt
package com.smartdd.app.data.remote.api

import com.smartdd.app.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

interface SmartDDApi {

    // ─── Auth ────────────────────────────────────────
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): Response<RegisterResponse>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body body: RefreshRequest): Response<RefreshResponse>

    @GET("auth/me")
    suspend fun getMe(): Response<UserResponse>

    // ─── QR ──────────────────────────────────────────
    @POST("qr")
    suspend fun createQR(@Body body: CreateQRRequest): Response<CreateQRResponse>

    @GET("qr")
    suspend fun listQRs(): Response<QRListResponse>

    @GET("qr/{uuid}")
    suspend fun getQR(@Path("uuid") uuid: String): Response<QRDetailResponse>

    @POST("qr/{uuid}/validate")
    suspend fun validateQR(
        @Path("uuid") uuid: String,
        @Body body: ValidateQRRequest
    ): Response<ValidateQRResponse>

    @DELETE("qr/{uuid}")
    suspend fun deleteQR(@Path("uuid") uuid: String): Response<Unit>

    // ─── Ring ────────────────────────────────────────
    @POST("ring")
    suspend fun ring(@Body body: RingRequest): Response<RingResponse>

    @POST("ring/respond")
    suspend fun respond(@Body body: RespondRequest): Response<RespondResponse>

    @GET("ring/session/{id}")
    suspend fun getSession(@Path("id") id: String): Response<SessionResponse>

    // ─── Configuración ───────────────────────────────
    @GET("user/config")
    suspend fun getConfig(): Response<ConfigResponse>

    @PUT("user/config")
    suspend fun updateConfig(@Body body: UpdateConfigRequest): Response<ConfigResponse>

    // ─── Plan ────────────────────────────────────────
    @GET("user/plan")
    suspend fun getPlan(): Response<PlanResponse>

    @POST("user/upgrade")
    suspend fun upgradePlan(@Body body: UpgradeRequest): Response<UpgradeResponse>
}
```

### 7.21 WebSocket client

```kotlin
// data/remote/websocket/WebSocketClient.kt
package com.smartdd.app.data.remote.websocket

import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketClient @Inject constructor(
    private val gson: Gson
) {
    private var webSocket: WebSocket? = null
    private var listeners = mutableListOf<WebSocketListener>()
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.SECONDS) // Sin timeout para WebSocket
        .build()

    interface Listener {
        fun onMessage(message: WSMessage)
        fun onConnectionStateChanged(state: ConnectionState)
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING
    }

    fun connect(token: String) {
        val request = Request.Builder()
            .url("wss://api.smartdd.com/ws")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WS", "Conectado")
                // Autenticar
                send(WSMessage("auth", mapOf("token" to token)))
                notifyState(ConnectionState.CONNECTED)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val message = gson.fromJson(text, WSMessage::class.java)
                listeners.forEach { it.onMessage(message) }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                notifyState(ConnectionState.DISCONNECTED)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WS", "Error: ${t.message}")
                notifyState(ConnectionState.RECONNECTING)
                // Reintentar después de 5 segundos
                reconnect(token)
            }
        })
    }

    private fun reconnect(token: String) {
        Thread.sleep(5000)
        connect(token)
    }

    fun send(message: WSMessage) {
        webSocket?.send(gson.toJson(message))
    }

    fun addListener(listener: Listener) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun disconnect() {
        webSocket?.close(1000, "Cliente cerró conexión")
        webSocket = null
    }

    private fun notifyState(state: ConnectionState) {
        listeners.forEach { it.onConnectionStateChanged(state) }
    }
}

data class WSMessage(
    val type: String,
    val data: Map<String, Any?> = emptyMap()
)
```

### 7.22 Estructura de navegación (NavGraph)

```kotlin
// navigation/NavGraph.kt
package com.smartdd.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object HomeReceptor : Screen("home_receptor")
    object HomeEmisor : Screen("home_emisor")
    object GenerateQR : Screen("generate_qr")
    object Scanner : Screen("scanner")
    object ARButton : Screen("ar_button/{qrId}") {
        fun createRoute(qrId: String) = "ar_button/$qrId"
    }
    object WaitingRing : Screen("waiting_ring/{sessionId}") {
        fun createRoute(sessionId: String) = "waiting_ring/$sessionId"
    }
    object IncomingCall : Screen("incoming_call/{sessionId}") {
        fun createRoute(sessionId: String) = "incoming_call/$sessionId"
    }
    object Chat : Screen("chat/{sessionId}") {
        fun createRoute(sessionId: String) = "chat/$sessionId"
    }
    object AudioCall : Screen("audio_call/{sessionId}") {
        fun createRoute(sessionId: String) = "audio_call/$sessionId"
    }
    object VideoCall : Screen("video_call/{sessionId}") {
        fun createRoute(sessionId: String) = "video_call/$sessionId"
    }
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object Upgrade : Screen("upgrade")
}

@Composable
fun SmartDDNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) { /* SplashScreen(navController) */ }
        composable(Screen.Onboarding.route) { /* OnboardingScreen(navController) */ }
        composable(Screen.Login.route) { /* LoginScreen(navController) */ }
        composable(Screen.Register.route) { /* RegisterScreen(navController) */ }
        composable(Screen.HomeReceptor.route) { /* HomeReceptorScreen(navController) */ }
        composable(Screen.HomeEmisor.route) { /* HomeEmisorScreen(navController) */ }
        composable(Screen.GenerateQR.route) { /* GenerateQRScreen(navController) */ }
        composable(Screen.Scanner.route) { /* ScannerScreen(navController) */ }
        composable(Screen.ARButton.route) { /* ARButtonScreen(navController) */ }
        composable(Screen.WaitingRing.route) { /* WaitingRingScreen(navController) */ }
        composable(Screen.IncomingCall.route) { /* IncomingCallScreen(navController) */ }
        composable(Screen.Chat.route) { /* ChatScreen(navController) */ }
        composable(Screen.AudioCall.route) { /* AudioCallScreen(navController) */ }
        composable(Screen.VideoCall.route) { /* VideoCallScreen(navController) */ }
        composable(Screen.Settings.route) { /* SettingsScreen(navController) */ }
        composable(Screen.Profile.route) { /* ProfileScreen(navController) */ }
        composable(Screen.Upgrade.route) { /* UpgradeScreen(navController) */ }
    }
}
```

---

## 8. Desarrollo de la app Android — Fase 2: QR y RA

### 8.1 Implementar escáner QR con ML Kit

```kotlin
// presentation/qr/scanner/ScannerScreen.kt
package com.smartdd.app.presentation.qr.scanner

import android.Manifest
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    onQRScanned: (String) -> Unit
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
        if (!locationPermission.status.isGranted) {
            locationPermission.launchPermissionRequest()
        }
    }

    if (cameraPermission.status.isGranted) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    // Preview
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    // Image analysis para QR
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val scanner = BarcodeScanning.getClient()
                    val executor = Executors.newSingleThreadExecutor()

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val inputImage = InputImage.fromMediaImage(
                                mediaImage, imageProxy.imageInfo.rotationDegrees
                            )

                            scanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        if (barcode.valueType == Barcode.TYPE_URL ||
                                            barcode.valueType == Barcode.TYPE_TEXT) {
                                            val qrData = barcode.rawValue ?: continue
                                            imageProxy.close()
                                            onQRScanned(qrData)
                                            return@addOnSuccessListener
                                        }
                                    }
                                    imageProxy.close()
                                }
                                .addOnFailureListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    // Seleccionar cámara trasera
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            ctx as androidx.lifecycle.LifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )
    } else {
        // Mostrar mensaje solicitando permiso
    }
}
```

### 8.2 Matemáticas de posicionamiento del botón 3D sobre el QR

El núcleo de la RA simulada es calcular dónde dibujar el botón 3D en pantalla basándose en la posición del QR detectado. Aquí está la lógica matemática detallada:

```
Input:
  - boundingBox del QR detectado por ML Kit (en coordenadas del análisis de imagen)
  - resolution del ImageAnalysis (ej: 1280x720)
  - dimensiones del PreviewView en pantalla (ancho, alto en píxeles)
  - rotación de la cámara (0°, 90°, 180°, 270°)

Cálculo:
  1. Convertir coordenadas del análisis de imagen a coordenadas de pantalla
     considerando la rotación de la cámara y el aspect ratio

  2. El centro del boundingBox → posición (x, y) del botón 3D
     centerX = (left + right) / 2
     centerY = (top + bottom) / 2

  3. El ancho del boundingBox → escala del botón 3D
     scale = boxWidth / viewWidth * 3 (factor 3x para que el botón sea más grande que el QR)

  4. Aplicar transformación de coordenadas según rotación:
     - Rotación 0°:   x = centerX * (viewWidth / analysisWidth)
                       y = centerY * (viewHeight / analysisHeight)
     - Rotación 90°:  x = centerY * (viewWidth / analysisHeight)
                       y = (analysisWidth - centerX) * (viewHeight / analysisWidth)
     - Rotación 270°: x = (analysisHeight - centerY) * (viewWidth / analysisHeight)
                       y = centerX * (viewHeight / analysisWidth)
```

Implementación en Kotlin:

```kotlin
// presentation/qr/ar/CoordinateMapper.kt
package com.smartdd.app.presentation.qr.ar

import android.graphics.Rect
import android.util.Size

/**
 * Mapea coordenadas del análisis de imagen de ML Kit
 * a coordenadas de pantalla para SceneView.
 *
 * ML Kit devuelve boundingBox en coordenadas del frame de la cámara
 * (rotado según imageInfo.rotationDegrees).
 * Necesitamos convertirlas a coordenadas de la vista en pantalla.
 */
object CoordinateMapper {

    /**
     * @param barcodeRect Rect del QR en coordenadas ML Kit
     * @param analysisSize Resolución del ImageAnalysis (ej: Size(1280, 720))
     * @param viewSize Tamaño del PreviewView en pantalla (píxeles)
     * @param rotation Grados de rotación de la imagen (0, 90, 180, 270)
     * @return Centro (x, y) y escala para SceneView
     */
    fun mapQRPosition(
        barcodeRect: Rect,
        analysisSize: Size,
        viewSize: Size,
        rotation: Int
    ): QRPosition {

        // Centro del QR en coordenadas del frame
        val centerX = (barcodeRect.left + barcodeRect.right) / 2f
        val centerY = (barcodeRect.top + barcodeRect.bottom) / 2f
        val boxWidth = barcodeRect.width().toFloat()

        // Escala base: el botón 3D será 3x el tamaño del QR en pantalla
        val baseScale = (boxWidth / analysisSize.width.toFloat()) * 3f

        // Coordenadas normalizadas (0..1) en el frame de la cámara
        val nx = centerX / analysisSize.width.toFloat()
        val ny = centerY / analysisSize.height.toFloat()

        // Aplicar rotación para obtener coordenadas en pantalla
        val (sx, sy) = when (rotation) {
            0 -> Pair(nx, ny)
            90 -> Pair(ny, 1f - nx)
            180 -> Pair(1f - nx, 1f - ny)
            270 -> Pair(1f - ny, nx)
            else -> Pair(nx, ny)
        }

        // Mapear a píxeles de pantalla (considerando aspect ratio)
        val viewX = sx * viewSize.width.toFloat()
        val viewY = sy * viewSize.height.toFloat()

        // Escala final: el botón mantiene proporción respecto al ancho de pantalla
        val scale = baseScale * (viewSize.width.toFloat() / 1080f)

        return QRPosition(
            x = viewX,
            y = viewY,
            scale = scale.coerceIn(0.3f, 1.5f)  // Limitar escala mín/máx
        )
    }
}

data class QRPosition(
    val x: Float,     // Centro X en píxeles de pantalla
    val y: Float,     // Centro Y en píxeles de pantalla
    val scale: Float  // Escala del modelo 3D (1.0 = tamaño original)
)
```

### 8.3 Implementar RA simulada con SceneView (botón 3D)

```kotlin
// presentation/qr/ar/ARButtonScreen.kt
package com.smartdd.app.presentation.qr.ar

import android.Manifest
import android.util.Size
import android.widget.FrameLayout
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.github.sceneview.SceneView
import io.github.sceneview.light.Light
import io.github.sceneview.node.Node
import io.github.sceneview.removing.removeChildren
import java.util.concurrent.Executors

/**
 * Pantalla que muestra el preview de cámara con el botón 3D
 * superpuesto sobre el código QR detectado.
 *
 * Flujo:
 * 1. ML Kit detecta QR y devuelve boundingBox
 * 2. CoordinateMapper convierte a coordenadas de pantalla
 * 3. SceneView renderiza modelo 3D en esa posición
 * 4. Si el QR se mueve, el botón 3D lo sigue
 * 5. Si el QR desaparece, el botón 3D se oculta
 * 6. Al presionar el botón 3D → onButtonPressed()
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ARButtonScreen(
    qrUuid: String,
    onButtonPressed: () -> Unit,
    onError: (String) -> Unit
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current

    // Estado del QR detectado
    var qrPosition by remember { mutableStateOf<QRPosition?>(null) }
    var isDetecting by remember { mutableStateOf(true) }

    // Referencias a SceneView (para manipular el modelo 3D)
    var sceneViewRef by remember { mutableStateOf<SceneView?>(null) }
    var buttonModelNode by remember { mutableStateOf<Node?>(null) }

    // Iniciar detección de QR al montar
    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    if (cameraPermission.status.isGranted) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                // Contenedor para superponer SceneView sobre CameraX
                val container = FrameLayout(ctx)

                // ─── 1. Preview de cámara ─────────────────────
                val previewView = PreviewView(ctx)
                previewView.layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                container.addView(previewView)

                // ─── 2. SceneView para el modelo 3D ───────────
                val sceneView = SceneView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    // Fondo transparente para ver la cámara detrás
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
                container.addView(sceneView)
                sceneViewRef = sceneView

                // ─── 3. Configurar cámara y ML Kit ────────────
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    // Preview
                    val preview = Preview.Builder().build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)

                    // ImageAnalysis para QR
                    val analysisSize = Size(1280, 720)
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(analysisSize)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val scanner = BarcodeScanning.getClient()
                    val executor = Executors.newSingleThreadExecutor()

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        if (!isDetecting) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val mediaImage = imageProxy.image ?: run {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val inputImage = InputImage.fromMediaImage(
                            mediaImage, imageProxy.imageInfo.rotationDegrees
                        )

                        scanner.process(inputImage)
                            .addOnSuccessListener { barcodes ->
                                if (barcodes.isNotEmpty()) {
                                    val barcode = barcodes[0]
                                    val boundingBox = barcode.boundingBox ?: run {
                                        imageProxy.close()
                                        return@addOnSuccessListener
                                    }

                                    // Obtener tamaño de la vista
                                    val viewWidth = previewView.width
                                    val viewHeight = previewView.height
                                    if (viewWidth > 0 && viewHeight > 0) {
                                        val pos = CoordinateMapper.mapQRPosition(
                                            barcodeRect = boundingBox,
                                            analysisSize = analysisSize,
                                            viewSize = Size(viewWidth, viewHeight),
                                            rotation = imageProxy.imageInfo.rotationDegrees
                                        )
                                        qrPosition = pos
                                    }
                                } else {
                                    // QR no detectado en este frame
                                    // No actualizar posición (mantener última conocida)
                                    // Si pasan varios frames sin detección, ocultar
                                }
                                imageProxy.close()
                            }
                            .addOnFailureListener {
                                imageProxy.close()
                            }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            ctx as androidx.lifecycle.LifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        onError("Error al iniciar cámara: ${e.message}")
                    }
                }, ContextCompat.getMainExecutor(ctx))

                container
            }
        )

        // ─── 4. Actualizar posición del modelo 3D ────────────
        LaunchedEffect(qrPosition) {
            val pos = qrPosition ?: return@LaunchedEffect
            val sceneView = sceneViewRef ?: return@LaunchedEffect

            // Cargar modelo si no está cargado
            if (buttonModelNode == null) {
                val node = Node()
                node.loadModelGlbAsync(
                    context = context,
                    glbFileLocation = "models/boton_timbre.glb",  // assets/models/boton_timbre.glb
                    lifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current.lifecycle,
                    onSuccess = { loadedNode ->
                        buttonModelNode = loadedNode
                        sceneView.addChild(loadedNode)
                    }
                )
            }

            // Posicionar el modelo 3D en coordenadas normalizadas de SceneView
            // SceneView usa coordenadas [-1, 1] para vista 2D, o coordenadas 3D
            // Convertir de píxeles a coordenadas normalizadas de SceneView
            buttonModelNode?.position = org.joml.Vector3f(
                (pos.x / sceneView.width.toFloat()) * 2f - 1f,
                -(pos.y / sceneView.height.toFloat()) * 2f + 1f,
                -2f  // Profundidad: ligeramente frente a la cámara
            )

            // Ajustar escala
            buttonModelNode?.scale = org.joml.Vector3f(pos.scale, pos.scale, pos.scale)
        }
    } else {
        // Mostrar mensaje solicitando permiso de cámara
    }

    // Botón flotante "Cancelar" (opcional)
}

/**
 * Cargar el modelo 3D del botón timbre.
 * El archivo debe estar en: android/app/src/main/assets/models/boton_timbre.glb
 */
private fun Node.loadButtonModel(context: android.content.Context) {
    loadModelGlbAsync(
        context = context,
        glbFileLocation = "models/boton_timbre.glb",
        lifecycle = androidx.lifecycle.LifecycleOwner {}.lifecycle
    )
}
```

### 8.4 Modelo 3D del botón timbre — Especificación técnica

El modelo 3D debe ser un archivo GLB (GLTF binario) con las siguientes características:

```
boton_timbre.glb
├── Formato: GLTF 2.0 Binary (.glb)
├── Malla:
│   ├── Cuerpo: cilindro achatado (diámetro 1.0, altura 0.15)
│   ├── Detalle: aro brillante alrededor, icono de campana/timbre en el centro
│   └── Polígonos: < 5000 (optimizado para móvil)
├── Textura:
│   ├── Resolución: 512x512 PNG
│   ├── Color base: #D4AF37 (dorado) con gradiente
│   └── PBR: metálico (roughness 0.3, metalness 0.8)
├── Animaciones:
│   ├── idle (loop):
│   │   ├── Duración: 2s
│   │   ├── Escala: 1.0 ↔ 1.03 (pulsación suave)
│   │   └── Brillo: emisivo oscila ±20%
│   └── press (trigger):
│       ├── Duración: 0.4s
│       ├── Escala: 1.0 → 0.85 (hundimiento)
│       ├── Brillo: flash blanco al inicio
│       └── Rebote: escala 0.85 → 1.02 → 1.0 (efecto elástico)
└── Tamaño: < 500KB

Exportación recomendada desde Blender:
- Archivo → Export → glTF 2.0 (.glb)
- Opciones: +Y Up, Include: Selected Objects, Transform: Y Forward
- Animaciones: Include All Actions
```

### 8.5 ViewModel del botón AR (con manejo de estado)

```kotlin
// presentation/qr/ar/ARButtonViewModel.kt
package com.smartdd.app.presentation.qr.ar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartdd.app.data.remote.model.ValidateQRResponse
import com.smartdd.app.data.repository.QRRepository
import com.smartdd.app.data.repository.RingRepository
import com.smartdd.app.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ARButtonUiState(
    val qrUuid: String = "",
    val qrValidated: Boolean = false,
    val qrValidationMessage: String = "Escanea un código QR",
    val distance: Double = 0.0,
    val isRinging: Boolean = false,
    val ringSuccess: Boolean = false,
    val sessionId: String? = null,
    val roomId: String? = null,
    val error: String? = null,
    val buttonPressed: Boolean = false,  // true por 0.5s para animación press
    val emisorName: String = ""
)

@HiltViewModel
class ARButtonViewModel @Inject constructor(
    private val qrRepository: QRRepository,
    private val ringRepository: RingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ARButtonUiState())
    val state: StateFlow<ARButtonUiState> = _state.asStateFlow()

    fun setQRUuid(uuid: String) {
        _state.value = _state.value.copy(qrUuid = uuid)
    }

    fun validateQR(lat: Double, lng: Double) {
        val uuid = _state.value.qrUuid
        if (uuid.isBlank()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(error = null)

            when (val result = qrRepository.validateQR(uuid, lat, lng)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        qrValidated = result.data.valid,
                        qrValidationMessage = result.data.message,
                        distance = result.data.distance
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        error = result.message,
                        qrValidated = false
                    )
                }
            }
        }
    }

    fun ring(emisorName: String = "Visitante") {
        if (!_state.value.qrValidated) return

        // Animación: presionar el botón 3D
        _state.value = _state.value.copy(buttonPressed = true)

        viewModelScope.launch {
            _state.value = _state.value.copy(isRinging = true, error = null)

            when (val result = ringRepository.ring(_state.value.qrUuid, emisorName)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        isRinging = false,
                        ringSuccess = true,
                        sessionId = result.data.session.id,
                        roomId = result.data.session.roomId
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        isRinging = false,
                        buttonPressed = false,
                        error = result.message
                    )
                }
            }
        }

        // Resetear animación después de 0.5s
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            _state.value = _state.value.copy(buttonPressed = false)
        }
    }

    fun setEmisorName(name: String) {
        _state.value = _state.value.copy(emisorName = name)
    }
}
```

### 8.6 Manejo de detección múltiple y estabilidad

Para evitar que el botón 3D parpadee cuando el QR se detecta/piere, implementar un simple filtro de estabilidad:

```kotlin
// En el analyzer de ML Kit
private var lastDetectionTime = 0L
private var consecutiveMisses = 0
private val STABILITY_THRESHOLD_MS = 200L  // 200ms sin detección = perder tracking
private val MAX_MISSES = 10                // 10 frames sin detección = ocultar

// En onSuccessListener:
if (barcodes.isNotEmpty()) {
    consecutiveMisses = 0
    lastDetectionTime = System.currentTimeMillis()
    actualizar posición...
} else {
    consecutiveMisses++
    if (consecutiveMisses > MAX_MISSES) {
        ocultar botón 3D
    }
}

// En el Composable:
LaunchedEffect(qrPosition) {
    // Smooth interpolation: mover el botón gradualmente hacia la nueva posición
    // usando animación con easing en lugar de teletransportación
}
```

---

## 9. Desarrollo de la app Android — Fase 3: Comunicación

### 9.1 Flujo de renegociación Jitsi (preview → audio/video)

El corazón técnico de SMARTDD es la transición del preview unidireccional a la comunicación completa. Aquí está el flujo detallado:

```
FASE 1: PREVIEW (Emisor → Receptor, unidireccional)
┌──────────────────────────────────────────────┐
│ Emisor: join room con video=ON, audio=OFF    │
│         La cámara frontal stream AL SERVIDOR │
│ Receptor: join room con video=OFF, audio=OFF │
│         Recibe stream del Emisor (ve preview)│
└──────────────────────────────────────────────┘
         │
         │ Receptor responde (Chat / Audio / Video / Rechazar)
         ▼
┌──────────────────────────────────────────────┐
│ FASE 2: RESPUESTA                             │
├──────────────────────────────────────────────┤
│ CHAT:                                        │
│   1. POST /api/v1/respond {action:accept,    │
│      mode:chat}                              │
│   2. Ambos: leave room Jitsi                 │
│   3. Ambos: abren interfaz de chat nativa    │
│   4. WebSocket: {type:"chat_message", ...}   │
│                                              │
│ AUDIO:                                       │
│   1. POST /api/v1/respond {action:accept,    │
│      mode:audio}                             │
│   2. Emisor: setAudioMuted(false),           │
│      setVideoMuted(true) [apagar cámara]     │
│   3. Receptor: setAudioMuted(false)          │
│      [video ya está OFF]                     │
│   4. Ambos: comunicación de voz bidireccional│
│      PERMANECEN EN LA MISMA SALA JITSI       │
│                                              │
│ AUDIO+VIDEO:                                 │
│   1. POST /api/v1/respond {action:accept,    │
│      mode:video}                             │
│   2. Emisor: setAudioMuted(false),           │
│      setVideoMuted(false) (ya está ON)       │
│   3. Receptor: setAudioMuted(false),         │
│      setVideoMuted(false)                    │
│   4. Ambos: comunicación completa bidirec.   │
│      PERMANECEN EN LA MISMA SALA JITSI       │
│                                              │
│ RECHAZAR:                                    │
│   1. POST /api/v1/respond {action:reject}    │
│   2. Ambos: leave room Jitsi                 │
│   3. Emisor: ve "Solicitud rechazada"        │
└──────────────────────────────────────────────┘
```

**Nota técnica importante:** No se necesita crear una nueva sala Jitsi al responder. Tanto el preview como la comunicación completa ocurren en la **misma sala Jitsi**. La diferencia es qué streams de audio/video están activos (muted/unmuted). Esto evita reconexiones y latencia.

### 9.2 Servicio de control de Jitsi (singleton)

Para manejar el estado de Jitsi y la renegociación de forma centralizada:

```kotlin
// data/remote/jitsi/JitsiManager.kt
package com.smartdd.app.data.remote.jitsi

import android.content.Context
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.jitsi.meet.sdk.JitsiMeetView
import org.jitsi.meet.sdk.JitsiMeetViewListener
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager singleton para controlar la sesión Jitsi.
 * Permite:
 * - Unirse a sala con configuración inicial (video/audio muted)
 * - Cambiar mute de audio/video en runtime (renegociación)
 * - Abandonar sala
 * - Escuchar eventos de la conferencia
 */
@Singleton
class JitsiManager @Inject constructor() {

    private var jitsiView: JitsiMeetView? = null
    private var currentRoomId: String? = null

    // Callback para eventos de la conferencia
    var onConferenceJoined: (() -> Unit)? = null
    var onConferenceTerminated: (() -> Unit)? = null
    var onParticipantJoined: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    /**
     * Unirse a una sala Jitsi con configuración inicial.
     *
     * @param context Contexto de Android
     * @param roomId UUID de la sala (generado por el servidor)
     * @param videoMuted true = cámara apagada al entrar
     * @param audioMuted true = micrófono apagado al entrar
     * @param cameraFront true = usar cámara frontal
     */
    fun joinRoom(
        context: Context,
        roomId: String,
        videoMuted: Boolean = false,
        audioMuted: Boolean = false,
        cameraFront: Boolean = true
    ): JitsiMeetView {
        currentRoomId = roomId

        val view = JitsiMeetView(context)

        // Configurar opciones de la reunión
        val options = JitsiMeetConferenceOptions.Builder()
            .setServerURL(URL("https://jitsi.smartdd.com"))
            .setRoom(roomId)
            .setVideoMuted(videoMuted)      // Jitsi usa lógica invertida: true = muted
            .setAudioMuted(audioMuted)
            .setFeatureFlag("chat.enabled", false)
            .setFeatureFlag("invite.enabled", false)
            .setFeatureFlag("raise-hand.enabled", false)
            .setFeatureFlag("recording.enabled", false)
            .setFeatureFlag("live-streaming.enabled", false)
            .setFeatureFlag("toolbox.alwaysVisible", false)
            .setFeatureFlag("filmstrip.enabled", false)  // Ocultar miniaturas
            .setFeatureFlag("pip.enabled", false)
            .setFeatureFlag("welcomepage.enabled", false)
            .setFeatureFlag("resolution", 360)            // Calidad 360p para preview
            .build()

        // Configurar listener
        view.listener = object : JitsiMeetViewListener {
            override fun onConferenceJoined(data: MutableMap<String, Any>?) {
                onConferenceJoined?.invoke()
            }

            override fun onConferenceTerminated(data: MutableMap<String, Any>?) {
                currentRoomId = null
                onConferenceTerminated?.invoke()
            }

            override fun onConferenceWillJoin(data: MutableMap<String, Any>?) {}

            override fun onParticipantJoined(data: MutableMap<String, Any>?) {
                val participantId = data?.get("participantId") as? String
                if (participantId != null) {
                    onParticipantJoined?.invoke(participantId)
                }
            }

            override fun onParticipantLeft(data: MutableMap<String, Any>?) {}

            override fun onAudioMutedChanged(data: MutableMap<String, Any>?) {
                // Manejar cambio de mute de audio
            }

            override fun onVideoMutedChanged(data: MutableMap<String, Any>?) {
                // Manejar cambio de mute de video
            }

            override fun onAudioModeChanged(data: MutableMap<String, Any>?) {}

            override fun onEndpointTextMessageReceived(data: MutableMap<String, Any>?) {}

            override fun onScreenShareToggled(data: MutableMap<String, Any>?) {}

            override fun onChatMessageReceived(data: MutableMap<String, Any>?) {}

            override fun onChatToggled(data: MutableMap<String, Any>?) {}

            override fun onLargeVideoIdChanged(data: MutableMap<String, Any>?) {}

            override fun onVideoConferenceStarted() {}

            override fun onVideoConferenceStopped() {}

            override fun onReadyToClose() {
                leaveRoom()
            }
        }

        view.join(options)
        jitsiView = view
        return view
    }

    /**
     * RENEGOCIACIÓN: Cambiar estado del micrófono.
     * Se llama cuando el Receptor acepta con Audio o Video.
     *
     * @param muted true = apagar micrófono
     */
    fun setAudioMuted(muted: Boolean) {
        jitsiView?.setAudioMuted(muted)
    }

    /**
     * RENEGOCIACIÓN: Cambiar estado de la cámara.
     * Se llama cuando el Receptor acepta con Video (ambos activan cámara).
     *
     * @param muted true = apagar cámara
     */
    fun setVideoMuted(muted: Boolean) {
        jitsiView?.setVideoMuted(muted)
    }

    /**
     * Cambiar entre cámara frontal y trasera.
     */
    fun switchCamera() {
        jitsiView?.setCameraSwitch()
    }

    /**
     * Abandonar la sala actual.
     */
    fun leaveRoom() {
        jitsiView?.leave()
        jitsiView?.dispose()
        jitsiView = null
        currentRoomId = null
    }

    fun isInRoom(): Boolean = currentRoomId != null
    fun getCurrentRoomId(): String? = currentRoomId
}
```

### 9.3 Control de Jitsi en Compose con renegociación

```kotlin
// presentation/call/JitsiController.kt
package com.smartdd.app.presentation.call

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.smartdd.app.data.remote.jitsi.JitsiManager
import javax.inject.Inject

/**
 * Estado de la comunicación Jitsi.
 * Este estado se usa para decidir qué interfaz mostrar
 * y cómo controlar la renegociación.
 */
enum class JitsiCallState {
    IDLE,              // Sin llamada activa
    PREVIEW_EMISOR,    // Emisor: enviando video, esperando respuesta
    PREVIEW_RECEPTOR,  // Receptor: recibiendo video, va a responder
    AUDIO_CALL,        // Llamada de audio bidireccional
    VIDEO_CALL         // Videollamada bidireccional
}

/**
 * Composable que renderiza el JitsiMeetView.
 * Se adapta automáticamente al estado de la llamada.
 *
 * @param roomId UUID de la sala Jitsi
 * @param callState Estado actual de la comunicación
 * @param jitsiManager Instancia del manager
 * @param onEvent Callback para eventos de la conferencia
 */
@Composable
fun JitsiCallView(
    roomId: String,
    callState: JitsiCallState,
    jitsiManager: JitsiManager,
    onEvent: (JitsiCallEvent) -> Unit = {}
) {
    val context = LocalContext.current

    LaunchedEffect(callState) {
        when (callState) {
            JitsiCallState.PREVIEW_EMISOR -> {
                // Emisor: cámara frontal ON, micrófono OFF
                jitsiManager.joinRoom(
                    context = context,
                    roomId = roomId,
                    videoMuted = false,
                    audioMuted = true,
                    cameraFront = true
                )
            }
            JitsiCallState.PREVIEW_RECEPTOR -> {
                // Receptor: cámara OFF, micrófono OFF (solo recibe)
                jitsiManager.joinRoom(
                    context = context,
                    roomId = roomId,
                    videoMuted = true,
                    audioMuted = true,
                    cameraFront = false
                )
            }
            JitsiCallState.AUDIO_CALL -> {
                // RENEGOCIACIÓN: acti var micrófono de ambos
                jitsiManager.setAudioMuted(false)
                jitsiManager.setVideoMuted(true)  // Apagar cámara si estaba
            }
            JitsiCallState.VIDEO_CALL -> {
                // RENEGOCIACIÓN: activar cámara + micrófono de ambos
                jitsiManager.setAudioMuted(false)
                jitsiManager.setVideoMuted(false)
            }
            JitsiCallState.IDLE -> {
                jitsiManager.leaveRoom()
            }
        }
    }

    // Renderizar el JitsiMeetView si hay sala activa
    if (callState != JitsiCallState.IDLE) {
        AndroidView(
            factory = { ctx ->
                // El JitsiMeetView ya fue creado en joinRoom
                // Si no existe, crearlo aquí
                jitsiManager.joinRoom(
                    context = ctx,
                    roomId = roomId,
                    videoMuted = callState == JitsiCallState.PREVIEW_RECEPTOR,
                    audioMuted = callState != JitsiCallState.AUDIO_CALL &&
                                 callState != JitsiCallState.VIDEO_CALL,
                    cameraFront = callState == JitsiCallState.PREVIEW_EMISOR
                )
            },
            modifier = androidx.compose.ui.Modifier.fillMaxSize()
        )
    }

    // Eventos del JitsiManager
    DisposableEffect(Unit) {
        jitsiManager.onConferenceTerminated = {
            onEvent(JitsiCallEvent.TERMINATED)
        }
        jitsiManager.onConferenceJoined = {
            onEvent(JitsiCallEvent.JOINED)
        }
        onDispose { }
    }
}

sealed class JitsiCallEvent {
    object JOINED : JitsiCallEvent()
    object TERMINATED : JitsiCallEvent()
    data class ERROR(val message: String) : JitsiCallEvent()
}
```

### 9.4 Transiciones de estado entre preview y comunicación

Este es el diagrama de estados que maneja la lógica de comunicación en el ViewModel:

```
                  ┌─────────────┐
                  │   IDLE      │
                  └──────┬──────┘
                         │
              Emisor presiona timbre
                         │
                         ▼
              ┌─────────────────────┐
              │ PREVIEW_EMISOR      │ ← Emisor: cámara frontal ON
              │ (esperando respuesta)│   WebSocket espera respuesta
              └──────────┬──────────┘
                         │
              Receptor recibe notificación
                         │
                         ▼
              ┌─────────────────────┐
              │ PREVIEW_RECEPTOR    │ ← Receptor: ve preview del Emisor
              │ (decidiendo)        │   Botones: Chat | Audio | Video | Rechazar
              └──┬───────┬──────┬───┘
                 │       │      │
          Responde Chat  Audio  Video
                 │       │      │
                 ▼       ▼      ▼
          ┌────────┐ ┌──────┐ ┌────────┐
          │ CHAT   │ │AUDIO │ │ VIDEO  │ ← Misma sala Jitsi
          │(leave  │ │(mute │ │(mute   │
          │ Jitsi) │ │audio)│ │audio+  │
          │        │ │      │ │video)  │
          └────────┘ └──────┘ └────────┘
                 │       │      │
                 └───────┴──────┘
                         │
                   Cualquiera cuelga
                         │
                         ▼
                    ┌─────────┐
                    │  IDLE   │
                    └─────────┘
```

### 9.5 ViewModel de llamada (manejo de estados)

```kotlin
// presentation/call/CallViewModel.kt
package com.smartdd.app.presentation.call

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartdd.app.data.remote.model.UserConfigDTO
import com.smartdd.app.data.remote.websocket.WebSocketClient
import com.smartdd.app.data.remote.websocket.WSMessage
import com.smartdd.app.data.repository.RingRepository
import com.smartdd.app.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CallUiState(
    val sessionId: String = "",
    val roomId: String = "",
    val jitsiState: JitsiCallState = JitsiCallState.IDLE,
    val isReceptor: Boolean = false,     // true = Receptor, false = Emisor
    val emisorName: String = "Visitante",
    val receptorConfig: UserConfigDTO? = null,
    val isResponding: Boolean = false,
    val responseSent: Boolean = false,
    val responseAction: String? = null,
    val responseMode: String? = null,    // "chat" | "audio" | "video"
    val error: String? = null,
    val callDuration: Long = 0L          // segundos de llamada activa
)

@HiltViewModel
class CallViewModel @Inject constructor(
    private val ringRepository: RingRepository,
    private val webSocketClient: WebSocketClient
) : ViewModel() {

    private val _state = MutableStateFlow(CallUiState())
    val state: StateFlow<CallUiState> = _state.asStateFlow()

    fun initializeAsEmisor(sessionId: String, roomId: String, emisorName: String) {
        _state.value = _state.value.copy(
            sessionId = sessionId,
            roomId = roomId,
            emisorName = emisorName,
            isReceptor = false,
            jitsiState = JitsiCallState.PREVIEW_EMISOR
        )

        // Escuchar WebSocket para la respuesta del Receptor
        webSocketClient.addListener(object : WebSocketClient.Listener {
            override fun onMessage(message: WSMessage) {
                when (message.type) {
                    "ring_answered" -> {
                        if (message.data["sessionId"] == sessionId) {
                            val mode = message.data["mode"] as? String ?: return
                            handleReceptorResponse(mode)
                        }
                    }
                    "ring_rejected" -> {
                        if (message.data["sessionId"] == sessionId) {
                            _state.value = _state.value.copy(
                                error = "Solicitud rechazada",
                                jitsiState = JitsiCallState.IDLE
                            )
                        }
                    }
                    "ring_timeout" -> {
                        if (message.data["sessionId"] == sessionId) {
                            _state.value = _state.value.copy(
                                error = "Tiempo de espera agotado",
                                jitsiState = JitsiCallState.IDLE
                            )
                        }
                    }
                }
            }

            override fun onConnectionStateChanged(state: WebSocketClient.ConnectionState) {}
        })
    }

    fun initializeAsReceptor(sessionId: String, roomId: String, emisorName: String, config: UserConfigDTO?) {
        _state.value = _state.value.copy(
            sessionId = sessionId,
            roomId = roomId,
            emisorName = emisorName,
            isReceptor = true,
            receptorConfig = config,
            jitsiState = JitsiCallState.PREVIEW_RECEPTOR
        )
    }

    /**
     * Transición: Preview → Chat
     * Ambos abandonan Jitsi y abren chat nativo.
     */
    fun respondWithChat() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isResponding = true)
            // Enviar respuesta al servidor
            ringRepository.respond(_state.value.sessionId, "accept", "chat")
            // Cambiar estado
            _state.value = _state.value.copy(
                isResponding = false,
                responseSent = true,
                responseAction = "accept",
                responseMode = "chat",
                jitsiState = JitsiCallState.IDLE  // Abandonar Jitsi
            )
        }
    }

    /**
     * Transición: Preview → Audio
     * Ambos permanecen en Jitsi, solo se activa micrófono.
     */
    fun respondWithAudio() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isResponding = true)
            ringRepository.respond(_state.value.sessionId, "accept", "audio")
            _state.value = _state.value.copy(
                isResponding = false,
                responseSent = true,
                responseAction = "accept",
                responseMode = "audio",
                jitsiState = JitsiCallState.AUDIO_CALL  // Renegociar
            )
        }
    }

    /**
     * Transición: Preview → Video
     * Ambos permanecen en Jitsi, se activa cámara y micrófono.
     */
    fun respondWithVideo() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isResponding = true)
            ringRepository.respond(_state.value.sessionId, "accept", "video")
            _state.value = _state.value.copy(
                isResponding = false,
                responseSent = true,
                responseAction = "accept",
                responseMode = "video",
                jitsiState = JitsiCallState.VIDEO_CALL  // Renegociar
            )
        }
    }

    /**
     * Receptor rechaza la solicitud.
     */
    fun reject() {
        viewModelScope.launch {
            ringRepository.respond(_state.value.sessionId, "reject")
            _state.value = _state.value.copy(
                responseSent = true,
                responseAction = "reject",
                jitsiState = JitsiCallState.IDLE
            )
        }
    }

    /**
     * Emisor cancela la solicitud mientras espera.
     */
    fun cancelRing() {
        webSocketClient.send(WSMessage("cancel_ring", mapOf(
            "sessionId" to _state.value.sessionId
        )))
        _state.value = _state.value.copy(jitsiState = JitsiCallState.IDLE)
    }

    /**
     * Finalizar llamada activa (cuelga).
     */
    fun hangUp() {
        _state.value = _state.value.copy(jitsiState = JitsiCallState.IDLE)
    }

    private fun handleReceptorResponse(mode: String) {
        val newState = when (mode) {
            "chat" -> JitsiCallState.IDLE       // Abandonar Jitsi, abrir chat
            "audio" -> JitsiCallState.AUDIO_CALL
            "video" -> JitsiCallState.VIDEO_CALL
            else -> JitsiCallState.PREVIEW_EMISOR
        }
        _state.value = _state.value.copy(
            responseSent = true,
            responseAction = "accept",
            responseMode = mode,
            jitsiState = newState
        )
    }

    override fun onCleared() {
        super.onCleared()
        // Asegurar que se abandona la sala al salir del ViewModel
        if (_state.value.jitsiState != JitsiCallState.IDLE) {
            _state.value = _state.value.copy(jitsiState = JitsiCallState.IDLE)
        }
    }
}
```
import java.net.URL

@Composable
fun JitsiMeetViewComposable(
    roomId: String,
    serverUrl: String = "https://jitsi.smartdd.com",
    videoMuted: Boolean = false,
    audioMuted: Boolean = false,
    onConferenceTerminated: () -> Unit = {}
) {
    val context = LocalContext.current
    var jitsiView by remember { mutableStateOf<JitsiMeetView?>(null) }

    AndroidView(
        factory = { ctx ->
            val view = JitsiMeetView(ctx)

            val options = JitsiMeetConferenceOptions.Builder()
                .setServerURL(URL(serverUrl))
                .setRoom(roomId)
                .setVideoMuted(videoMuted)
                .setAudioMuted(audioMuted)
                .setFeatureFlag("chat.enabled", false)
                .setFeatureFlag("invite.enabled", false)
                .build()

            view.join(options)

            jitsiView = view
            view
        },
        modifier = androidx.compose.ui.Modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            jitsiView?.leave()
            jitsiView?.dispose()
        }
    }
}
```

### 9.6 Room Database para persistencia de chat

El chat de texto se almacena localmente (nunca en el servidor por privacidad). Usamos Room para persistir los mensajes.

```kotlin
// data/local/db/ChatDatabase.kt
package com.smartdd.app.data.local.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ─── Entidad ────────────────────────────────────────
@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["sessionId"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: String,
    val text: String,
    val isMine: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// ─── DAO ────────────────────────────────────────────
@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessages(sessionId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM chat_messages WHERE timestamp < :before")
    suspend fun deleteOldMessages(before: Long)
}

// ─── Database ───────────────────────────────────────
@Database(
    entities = [ChatMessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SmartDDDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: SmartDDDatabase? = null

        fun getInstance(context: Context): SmartDDDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartDDDatabase::class.java,
                    "smartdd_chat.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ─── Repositorio de chat ───────────────────────────
class ChatRepository(private val dao: ChatMessageDao) {
    fun getMessages(sessionId: String): Flow<List<ChatMessageEntity>> =
        dao.getMessages(sessionId)

    suspend fun saveMessage(sessionId: String, text: String, isMine: Boolean) {
        dao.insertMessage(ChatMessageEntity(sessionId = sessionId, text = text, isMine = isMine))
    }

    suspend fun clearSession(sessionId: String) = dao.deleteSession(sessionId)

    suspend fun cleanOldMessages(days: Int = 30) {
        dao.deleteOldMessages(System.currentTimeMillis() - (days * 86400000L))
    }
}

// ─── DI Module ──────────────────────────────────────
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartDDDatabase =
        SmartDDDatabase.getInstance(context)

    @Provides
    fun provideChatDao(db: SmartDDDatabase): ChatMessageDao = db.chatMessageDao()

    @Provides
    @Singleton
    fun provideChatRepository(dao: ChatMessageDao): ChatRepository = ChatRepository(dao)
}
```

### 9.7 ChatScreen con persistencia Room

```kotlin
// presentation/call/chat/ChatScreen.kt
package com.smartdd.app.presentation.call.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartdd.app.data.local.db.ChatRepository
import com.smartdd.app.data.remote.websocket.WebSocketClient
import com.smartdd.app.data.remote.websocket.WSMessage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String,
    webSocketClient: WebSocketClient,
    chatRepository: ChatRepository,
    onClose: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Mensajes desde Room (Flow)
    val messages by chatRepository.getMessages(sessionId)
        .collectAsState(initial = emptyList())

    // Listener WebSocket para mensajes entrantes
    LaunchedEffect(webSocketClient) {
        webSocketClient.addListener(object : WebSocketClient.Listener {
            override fun onMessage(message: WSMessage) {
                if (message.type == "chat_message" && message.data["sessionId"] == sessionId) {
                    val text = message.data["text"] as? String ?: return
                    scope.launch { chatRepository.saveMessage(sessionId, text, isMine = false) }
                }
            }
            override fun onConnectionStateChanged(state: WebSocketClient.ConnectionState) {}
        })
    }

    // Auto-scroll al último mensaje
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Chat") },
            navigationIcon = { TextButton(onClick = onClose) { Text("Cerrar") } }
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubble(text = msg.text, isMine = msg.isMine, timestamp = msg.timestamp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe un mensaje...") },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (inputText.isNotBlank()) {
                    val text = inputText
                    webSocketClient.send(WSMessage("chat_message", mapOf("sessionId" to sessionId, "text" to text)))
                    scope.launch { chatRepository.saveMessage(sessionId, text, isMine = true) }
                    inputText = ""
                }
            }) { Text("Enviar") }
        }
    }
}

@Composable
fun ChatBubble(text: String, isMine: Boolean, timestamp: Long) {
    val timeStr = remember(timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            ),
            color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(text = text, color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                Text(
                    text = timeStr,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
```
                topEnd = 12.dp,
                bottomStart = if (message.isMine) 12.dp else 0.dp,
                bottomEnd = if (message.isMine) 0.dp else 12.dp
            ),
            color = if (message.isMine) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 1.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(12.dp),
                color = if (message.isMine) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
```

---

## 10. Desarrollo del Admin Panel

### 10.1 Crear proyecto React + Vite

```bash
# En tu máquina de desarrollo
mkdir -p ~/smartdd/admin-panel
cd ~/smartdd/admin-panel

# Crear proyecto Vite
npm create vite@latest . -- --template react-ts

# Instalar dependencias
npm install
npm install react-router-dom @tanstack/react-query axios recharts
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init -p
```

### 10.2 Configurar Docker para build

```dockerfile
# admin-panel/Dockerfile
FROM node:22-alpine AS builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### 10.3 Config nginx para admin

```nginx
# admin-panel/nginx.conf
server {
    listen 80;
    server_name _;

    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    gzip on;
    gzip_types text/css application/javascript application/json;
}
```

### 10.4 Estructura del Admin Panel

```
admin-panel/
├── src/
│   ├── api/
│   │   └── client.ts          # Axios client con JWT
│   ├── components/
│   │   ├── Layout.tsx         # Sidebar + header
│   │   ├── UserTable.tsx      # Tabla de usuarios
│   │   └── StatsCards.tsx     # Cards de estadísticas
│   ├── pages/
│   │   ├── Dashboard.tsx      # Stats generales
│   │   ├── Users.tsx          # Lista de usuarios
│   │   ├── UserDetail.tsx     # Detalle de usuario
│   │   ├── QRs.tsx            # QRs globales
│   │   ├── Settings.tsx       # Config global
│   │   └── Login.tsx          # Login del admin
│   ├── hooks/
│   │   └── useAuth.ts
│   ├── types/
│   │   └── index.ts
│   └── App.tsx
└── Dockerfile
```

---

## 11. Integración de pagos (Google Play Billing)

### 11.1 Configurar productos en Google Play Console

1. Ve a Google Play Console → Tu app → Monetizar → Productos
2. Crear suscripción: `smartdd_pro_monthly` — $3.99/mes
3. Crear suscripción: `smartdd_pro_yearly` — $39.99/año

### 11.2 Implementar BillingClient en Android

```kotlin
// data/billing/BillingManager.kt
package com.smartdd.app.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    private val context: Context
) {
    private lateinit var billingClient: BillingClient

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState

    sealed class PurchaseState {
        object Idle : PurchaseState()
        object Loading : PurchaseState()
        object Success : PurchaseState()
        data class Error(val message: String) : PurchaseState()
    }

    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    // Procesar compra
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Consultar compras existentes
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Reintentar conexión
            }
        })
    }

    fun launchBillingFlow(activity: Activity, productId: String) {
        _purchaseState.value = PurchaseState.Loading

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { _, productDetailsList ->
            if (productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken

                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .apply { offerToken?.let { setOfferToken(it) } }
                                .build()
                        )
                    )
                    .build()

                billingClient.launchBillingFlow(activity, flowParams)
            } else {
                _purchaseState.value = PurchaseState.Error("Producto no encontrado")
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Enviar purchaseToken al servidor para verificar
            verifyPurchaseOnServer(purchase)
        }
    }

    private fun verifyPurchaseOnServer(purchase: Purchase) {
        // POST /api/v1/user/upgrade { purchaseToken, productId }
        _purchaseState.value = PurchaseState.Success
    }

    private fun queryPurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { _, purchases ->
            for (purchase in purchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    // Usuario ya tiene suscripción activa
                }
            }
        }
    }
}
```

---

## 12. Notificaciones Push (Firebase Cloud Messaging)

### 12.1 Configurar Firebase Console

1. Ve a [Firebase Console](https://console.firebase.google.com)
2. Crea un proyecto o selecciona el existente
3. Android: Agrega app con package `com.smartdd.app`
4. Descarga `google-services.json` y colócalo en `android/app/`
5. Copia la **Clave del servidor** (Server Key) → `FCM_SERVER_KEY` en `.env`

### 12.2 Implementar FirebaseMessagingService

```kotlin
// data/remote/fcm/SmartDDFirebaseService.kt
package com.smartdd.app.data.remote.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smartdd.app.MainActivity
import com.smartdd.app.R
import com.smartdd.app.data.remote.api.SmartDDApi
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class SmartDDFirebaseService : FirebaseMessagingService() {

    @Inject
    lateinit var api: SmartDDApi

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Enviar token al servidor
        registerDeviceToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val type = message.data["type"] ?: return
        val sessionId = message.data["sessionId"] ?: return
        val roomId = message.data["roomId"] ?: ""

        when (type) {
            "incoming_ring" -> {
                showRingNotification(sessionId, roomId, message.data["emisorName"] ?: "Visitante")
            }
            "ring_answered" -> {
                // La app está en foreground, WebSocket maneja esto
            }
            "ring_rejected" -> {
                // La app está en foreground, WebSocket maneja esto
            }
        }
    }

    private fun showRingNotification(sessionId: String, roomId: String, emisorName: String) {
        val channelId = "smartdd_ring"
        val channelName = "Timbres SMARTDD"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de timbre SMARTDD"
                enableVibration(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("sessionId", sessionId)
            putExtra("roomId", roomId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("¡Alguien está timbrando!")
            .setContentText("$emisorName está en tu puerta")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(Random.nextInt(), notification)
    }

    private fun registerDeviceToken(token: String) {
        // Enviar token al servidor para que pueda enviar notificaciones push
        // api.registerDevice(RegisterDeviceRequest(token, "android"))
    }
}
```

---

## 13. Pruebas y depuración

### 13.1 Pruebas del backend

```bash
# Probar con curl (desde el servidor o local)

# 1. Health check
curl https://api.smartdd.com/api/v1/health

# 2. Registro
curl -X POST https://api.smartdd.com/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test","email":"test2@test.com","password":"test123"}'

# 3. Login
TOKEN=$(curl -s -X POST https://api.smartdd.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@smartdd.com","password":"test123456"}' | \
  python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

# 4. Generar QR
curl -X POST https://api.smartdd.com/api/v1/qr \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"lat":19.4326,"lng":-99.1332}'

# 5. Probar admin
curl https://api.smartdd.com/api/v1/admin/stats \
  -H "Authorization: Bearer $TOKEN"
```

### 13.2 Pruebas de WebSocket

```bash
# Terminal 1: Instalar wscat
npm install -g wscat

# Conectar como Emisor
wscat -c "wss://api.smartdd.com/ws"
> {"type":"auth","token":"<TOKEN_EMISOR>"}
< {"type":"authenticated","userId":"..."}

# Terminal 2: Conectar como Receptor
wscat -c "wss://api.smartdd.com/ws"
> {"type":"auth","token":"<TOKEN_RECEPTOR>"}
< {"type":"authenticated","userId":"..."}

# Terminal 1: Enviar ring
> {"type":"ring","qrId":"<UUID_DEL_QR>"}
< {"type":"ring_sent","sessionId":"...","roomId":"..."}

# Terminal 2: Recibir incoming_ring
< {"type":"incoming_ring","sessionId":"...","roomId":"...","previewVideo":true}

# Terminal 2: Responder
> {"type":"respond","sessionId":"...","action":"accept","mode":"chat"}
< {"type":"response_sent","sessionId":"..."}

# Terminal 1: Recibir respuesta
< {"type":"ring_answered","sessionId":"...","action":"accept","mode":"chat"}
```

### 13.3 Pruebas en dispositivos reales

| Dispositivo | RAM | Android | Propósito |
|---|---|---|---|
| Google Pixel 6+ | 8GB | 14+ | Pruebas en gama alta |
| Samsung Galaxy A54 | 6GB | 13 | Pruebas en gama media |
| Xiaomi Redmi Note 12 | 4GB | 12 | Pruebas en gama media baja |
| Emulador API 26 | 2GB | 8.0 | Prueba mínima soportada |

Pruebas a realizar:
1. Escaneo QR en diferentes condiciones de luz
2. Renderizado del botón 3D en diferentes resoluciones
3. Preview de video (latencia, calidad)
4. Comunicación Chat/Audio/Video
5. Cambio entre WiFi y datos móviles
6. Recepción de notificaciones push con app cerrada
7. Timeout de sesión

### 13.4 Pruebas de carga (opcional)

```bash
# Usar k6 para pruebas de carga
# https://k6.io/

# Instalar k6
sudo apt install k6

# Crear script de prueba
cat > loadtest.js << 'EOF'
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '1m', target: 10 },  // Subir a 10 usuarios
    { duration: '2m', target: 10 },  // Mantener
    { duration: '1m', target: 0 },   // Bajar
  ],
};

export default function () {
  const res = http.get('https://api.smartdd.com/api/v1/health');
  check(res, { 'status 200': (r) => r.status === 200 });
  sleep(1);
}
EOF

# Ejecutar
k6 run loadtest.js
```

---

## 14. Empaquetado y publicación

### 14.1 Generar APK de debug

```bash
cd android
./gradlew assembleDebug

# APK generado en:
# android/app/build/outputs/apk/debug/app-debug.apk
```

### 14.2 Generar AAB de release (Play Store)

```bash
# 1. Generar keystore (si no tienes)
keytool -genkey -v -keystore smartdd.keystore \
  -alias smartdd -keyalg RSA -keysize 2048 \
  -validity 10000

# 2. Crear archivo keystore.properties en android/
cat > keystore.properties << 'EOF'
storePassword=tu-password
keyPassword=tu-password
keyAlias=smartdd
storeFile=smartdd.keystore
EOF

# 3. Configurar signing en build.gradle.kts
# android { signingConfigs { release { ... } } }

# 4. Generar AAB
./gradlew bundleRelease

# AAB generado en:
# android/app/build/outputs/bundle/release/app-release.aab
```

### 14.3 Publicar en Google Play Store

1. Ve a [Google Play Console](https://play.google.com/console)
2. Crea una nueva aplicación
3. Completa la ficha de Play Store:
   - Título: SMARTDD - Smart Ding Dong
   - Descripción corta: Timbre virtual con realidad aumentada
   - Descripción larga: (usar texto de docs/COMPETITIVE_ANALYSIS.md)
   - Categoría: Comunicación
   - Tags: QR, timbre, realidad aumentada, doorbell
4. Subir AAB en Producción
5. Completar formulario de clasificación de contenido
6. Configurar precios y distribución

### 14.4 Subir el backend al servidor (producción)

```bash
# En el servidor Ubuntu
cd ~/smartdd

# Actualizar código (si usas git)
git pull

# Reconstruir y reiniciar signaling
docker compose build signaling
docker compose up -d signaling

# Ejecutar migraciones (si hubo cambios)
docker compose exec signaling npx prisma migrate deploy
```

---

## 15. Mantenimiento y monitoreo

### 15.1 Backup de base de datos

```bash
# Crear script de backup
cat > ~/smartdd/scripts/backup.sh << 'SCRIPT'
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/var/backups/smartdd"
mkdir -p $BACKUP_DIR

cd ~/smartdd/infra

# Backup PostgreSQL
docker compose exec -T postgres pg_dump -U smartdd smartdd | \
  gzip > $BACKUP_DIR/smartdd_$DATE.sql.gz

# Eliminar backups de más de 30 días
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete

echo "Backup completado: $BACKUP_DIR/smartdd_$DATE.sql.gz"
SCRIPT

chmod +x ~/smartdd/scripts/backup.sh

# Agregar al crontab (diario a las 3am)
crontab -e
# Añadir:
# 0 3 * * * /home/usuario/smartdd/scripts/backup.sh >> /var/log/smartdd-backup.log 2>&1
```

### 15.2 Monitoreo de servicios

```bash
# Script para verificar estado
cat > ~/smartdd/scripts/health-check.sh << 'SCRIPT'
#!/bin/bash

# Verificar que los contenedores están corriendo
cd ~/smartdd/infra

SERVICES=("postgres" "redis" "signaling" "nginx" "cloudflared" "jitsi-web" "jitsi-prosody" "jitsi-jicofo" "jitsi-jvb")

for service in "${SERVICES[@]}"; do
    if docker compose ps $service | grep -q "Up"; then
        echo "✅ $service: OK"
    else
        echo "❌ $service: DOWN"
        # Opcional: enviar alerta
    fi
done

# Verificar health check de la API
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" https://api.smartdd.com/api/v1/health)
if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ API: OK"
else
    echo "❌ API: ERROR (HTTP $HTTP_CODE)"
fi
SCRIPT

chmod +x ~/smartdd/scripts/health-check.sh

# Ejecutar cada hora
crontab -e
# Añadir:
# 0 * * * * /home/usuario/smartdd/scripts/health-check.sh >> /var/log/smartdd-health.log 2>&1
```

### 15.3 Logs y rotación

Docker ya maneja logs, pero puedes configurar rotación:

```bash
# En docker-compose.yml, añadir a cada servicio:
# logging:
#   driver: "json-file"
#   options:
#     max-size: "10m"
#     max-file: "3"
```

### 15.4 Actualización de Jitsi Meet

```bash
# Verificar nueva versión
docker compose pull jitsi-web jitsi-prosody jitsi-jicofo jitsi-jvb

# Reiniciar servicios
docker compose up -d jitsi-web jitsi-prosody jitsi-jicofo jitsi-jvb

# Verificar logs
docker compose logs -f --tail=50 jitsi-jvb
```

### 15.5 Escalar recursos (cuando sea necesario)

**Si Jitsi JVB se queda sin recursos:**
- Aumentar RAM del servidor
- O añadir otro nodo JVB (escalado horizontal)

**Si PostgreSQL se vuelve lento:**
- Agregar índices adicionales (ya incluidos en schema.prisma)
- Configurar conexión pool en Prisma
- Migrar a un servidor dedicado de base de datos

**Si el signaling server no da abasto:**
- Aumentar `worker_connections` en nginx.conf
- Escalar a múltiples instancias del signaling server con Redis pub/sub

---

## Fin del manual

Este manual cubre todo el ciclo de desarrollo de SMARTDD, desde la preparación del servidor hasta la publicación en Play Store y el mantenimiento continuo. Para dudas específicas, consulta los archivos de documentación en `docs/`.

**Próximos pasos inmediatos:**

1. ✅ Instalar Docker en el servidor Ubuntu 26.04
2. ✅ Configurar Cloudflare Tunnel
3. ✅ Iniciar `docker compose up -d`
4. ✅ Inicializar backend con `npm run dev`
5. ✅ Probar endpoints básicos con curl
6. Abrir Android Studio y comenzar con la Fase 1 del desarrollo Android
