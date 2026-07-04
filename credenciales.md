# SMARTDD — Credenciales y Accesos

## Servidor

| Recurso | Valor |
|---------|-------|
| IP | `192.168.100.101` |
| Usuario SSH | `gcarranco` |
| Puerto SSH | `22` |
| SO | Ubuntu 26.04 |
| Sudo | `admin123` |

## Base de Datos (PostgreSQL)

| Recurso | Valor |
|---------|-------|
| Puerto host | `5433` |
| Usuario | `smartdd` |
| Contraseña | `smartdd_pass` |
| Base de datos | `smartdd` |
| URL (host) | `postgresql://smartdd:smartdd_pass@127.0.0.1:5433/smartdd` |

## Redis

| Recurso | Valor |
|---------|-------|
| Puerto host | `6380` |
| Contraseña | `redis_pass` |
| URL (host) | `redis://default:redis_pass@127.0.0.1:6380` |

## Signaling Server (API REST + WebSocket)

| Recurso | Valor |
|---------|-------|
| URL API | `http://192.168.100.101:3000` |
| URL vía nginx | `http://192.168.100.101:8000` (proxy a :3000) |
| WebSocket | `ws://192.168.100.101:8000/ws` (vía nginx) |
| Puerto contenedor | `3000` |
| Imagen Docker | `smartdd-signaling:latest` / `infra-signaling:latest` |

## Jitsi Meet

| Recurso | Valor |
|---------|-------|
| URL vía nginx | `http://192.168.100.101:82` |
| Jitsi web interno | `127.0.0.1:8081` |
| XMPP domain | `192.168.100.101` |
| Puerto JVB (UDP) | `10000` |

## Apache (Puerto 80)

| Sitio | Dominio | Destino |
|-------|---------|---------|
| Principal | `sga-sp.com` | `/var/www/sga-sp.com/public` (PHP 8.5) |
| SPI | `spi.sga-sp.com` | `/var/www/spi.sga-sp.com/public` (PHP 8.5) |
| Caja | `caja.sga-sp.com` | `/var/www/caja.sga-sp.com` (PHP 8.5) |
| Evolution API | `evolution.sga-sp.com` | Proxy a `127.0.0.1:8080` |

## Nginx (Proxy Reverso — SMARTDD)

| Puerto | Uso | Destino |
|--------|-----|---------|
| `8000` | API REST + WebSocket | `127.0.0.1:3000` |
| `81` | Admin Panel (estático) | `./admin-dev/index.html` |
| `82` | Jitsi Meet | `127.0.0.1:8081` |

## JWT

| Recurso | Valor |
|---------|-------|
| JWT Secret | `dev-jwt-secret-smartdd-2026` |
| JWT Expiración | `24h` |
| Refresh Secret | `dev-jwt-refresh-secret-smartdd-2026` |
| Refresh Expiración | `7d` |

## Usuarios Seed (desarrollo)

| Rol | Email | Contraseña |
|-----|-------|------------|
| Admin | `admin@smartdd.com` | `admin123456` |
| Test | `test@smartdd.com` | `test123456` |

## Jitsi Auth

| Variable | Valor |
|----------|-------|
| `JICOFO_AUTH_PASSWORD` | `jicofo_secret` |
| `JVB_AUTH_PASSWORD` | `jvb_secret` |

## Contenedores

| Nombre | Servicio | Estado |
|--------|----------|--------|
| `smartdd-postgres` | PostgreSQL 16 | healthy |
| `smartdd-redis` | Redis 7 | healthy |
| `smartdd-signaling` | Signaling Server | running |
| `smartdd-nginx` | nginx proxy | running |
| `smartdd-jitsi-prosody` | Prosody XMPP | running |
| `smartdd-jitsi-jicofo` | Jicofo focus | running |
| `smartdd-jitsi-jvb` | Jitsi Videobridge | running |
| `smartdd-jitsi-web` | Jitsi Meet web | running |
| `evolution-api` | Evolution API (WhatsApp) | running |
| `evolution-redis` | Evolution Redis | running |

## Archivos de Configuración

| Archivo | Propósito |
|---------|-----------|
| `infra/.env` | Variables de entorno (dev) |
| `infra/docker-compose.yml` | Definición de servicios |
| `infra/docker-compose.override.yml` | Host networking + dev overrides |
| `infra/nginx/nginx.dev.conf` | Configuración nginx (dev) — API en :8000 |
| `infra/jitsi-web/site.conf` | Override nginx Jitsi web (escucha en :8081) |
| `backend/Dockerfile` | Build del signaling server |
| `backend/prisma/schema.prisma` | Esquema de base de datos |
| `/root/evolution-docker-compose.yml` | Evolution API (producción) |
| `/etc/apache2/sites-available/` | VirtualHosts: sga-sp.com, spi, caja, evolution |

## Evolution API (WhatsApp — otro proyecto)

| Recurso | Valor |
|---------|-------|
| URL interna | `http://127.0.0.1:8080` |
| URL pública | `http://evolution.sga-sp.com` (vía Apache) |
| API Key | `4piK3yWah4202G` |
| Puerto contenedor | `8080` |
| Redis | `evolution-redis` (puerto host `6379`) |

## Comandos Útiles

```bash
# Iniciar servicios SMARTDD
cd ~/smartdd/infra
echo admin123 | sudo -S docker compose up -d

# Iniciar Evolution API
echo admin123 | sudo -S docker compose -f /root/evolution-docker-compose.yml up -d

# Apache
echo admin123 | sudo -S systemctl start apache2
echo admin123 | sudo -S systemctl stop apache2
echo admin123 | sudo -S systemctl reload apache2

# Ver logs de un servicio
echo admin123 | sudo -S docker logs smartdd-signaling

# Reconstruir y reiniciar signaling
echo admin123 | sudo -S docker compose up -d --build signaling

# Acceder a la base de datos
echo admin123 | sudo -S docker exec -it smartdd-postgres psql -U smartdd -d smartdd -p 5433

# Ver estado de todos los contenedores
echo admin123 | sudo -S docker ps --format '{{.Names}} {{.Status}}'

# Nota: sudo requiere contraseña (admin123)
# Usar: echo admin123 | sudo -S <comando>
```

> **⚠️ Importante:** Estas credenciales son **solo para desarrollo**. Cambiar todas las contraseñas y secrets antes de pasar a producción.
