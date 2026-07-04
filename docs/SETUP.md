# Guía de Instalación y Configuración — SMARTDD

---

## Requisitos del servidor

| Recurso | Mínimo | Recomendado |
|---|---|---|
| **CPU** | 2 núcleos | 4 núcleos |
| **RAM** | 4 GB | 8 GB |
| **Disco** | 20 GB SSD | 40 GB SSD |
| **SO** | Ubuntu 26.04 | Ubuntu 26.04 LTS |
| **Docker** | 24+ | 27+ |
| **Docker Compose** | v2 | v2 |
| **Internet** | 10 Mbps simétrico | 50 Mbps simétrico |
| **Puerto UDP** | 10000 (JVB) | Abierto en router |
| **Dominio** | smartdd.com | Con Cloudflare DNS |

---

## 1. Instalación de Docker y Docker Compose

```bash
# Actualizar paquetes
sudo apt update && sudo apt upgrade -y

# Instalar Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Agregar usuario al grupo docker
sudo usermod -aG docker $USER

# Verificar instalación
docker --version
docker compose version

# Habilitar Docker al inicio
sudo systemctl enable docker
```

---

## 2. Preparar el proyecto

```bash
# Clonar o copiar el proyecto
mkdir -p ~/smartdd
cd ~/smartdd

# Copiar todos los archivos del proyecto aquí
# Estructura esperada:
# smartdd/
# ├── infra/
# │   ├── docker-compose.yml
# │   ├── .env
# │   ├── nginx/
# │   │   └── nginx.conf
# │   └── cloudflared/
# │       └── config.yml
# ├── backend/
# │   ├── Dockerfile
# │   ├── package.json
# │   ├── prisma/
# │   │   └── schema.prisma
# │   └── src/
# ├── android/
# └── docs/
```

---

## 3. Configurar variables de entorno

```bash
cd ~/smartdd/infra
cp .env.example .env
nano .env
```

Editar las siguientes variables:
- `DATABASE_URL` — credenciales de PostgreSQL
- `JWT_SECRET` — clave secreta para tokens
- `JWT_REFRESH_SECRET` — clave secreta para refresh tokens
- `TUNNEL_TOKEN` — token de Cloudflare Tunnel
- `FCM_SERVER_KEY` — clave del servidor Firebase
- `PLAY_LICENSE_KEY` — licencia de Google Play
- `ADMIN_EMAIL` / `ADMIN_PASSWORD` — credenciales del admin inicial

---

## 4. Configurar Cloudflare Tunnel

### 4.1 Crear el tunnel en Cloudflare Zero Trust

```bash
# En tu máquina local (no en el servidor aún)
# Descargar cloudflared
curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 -o cloudflared
chmod +x cloudflared

# Autenticar con Cloudflare
./cloudflared tunnel login

# Crear un tunnel
./cloudflared tunnel create smartdd

# El comando genera un tunnel ID y un archivo JSON con credenciales
# Copiar el tunnel ID al archivo config.yml
```

### 4.2 Configurar DNS en Cloudflare Dashboard

```
CNAME api.smartdd.com   → {tunnel-id}.cfargotunnel.com
CNAME admin.smartdd.com → {tunnel-id}.cfargotunnel.com
CNAME jitsi.smartdd.com → {tunnel-id}.cfargotunnel.com
```

### 4.3 Copiar credenciales al servidor

```bash
# Copiar el archivo de credenciales al servidor
scp ~/.cloudflared/{tunnel-id}.json user@server:~/smartdd/infra/cloudflared/
```

---

## 5. Configurar Jitsi Meet

### 5.1 Configurar autenticación de sala (opcional)

Crear archivo `infra/jitsi/prosody.cfg.lua` (se mapea como volumen en jitsi-prosody):

```lua
-- Configuración para usar UUID como contraseña de sala
VirtualHost "jitsi.smartdd.com"
    authentication = "token"
    app_id = "smartdd"
    app_secret = "your-jitsi-app-secret"
```

### 5.2 Deshabilitar salas públicas

En la configuración de Jitsi (jitsi-web), deshabilitar la creación de salas desde la interfaz web.

---

## 6. Iniciar la infraestructura

