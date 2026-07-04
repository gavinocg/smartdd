# PROMPT COMPLETO — SMARTDD (Smart Ding Dong)

## Instrucciones para desarrollador fullstack

---

## 1. Visión general

Aplicación Android que permite comunicación texto/audio/video entre un Receptor y múltiples Emisores mediante un código QR con realidad aumentada simulada (botón timbre 3D). El Receptor genera un QR vinculado a su geolocalización, lo imprime y lo coloca en su puerta. El Emisor escanea el QR con la app, ve un timbre virtual 3D superpuesto sobre el QR en la cámara, lo presiona, y se establece una comunicación.

---

## 2. Stack tecnológico

### 2.1 Infraestructura (Docker en Ubuntu 26.04, servidor local)

| Contenedor | Imagen | Función |
|---|---|---|
| **postgres** | `postgres:16-alpine` | Base de datos |
| **redis** | `redis:7-alpine` | Cache, pub/sub, rate limiting |
| **signaling** | `node:22-alpine` (build propio) | API REST + WebSocket server |
| **admin-panel** | React build + nginx | Panel de administración web |
| **nginx** | `nginx:alpine` | Proxy reverso para todos los servicios HTTP |
| **jitsi-web** | `jitsi/web:latest` | Interfaz web de Jitsi Meet |
| **jitsi-prosody** | `jitsi/prosody:latest` | Servidor XMPP |
| **jitsi-jicofo** | `jitsi/jicofo:latest` | Focus / orquestador de salas |
| **jitsi-jvb** | `jitsi/jvb:latest` | Video Bridge (streaming WebRTC) |
| **cloudflared** | `cloudflare/cloudflared` | Tunnel Cloudflare |

### 2.2 Backend (Signaling Server)

- **Runtime:** Node.js 22 + TypeScript
- **Framework:** Express.js
- **WebSocket:** ws (nativo Node.js)
- **ORM:** Prisma 5+
- **Base de datos:** PostgreSQL 16
- **Cache:** Redis 7 (pub/sub, rate limiting, sesiones)
- **Autenticación:** JWT (access + refresh token)

### 2.3 Frontend Android

- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose
- **Arquitectura:** MVVM + Clean Architecture (domain/data/presentation)
- **DI:** Hilt
- **Networking:** Retrofit (REST) + OkHttp WebSocket
- **Cámara/QR:** CameraX + ML Kit Barcode Scanning
- **RA:** SceneView (OpenGL) para renderizar modelo 3D
- **Video/Audio:** Jitsi Meet SDK (org.jitsi.react:jitsi-meet-sdk)
- **Notificaciones:** Firebase Cloud Messaging
- **Pagos:** Google Play Billing Library
- **Almacenamiento seguro:** EncryptedSharedPreferences (JWT)

### 2.4 Admin Panel

- **Stack:** React + Vite (o Next.js)
- **UI:** Component library (shadcn/ui o similar)
- **Estado:** TanStack Query
- **API:** Consume el mismo signaling server con endpoints específicos de admin

---

## 3. Modelo de datos (Prisma Schema)

```prisma
enum Plan {
  FREE
  PRO
  B2P
}

enum SessionStatus {
  PENDING
  PREVIEW
  ACTIVE
  COMPLETED
  REJECTED
  TIMEOUT
}

enum ResponseMode {
  CHAT
  AUDIO
  VIDEO
}

model User {
  id            String   @id @default(uuid())
  name          String
  email         String   @unique
  passwordHash  String
  plan          Plan     @default(FREE)
  role          String   @default("user") // "user" | "admin" | "business"
  active        Boolean  @default(true)
  createdAt     DateTime @default(now())
  updatedAt     DateTime @updatedAt

  qrCodes      QrCode[]
  sessions     RingSession[]  @relation("ReceptorSessions")
  devices      Device[]
  config       UserConfig?
}

model QrCode {
  id        String   @id @default(uuid())
  uuid      String   @unique
  userId    String
  user      User     @relation(fields: [userId], references: [id])
  lat       Float
  lng       Float
  radiusMeters Int   @default(50)
  active    Boolean  @default(true)
  metadata  Json?    // Para futuro B2P: { tableNumber, businessId, etc }
  createdAt DateTime @default(now())
  updatedAt DateTime @updatedAt

  sessions  RingSession[]
}

model RingSession {
  id          String        @id @default(uuid())
  uuid        String        @unique // roomId para Jitsi
  qrId        String
  qr          QrCode        @relation(fields: [qrId], references: [id])
  emisorId    String
  emisorName  String?
  receptorId  String
  receptor    User          @relation("ReceptorSessions", fields: [receptorId], references: [id])
  status      SessionStatus @default(PENDING)
  responseMode ResponseMode?
  previewStartedAt DateTime?
  respondedAt   DateTime?
  createdAt   DateTime      @default(now())
  updatedAt   DateTime      @updatedAt
}

model Device {
  id          String   @id @default(uuid())
  userId      String
  user        User     @relation(fields: [userId], references: [id])
  token       String   // FCM token
  platform    String   // "android"
  active      Boolean  @default(true)
  createdAt   DateTime @default(now())
}

model UserConfig {
  id                String       @id @default(uuid())
  userId            String       @unique
  user              User         @relation(fields: [userId], references: [id])
  defaultMode       ResponseMode @default(CHAT)
  chatEnabled       Boolean      @default(true)
  audioEnabled      Boolean      @default(true)
  videoEnabled      Boolean      @default(true)
  timeoutSeconds    Int          @default(60)
  createdAt         DateTime     @default(now())
  updatedAt         DateTime     @updatedAt
}

model AdminLog {
  id        String   @id @default(uuid())
  adminId   String
  action    String   // "change_plan", "suspend_user", "delete_user"
  targetId  String   // userId afectado
  details   Json?
  createdAt DateTime @default(now())
}
```

---

## 4. API REST (Signaling Server)

### 4.1 Autenticación

```
POST   /api/v1/auth/register    → { name, email, password } → { user, token }
POST   /api/v1/auth/login       → { email, password }      → { user, token, refreshToken }
POST   /api/v1/auth/refresh     → { refreshToken }         → { token }
GET    /api/v1/auth/me          → { user }                 [Auth required]
```

### 4.2 QRs

```
POST   /api/v1/qr                    → { lat, lng, radius? }    → { qr }        [Auth]
GET    /api/v1/qr                     → { qrs: QrCode[] }        → Lista del user [Auth]
GET    /api/v1/qr/:uuid               → { qr }                                    [Public]
POST   /api/v1/qr/:uuid/validate      → { lat, lng }             → { valid, distance }
DELETE /api/v1/qr/:uuid               → { success }                              [Auth]
```

### 4.3 Ring / Sesiones

```
POST   /api/v1/ring               → { qrId }              → { session, roomId }   [Auth?]
POST   /api/v1/respond            → { sessionId, action, mode } → { session }     [Auth]
GET    /api/v1/session/:id        → { session }                                   [Auth]
```

### 4.4 Configuración del Receptor

```
GET    /api/v1/user/config        → { config }                                    [Auth]
PUT    /api/v1/user/config        → { defaultMode, chatEnabled, audioEnabled, videoEnabled }
```

### 4.5 Planes

```
GET    /api/v1/user/plan          → { plan, qrLimit, deviceLimit }
POST   /api/v1/user/upgrade       → { plan: "pro" }            [Google Play Billing]
```

### 4.6 Admin