```bash
cd ~/smartdd/infra

# Descargar imágenes
docker compose pull

# Iniciar todos los servicios
docker compose up -d

# Verificar que todos los servicios estén corriendo
docker compose ps

# Ver logs
docker compose logs -f

# Servicios individuales
docker compose logs -f signaling
docker compose logs -f jitsi-jvb
```

---

## 7. Configurar el firewall (ufw)

```bash
# Puerto para Jitsi Video Bridge (UDP)
sudo ufw allow 10000/udp

# SSH
sudo ufw allow 22

# Solo permitir Cloudflare IPs para HTTP/HTTPS (opcional)
sudo ufw allow from 173.245.48.0/20
sudo ufw allow from 103.21.244.0/22
# ... resto de rangos de Cloudflare

sudo ufw enable
```

---

## 8. Desarrollo del backend (signaling server)

```bash
# En el servidor o localmente
cd ~/smartdd/backend

# Instalar dependencias
npm install

# Configurar variables de entorno para desarrollo
cp .env.development .env

# Ejecutar migraciones de Prisma
npx prisma migrate dev --name init

# Generar Prisma Client
npx prisma generate

# Iniciar en modo desarrollo
npm run dev
```

---

## 9. Desarrollo de Android

### 9.1 Requisitos locales

- Android Studio Hedgehog (2023.1.1+) o superior
- JDK 17+
- Android SDK API 26+
- Gradle 8+

### 9.2 Configurar proyecto

```bash
# Abrir android/ en Android Studio
# Gradle sincronizará automáticamente

# Configurar URL del servidor
# En android/app/src/main/res/values/strings.xml añadir:
# <string name="server_url">https://api.smartdd.com</string>
# <string name="jitsi_domain">jitsi.smartdd.com</string>

# Agregar google-services.json de Firebase en android/app/
```

### 9.3 Compilar APK

```bash
cd android
./gradlew assembleDebug
# APK en: android/app/build/outputs/apk/debug/
```

---

## 10. Comandos útiles

```bash
# Reiniciar un servicio específico
docker compose restart signaling

# Ver logs en tiempo real
docker compose logs -f --tail=100 signaling

# Acceder a la base de datos
docker compose exec postgres psql -U smartdd -d smartdd

# Backup de la base de datos
docker compose exec postgres pg_dump -U smartdd smartdd > backup_$(date +%Y%m%d).sql

# Restaurar backup
cat backup.sql | docker compose exec -T postgres psql -U smartdd -d smartdd

# Actualizar Jitsi Meet
docker compose pull jitsi-web jitsi-prosody jitsi-jicofo jitsi-jvb
docker compose up -d

# Detener todo
docker compose down

# Detener y eliminar volúmenes (cuidado: borra datos)
docker compose down -v
```

---

## 11. Verificación de funcionamiento

### 11.1 API

```bash
# Health check
curl https://api.smartdd.com/api/v1/health

# Debería responder:
# { "status": "ok", "timestamp": "..." }
```

### 11.2 Jitsi

```bash
# Abrir en navegador:
https://jitsi.smartdd.com

# Debería mostrar la interfaz de Jitsi Meet (aunque no se usará directamente)
```

### 11.3 Admin Panel

```bash
# Abrir en navegador:
https://admin.smartdd.com

# Login con credenciales de admin
```

---

## 12. Solución de problemas comunes

### El tunnel Cloudflare no funciona
- Verificar que el token del tunnel sea correcto
- Verificar que los registros DNS apunten al tunnel ID correcto
- Revisar logs: `docker compose logs cloudflared`

### No se establece comunicación WebRTC
- Verificar que el puerto UDP 10000 esté abierto en el router/firewall
- Verificar que el JVB esté corriendo: `docker compose logs jitsi-jvb`
- Probar con un STUN público si JVB no responde

### Error de conexión a PostgreSQL
- Verificar que postgres esté corriendo: `docker compose ps`
- Verificar credenciales en `.env`
- Revisar logs: `docker compose logs postgres`

### La app Android no conecta
- Verificar que el tunnel esté activo
- Verificar que el URL del servidor en la app sea correcto
- Verificar que el dispositivo tenga acceso a internet
- Probar con `curl` desde el dispositivo hacia `https://api.smartdd.com/api/v1/health`