```
GET    /api/v1/admin/users        → { users[], total, page }                    [Auth: admin]
GET    /api/v1/admin/users/:id    → { user, qrCodes[], sessions[] }             [Auth: admin]
PUT    /api/v1/admin/users/:id/plan  → { plan }                                 [Auth: admin]
PUT    /api/v1/admin/users/:id/suspend → { active: false }                      [Auth: admin]
GET    /api/v1/admin/stats        → { totalUsers, freeUsers, proUsers, todayQRs } [Auth: admin]
GET    /api/v1/admin/config       → { defaultRadius, defaultTimeout }           [Auth: admin]
PUT    /api/v1/admin/config       → { defaultRadius, defaultTimeout }           [Auth: admin]
GET    /api/v1/admin/logs         → { logs[] }                                   [Auth: admin]
```

---

## 5. Eventos WebSocket

### Conexión

```
Cliente → Servidor: { type: "auth", token: "jwt..." }
Servidor → Cliente: { type: "authenticated", userId: "..." }
```

### Ring / Timbre

```
Cliente (Emisor) → Servidor: { type: "ring", qrId: "..." }
Servidor → Cliente (Receptor): { type: "incoming_ring", sessionId, roomId, emisorName, previewVideo: true }
Servidor → Cliente (Emisor): { type: "ring_sent", sessionId, roomId }
```

### Respuesta

```
Cliente (Receptor) → Servidor: { type: "respond", sessionId, action: "accept"|"reject", mode?: "chat"|"audio"|"video" }
Servidor → Cliente (Emisor): { type: "ring_answered", action, mode }
Servidor → Cliente (Emisor): { type: "ring_rejected" } [si action=reject]
Servidor → Cliente (Receptor): { type: "response_sent", sessionId }
```

### Cancelación

```
Cliente (Emisor) → Servidor: { type: "cancel_ring", sessionId }
Servidor → Cliente (Receptor): { type: "ring_cancelled", sessionId }
```

### Timeout

```
Servidor → Cliente (Receptor): { type: "ring_timeout", sessionId }
Servidor → Cliente (Emisor): { type: "ring_timeout", sessionId }
```

### Chat

```
Cliente → Servidor: { type: "chat_message", sessionId, message: "..." }
Servidor → Cliente (Receptor): { type: "chat_message", sessionId, from, message }
Servidor → Cliente (Emisor): { type: "chat_message", sessionId, from, message }
```

---

## 6. Flujo completo de punta a punta

```
1. [Android] Receptor: Login → Configura modo respuesta → Genera QR (lat/lng actual)
   - POST /api/v1/auth/login → { token }
   - GET /api/v1/user/config → { defaultMode, chatEnabled, audioEnabled, videoEnabled }
   - POST /api/v1/qr { lat, lng } → { qr: { uuid, imageUrl } }
   - App muestra QR en pantalla. Receptor descarga/imprime.

2. [Android] Emisor: Abre app → Escanea QR con cámara
   - CameraX + ML Kit detectan QR
   - Extrae UUID del QR
   - POST /api/v1/qr/{uuid}/validate { lat, lng }
   - Servidor calcula distancia entre QR.lat/lng y Emisor.lat/lng
   - Si distancia > radiusMeters → rechaza ("Debes estar en la ubicación del QR")
   - Si distancia ≤ radiusMeters → OK

3. [Android] Emisor: Ve botón timbre 3D sobre QR
   - SceneView renderiza modelo 3D sobre coordenadas del QR
   - Animación pulsante. Texto: "Presiona el timbre para llamar"

4. [Android] Emisor: Presiona botón timbre
   - Animación press 3D
   - POST /api/v1/ring { qrId } → { sessionId, roomId }
   - Activa cámara frontal (solicita permiso si es necesario)
   - JitsiMeetActivity.join(roomId, { video: ON, audio: OFF })
   - WebSocket envía { type: "ring", qrId }
   - App muestra pantalla "Esperando respuesta..." con preview de video propio

5. [Servidor] Signaling Server:
   - Crea RingSession con status PENDING
   - Envía WebSocket { incoming_ring } al Receptor
   - Si Receptor no está conectado WebSocket → envía notificación FCM vía Firebase Admin SDK
   - Inicia timer de timeout (60 segundos configurable)

6. [Android] Receptor: Recibe notificación
   - App muestra pantalla Incoming Call
   - JitsiMeetActivity.join(roomId, { video: OFF, audio: OFF })
   - Receptor ve preview de video del Emisor en vivo
   - Botones: Responder Chat | Audio | Video (según configuración) + Rechazar

7. [Android] Receptor: Responde
   - Opción A: Responde Chat
     - WebSocket: { type: "respond", sessionId, action: "accept", mode: "chat" }
     - Ambos abandonan sala Jitsi
     - Se abre interfaz de chat de texto (WebSocket propio para mensajes)
   - Opción B: Responde Audio
     - WebSocket: { type: "respond", sessionId, action: "accept", mode: "audio" }
     - SDK setAudioMuted(false) en ambos
     - Comunicación de audio bidireccional (video OFF)
   - Opción C: Responde Audio+Video
     - WebSocket: { type: "respond", sessionId, action: "accept", mode: "video" }
     - SDK setAudioMuted(false) + setVideoMuted(false) en ambos
     - Comunicación completa bidireccional
   - Opción D: Rechazar
     - WebSocket: { type: "respond", sessionId, action: "reject" }
     - Ambos abandonan sala Jitsi
     - Emisor ve "Solicitud rechazada"

8. [Android] Fin de comunicación
   - Cualquiera puede finalizar (botón colgar/cerrar)
   - Ambos abandonan sala Jitsi
   - Servidor actualiza sesión a COMPLETED
```

---

## 7. Funcionalidades del Panel de Administración

- Dashboard con stats: usuarios totales, gratis vs pro, QRs generados hoy
- CRUD de usuarios: listar, buscar, filtrar por plan, cambiar plan, suspender/activar
- Vista de detalle de usuario con sus QRs y sesiones
- Configuración global: radio de geolocalización default, timeout de sesión
- Log de auditoría de acciones del admin

---

## 8. Validaciones y reglas de negocio

| Regla | Descripción |
|---|---|
| **Límite Free** | 1 QR activo, 1 dispositivo simultáneo |
| **Límite Pro** | QRs y dispositivos ilimitados |
| **Geolocalización** | Emisor debe estar dentro del radio configurado del QR (default 50m) |
| **Timeout preview** | 60 segundos configurable (campo `timeoutSeconds` en `UserConfig`) |
| **Preview unidireccional** | Emisor: video ON. Receptor: video OFF (solo recibe) |
| **Chat offline** | Mensajes de chat almacenados solo localmente (Room DB en Android) |
| **Mínimo una opción** | El Receptor debe tener al menos una opción de respuesta habilitada |
| **Sesión única** | Un QR solo puede tener una sesión activa a la vez |

---

## 9. Dependencias Android (build.gradle)

```gradle
// Versiones
ext {
    compose_version = '1.7.0'
    hilt_version = '2.51'
}

// Dependencias principales
implementation 'org.jitsi.react:jitsi-meet-sdk:12.1.3'
implementation 'com.google.mlkit:barcode-scanning:17.3.0'
implementation 'io.github.sceneview:sceneview:2.2.2'
implementation 'com.squareup.retrofit2:retrofit:2.11.0'
implementation 'com.squareup.retrofit2:converter-gson:2.11.0'
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
implementation 'com.google.firebase:firebase-messaging:24.1.0'
implementation 'com.android.billingclient:billing:7.1.1'
implementation 'androidx.security:security-crypto:1.1.0-alpha06'
implementation 'androidx.room:room-runtime:2.6.1'
implementation 'androidx.room:room-ktx:2.6.1'

// Compose
implementation "androidx.compose.ui:ui:$compose_version"
implementation "androidx.compose.material3:material3:1.3.0"
implementation "androidx.compose.ui:ui-tooling-preview:$compose_version"
implementation "androidx.navigation:navigation-compose:2.8.0"
implementation "androidx.camera:camera-camera2:1.4.0"

// DI
implementation "com.google.dagger:hilt-android:$hilt_version"
kapt "com.google.dagger:hilt-compiler:$hilt_version"

// Permisos
implementation "com.google.accompanist:accompanist-permissions:0.36.0"
```

---

## 10. Variables de entorno (`.env`)

```env
# Base de datos
DATABASE_URL=postgresql://smartdd:smartdd_pass@postgres:5432/smartdd

# Redis
REDIS_URL=redis://redis:6379

# JWT
JWT_SECRET=your-secret-key-here
JWT_EXPIRES_IN=24h
JWT_REFRESH_SECRET=your-refresh-secret-here
JWT_REFRESH_EXPIRES_IN=7d

# Jitsi
JITSI_DOMAIN=jitsi.smartdd.com
JITSI_SECRET=your-jitsi-app-secret

# Cloudflare Tunnel
TUNNEL_TOKEN=your-cloudflare-tunnel-token

# Firebase
FCM_SERVER_KEY=your-fcm-server-key

# Google Play Billing
PLAY_LICENSE_KEY=your-google-play-license-key

# Admin
ADMIN_EMAIL=admin@smartdd.com
ADMIN_PASSWORD=admin-secure-password

# Config global
DEFAULT_RADIUS_METERS=50
DEFAULT_TIMEOUT_SECONDS=60
```

---

## 11. Subdominios y DNS

| Subdominio | Servicio | Puerto interno |
|---|---|---|
| `api.smartdd.com` | nginx → signaling | 3000 |
| `admin.smartdd.com` | nginx → admin-panel | 80 |
| `jitsi.smartdd.com` | nginx → jitsi-web | 80 |

---

## 12. Permisos Android (AndroidManifest.xml)

```xml
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
```

---

## 13. Arquitectura Android (MVVM + Clean Architecture)

```
com.smartdd.app/
├── data/
│   ├── local/
│   │   ├── db/              # Room database (chat messages, cache)
│   │   └── preferences/     # EncryptedSharedPreferences (JWT)
│   ├── remote/
│   │   ├── api/             # Retrofit interfaces
│   │   ├── websocket/       # WebSocket client
│   │   └── fcm/             # Firebase Messaging Service
│   └── repository/          # Implementaciones de repositorios
├── domain/
│   ├── model/               # Entidades de dominio
│   ├── repository/          # Interfaces de repositorio
│   └── usecase/             # Casos de uso
├── presentation/
│   ├── auth/                # Login, Register
│   ├── home/                # Home screen
│   ├── qr/
│   │   ├── generate/        # Generar QR
│   │   ├── scanner/         # Escanear QR
│   │   └── ar/              # Timbre 3D RA
│   ├── ring/
│   │   ├── sender/          # Emisor: esperando respuesta
│   │   └── receiver/        # Receptor: incoming call
│   ├── call/
│   │   ├── chat/            # Interfaz de chat
│   │   ├── audio/           # Llamada de audio
│   │   └── video/           # Videollamada
│   ├── profile/             # Perfil y upgrade
│   └── settings/            # Configuración del Receptor
├── di/                      # Módulos Hilt
├── navigation/              # NavGraph
└── SmartDDApp.kt            # Application class
```

---

## 14. Consideraciones técnicas importantes

### 14.1 RA Simulada (sin ARCore)
- Usar ML Kit Barcode Scanning para detectar QR y obtener coordenadas en el preview
- SceneView para renderizar modelo 3D (GLB/GLTF) sobre las coordenadas
- El modelo 3D debe ser <500KB y <5k polígonos para rendimiento fluido
- El modelo debe tener animación idle (pulsación) y animación trigger (hundimiento)

### 14.2 Jitsi Meet SDK
- Usar `JitsiMeetActivity` o `JitsiMeetView` para integrar la sala
- Control programático: `jitsiMeetView.setAudioMuted(true/false)`, `jitsiMeetView.setVideoMuted(true/false)`
- Configurar `setCameraFacing("front")` para el Emisor
- La sala Jitsi usa UUIDv4 generado por el servidor como nombre de sala

### 14.3 Chat de texto propio
- No usar el chat de Jitsi. Implementar chat propio vía WebSocket
- Al responder con Chat, ambos abandonan sala Jitsi y se abren vistas de chat nativas
- Los mensajes se envían como eventos WebSocket y se almacenan localmente en Room DB
- El historial de chat no se persiste en el servidor por privacidad

### 14.4 Manejo de permisos
- Solicitar permisos de forma progresiva (just-in-time)
- CAMERA: al abrir escáner QR o al iniciar preview de video
- ACCESS_FINE_LOCATION: al validar QR
- RECORD_AUDIO: al aceptar llamada de audio/video
- POST_NOTIFICATIONS: al registrarse

### 14.5 Manejo de errores
- Timeout de conexión: mostrar "Sin conexión" después de 10s sin respuesta del servidor
- Error de geolocalización: "No pudimos verificar tu ubicación. Activa el GPS e inténtalo de nuevo"
- Error de cámara: "No se pudo acceder a la cámara. Verifica los permisos"
- Error de WebRTC: "No se pudo establecer la comunicación. Verifica tu conexión a internet"

---

## 15. Plan de desarrollo por fases

### Fase 1: Infraestructura (Semana 1-2)
- [ ] Configurar Docker Compose con todos los servicios
- [ ] Configurar Cloudflare Tunnel
- [ ] Configurar nginx como proxy reverso
- [ ] Inicializar PostgreSQL y Redis
- [ ] Configurar Jitsi Meet self-hosted
- [ ] Verificar comunicación entre contenedores

### Fase 2: Backend (Semana 2-4)
- [ ] Proyecto Node.js + TypeScript + Express
- [ ] Esquema Prisma + migraciones
- [ ] Endpoints de autenticación (register, login, JWT)
- [ ] Endpoints de QR (CRUD + validación geolocalización)
- [ ] Endpoints de Ring/Sesión
- [ ] WebSocket server (eventos en tiempo real)
- [ ] Endpoints de Admin
- [ ] Integración con Redis (pub/sub, rate limiting)

### Fase 3: Android — Core (Semana 4-6)
- [ ] Proyecto Android + DI (Hilt) + Navigation
- [ ] Pantallas: Splash, Login, Registro
- [ ] Networking: Retrofit + WebSocket client
- [ ] Autenticación y almacenamiento de JWT
- [ ] Pantalla Home (Receptor y Emisor)

### Fase 4: Android — QR y RA (Semana 6-8)
- [ ] Generación de QR (pantalla + API)
- [ ] Escáner QR con ML Kit + CameraX
- [ ] Validación de geolocalización
- [ ] Integración SceneView: modelo 3D del timbre
- [ ] Animación del botón 3D (pulsación + press)

### Fase 5: Android — Comunicación (Semana 8-10)
- [ ] Integración Jitsi Meet SDK
- [ ] Preview de video (Emisor → Receptor)
- [ ] Pantalla Incoming Call con configuración dinámica
- [ ] Respuesta Chat (WebSocket + Room DB)
- [ ] Respuesta Audio (Jitsi setAudioMuted)
- [ ] Respuesta Video (Jitsi setAudioMuted + setVideoMuted)

### Fase 6: Admin Panel (Semana 10-11)
- [ ] Proyecto React + Vite
- [ ] Dashboard con stats
- [ ] CRUD de usuarios
- [ ] Configuración global

### Fase 7: Finalización (Semana 11-12)
- [ ] Google Play Billing Integration
- [ ] Notificaciones Push (FCM)
- [ ] Pruebas de integración
- [ ] Pruebas en dispositivos reales (gama media y alta)
- [ ] Empaquetado APK + Play Store listing
