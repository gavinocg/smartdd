# Plan de Desarrollo — SMARTDD

> Basado en `docs/SRS.md`, `docs/DEVELOPER_PROMPT.md`, `docs/Manual de Desarrollo.md`
> Generado: 2026-07-04

---

## Leyenda

| Símbolo | Significado |
|---|---|
| ✅ | Completado |
| 🔴 | Pendiente — Alta prioridad |
| 🟡 | Pendiente — Media prioridad |
| 🟢 | Pendiente — Baja prioridad |

---

## Fase 1: Infraestructura (Docker + Red)

| Item | Prioridad | Estado | Notas |
|---|---|---|---|
| Docker Compose (10 servicios) | — | ✅ | `infra/docker-compose.yml`, corriendo en servidor |
| nginx reverse proxy | — | ✅ | `infra/nginx/nginx.conf` + `.dev.conf` |
| Jitsi Meet self-hosted | — | ✅ | Puerto 82 (host) / 8081 (web interna) |
| `.env.example` | — | ✅ | Template de variables de entorno |
| Cloudflare Tunnel | 🟢 | ⚠️ Pendiente | `config.yml` existe, faltan credenciales (`credentials.json`) |
| HTTPS/SSL (Let's Encrypt) | 🟢 | ❌ | Todo en HTTP para desarrollo |
| Jitsi JWT auth | 🟢 | ❌ | `JITSI_SECRET` existe en env pero no implementado |

---

## Fase 2: Backend (Node.js / Express / TypeScript)

### Rutas existentes

| Ruta | Archivo | Estado |
|---|---|---|
| `POST /api/v1/auth/register` | `routes/auth.ts` | ✅ |
| `POST /api/v1/auth/login` | `routes/auth.ts` | ✅ |
| `POST /api/v1/auth/refresh` | `routes/auth.ts` | ✅ |
| `GET /api/v1/auth/me` | `routes/auth.ts` | ✅ |
| `POST /api/v1/qr` | `routes/qr.ts` | ✅ |
| `GET /api/v1/qr` | `routes/qr.ts` | ✅ |
| `GET /api/v1/qr/:uuid` | `routes/qr.ts` | ✅ (público) |
| `POST /api/v1/qr/:uuid/validate` | `routes/qr.ts` | ✅ (público, Haversine) |
| `DELETE /api/v1/qr/:uuid` | `routes/qr.ts` | ✅ |
| `POST /api/v1/ring` | `routes/ring.ts` | ✅ |
| `POST /api/v1/respond` | `routes/ring.ts` | ✅ |
| `GET /api/v1/session/:id` | `routes/ring.ts` | ✅ |
| `GET /api/v1/user/config` | `routes/config.ts` | ✅ |
| `PUT /api/v1/user/config` | `routes/config.ts` | ✅ |
| `GET /api/v1/user/plan` | `routes/plan.ts` | ✅ |
| `POST /api/v1/user/upgrade` | `routes/plan.ts` | ⚠️ Sin verificar purchaseToken |
| `POST /api/v1/user/cancel` | `routes/plan.ts` | ✅ |
| `GET /admin/users` | `routes/admin.ts` | ✅ |
| `GET /admin/users/:id` | `routes/admin.ts` | ✅ |
| `PUT /admin/users/:id/plan` | `routes/admin.ts` | ✅ |
| `PUT /admin/users/:id/suspend` | `routes/admin.ts` | ✅ |
| `GET /admin/stats` | `routes/admin.ts` | ✅ |
| `GET /admin/config` | `routes/admin.ts` | ✅ |
| `GET /admin/logs` | `routes/admin.ts` | ✅ |
| WebSocket `/ws` | `services/websocket.ts` | ✅ |

### Pendientes

| # | Item | Prioridad | Estado | Archivo | Detalle |
|---|---|---|---|---|---|
| B1 | **Device registration route** | 🔴 | ✅ | `routes/device.ts` | `POST /api/v1/devices` creado + registrado en index.ts |
| B2 | **FCM push real** | 🔴 | ✅ | `services/fcm.ts` | `firebase-admin` instalado, `sendPushNotification()` + `notifyUser()` implementados |
| B3 | **Chat history endpoints** | 🟡 | ❌ No implementar | — | Por especificación: *"El historial de chat no se persiste en el servidor por privacidad"*. Chat funciona solo vía WebSocket + Room local. Endpoints REST eliminados del Android |
| B4 | **Upgrade verification** | 🟡 | ✅ | `services/play.ts` + `routes/plan.ts` actualizado. `googleapis` instalado. Verifica purchaseToken con Google Play Developer API (modo desarrollo: acepta sin verificar) |
| B5 | **Rate limiting** | 🟢 | ✅ | `routes/auth.ts` | `express-rate-limit` aplicado (10 intentos/15min en login/register) |

---

## Fase 3: Android App (Kotlin / Jetpack Compose)

### Pantallas

| Pantalla | ViewModel | Estado |
|---|---|---|
| Splash | — | ✅ |
| Login | `LoginViewModel` | ✅ |
| Register | `RegisterViewModel` | ✅ |
| Home | `HomeViewModel` | ✅ |
| Generar QR | `GenerateQRViewModel` | ✅ |
| Scanner QR (CameraX + ML Kit) | `ScannerViewModel` | ✅ |
| AR Doorbell (cámara + QR) | `CallViewModel` | ✅ (sin modelo 3D — SceneView 2.3.0 incompatible con 2.2.x API, reemplazado por preview cámara + botón) |
| Audio Call (JitsiMeetView embebido) | — | ✅ |
| Video Call (JitsiMeetView embebido) | — | ✅ |
| Waiting (llamando) | — | ✅ |
| Incoming Call | `CallViewModel` | ✅ |
| Chat | `ChatViewModel` | ✅ |
| Profile | `ProfileViewModel` | ✅ |
| Settings | `SettingsViewModel` | ✅ |

### Capas

| Capa | Archivos | Estado |
|---|---|---|
| API (SmartDDApi + interceptor) | 2 | ✅ |
| WebSocket client | 1 | ✅ |
| FCM service | 2 | ✅ |
| Room DB | 3 | ✅ |
| Repositories | 6 | ✅ |
| Hilt DI | 1 | ✅ |
| Navigation | 1 | ✅ |
| Theme | 1 | ✅ |

### Pendientes

| # | Item | Prioridad | Estado | Detalle |
|---|---|---|---|---|---|
| A1 | **`google-services.json`** | 🔴 | ⚠️ Placeholder | `android/app/google-services.json` creado con valores dummy. Reemplazar con valores reales de Firebase Console |
| A2 | **Modelo 3D timbre** | 🟡 | ❌ Eliminado | `ARDoorbellScreen.kt` usaba SceneView 2.2.2 API (`addNode`, `Cube` público, `Node` View-based). SceneView 2.3.0 cambió a API Compose-interna (`addNode` internal, `Cube` constructor private). Se eliminó el modelo 3D, queda preview cámara + botón de timbre |
| A3 | **App icon** (`@mipmap/ic_launcher`) | 🟡 | ✅ | Vector drawable (timbre), adaptive icon XML, background color, placeholders PNG para todas las densidades |
| A4 | **ProGuard rules** | 🟢 | ✅ | `proguard-rules.pro` creado con reglas para Retrofit, OkHttp, Room, Hilt, Jitsi, Firebase, SceneView, ML Kit. Referenciado en `build.gradle.kts` con `isMinifyEnabled = true` |
| A5 | **Audio/Video screens separados** | 🟢 | ✅ | `call/audio/AudioCallScreen.kt` y `call/video/VideoCallScreen.kt` creados con JitsiMeetView embebido vía AndroidView. Audio: UI personalizada (timer, mute, speaker, colgar). Video: overlay de controles sobre JitsiMeetView full-screen. `IncomingCallScreen` y `WaitingScreen` actualizados para navegar a estas screens en lugar de lanzar JitsiMeetActivity |
| A6 | **FCM deep link handling** | 🔴 | ✅ | `MainActivity.kt` + `NavGraph.kt` actualizados para leer extras del Intent y navegar a incoming_call/chat |

### Build APK — Problemas resueltos

| # | Problema | Solución |
|---|---|---|
| G1 | `maven.jitsi.org` DNS no resuelve (muerto) | Reemplazado por `https://github.com/jitsi/jitsi-maven-repository/raw/master/releases` |
| G2 | JitPack 401 para `jitsi-meet-sdk:12.1.3` | Mismo repo Jitsi Maven (G1) |
| G3 | Room KAPT: `kotlinx-metadata` 2.0.0 no soporta metadata 2.1.0/2.2.0 | Migrar Room a KSP (`ksp("androidx.room:room-compiler:2.8.4")`), Room 2.6.1 → 2.8.4 |
| G4 | Hilt KAPT: mismo error de metadata | Hilt 2.51 → 2.55 (incluye `kotlinx-metadata-jvm` actualizado) |
| G5 | SceneView 2.3.3 compilado con Kotlin 2.2.x (metadata 2.2.0) | SceneView 2.3.3 → 2.3.0 (compilado con Kotlin 2.0.21) |
| G6 | AndroidManifest merger conflict con Jitsi SDK | `tools:replace="android:label"` en `<application>`, `tools:replace="android:configChanges,android:theme"` en actividades |
| G7 | Hilt circular dependency: AuthInterceptor ↔ AuthRepository ↔ SmartDDApi | `AuthInterceptor` usa `Lazy<AuthRepository>` en lugar de inyección directa |
| G8 | `provideTokenManager(self-cycle)` en Modules.kt | Eliminar método redundante (`TokenManager` ya tiene `@Inject` constructor) |
| G9 | JitsiMeetSDK no existe en SDK 12.x | Eliminar referencias a `JitsiMeetSDK.setAudioMuted()` — no hay API pública para mute runtime en SDK 12.x |
| G10 | `return` prohibido en lambda de `CameraXQRTracker` | Cambiar `return` → `return@addOnSuccessListener` |
| G11 | `BarcodeScannerOptions` import faltante en ScannerScreen.kt | Agregar `import com.google.mlkit.vision.barcode.BarcodeScannerOptions` |

---

## Fase 4: Admin Panel (React / Vite)

| Item | Estado |
|---|---|
| Dashboard con stats | ✅ |
| Login | ✅ |
| Users CRUD + search/filter | ✅ |
| User detail con QRs y sesiones | ✅ |
| Audit logs | ✅ |
| Build + deploy (puerto 81) | ✅ |
| Global config UI | 🟢 Pendiente |

---

## Fase 5: Finalización

| # | Item | Prioridad | Estado | Detalle |
|---|---|---|---|---|
| F1 | Pruebas de integración | 🟡 | ✅ | `src/__tests__/api.test.ts` con Jest + ts-jest. Tests: health, auth, QR, ring, config, devices. Salta si `TEST_API_URL` no está definido |
| F2 | Pruebas en dispositivos reales | 🟡 | ❌ | APK `app-debug.apk` (240MB) compilado, pendiente instalar y probar flujo completo |
| F3 | APK release + Play Store | 🟢 | ✅ APK split por ABI | `app-arm64-v8a-debug.apk` (94MB), `app-armeabi-v7a-debug.apk` (75MB), `app-x86_64-debug.apk` (96MB). Falta: release signing, google-services.json real |
| F4 | Migraciones Prisma | 🟢 | ✅ | `prisma/migrations/20260702_init/` creada con migration.sql y migration_lock.toml. `prisma validate` y `prisma generate` exitosos. Server: `prisma migrate resolve --applied` por DB preexistente |

---

## Orden de ejecución recomendado

```
Paso 1 (🔴): B1 + B2 + A1 + A6  ✅  Backend devices, FCM real, google-services placeholder, deep links
Paso 2 (🟡): B4 + B5             ✅  Upgrade verification (Google Play) + Rate limiting
Paso 3 (🟡): A3 + A4             ✅  App icon vector + ProGuard rules
Paso 4 (🟢): A5                  ✅  Audio/Video screens separados con JitsiMeetView embebido
Paso 5 (🟢): F4                  ✅  Migraciones Prisma (SQL + lock + generate)
Paso 6 (🟢): F1                  ✅  Tests de integración (Jest + ts-jest)
Paso 7 (🔴): Build APK          ✅  Compilado en servidor (KSP, Hilt 2.55, Room 2.8.4, SceneView 2.3.0)
Paso 8 (🔴): A2                  ⚠️  Modelo 3D eliminado por API incompatible — decidir: restaurar con SceneView 4.x o mantener sin modelo
Paso 9 (🔴): A1                  ❌  google-services.json real desde Firebase Console (FCM no funciona sin él)
Paso 10 (🟡): F2 + F3           →  Pruebas dispositivo real + Play Store
```

---

## Deploy Checklist

### Pre-requisitos (una vez)
- [ ] Firebase Console: crear proyecto `smartdd`, descargar `google-services.json` → `android/app/`
- [ ] Firebase Console: crear service account, descargar clave → `backend/serviceAccountKey.json`
- [ ] Google Play Console: crear service account con permiso "Android Publisher" para verificación de purchases
- [ ] Configurar DNS para `smartdd.com` → IP del servidor
- [ ] Let's Encrypt SSL para `smartdd.com` y subdominios
- [ ] Cloudflare tunnel (opcional) para HTTPS

### Deploy backend
```bash
# En el servidor de producción
git pull origin main
cd backend
npm ci --omit=dev
npx prisma generate
npx prisma migrate deploy
npx tsx src/seed.ts          # Crea admin + test user
npm run build

# Containers
cd ../infra
docker compose down --remove-orphans
docker compose up -d --build

# Verificar
curl http://localhost:8000/api/v1/health
```

### Deploy Android
```bash
cd android
export ANDROID_HOME=$HOME/android-sdk
export ANDROID_SDK_ROOT=$HOME/android-sdk
# Colocar google-services.json REAL antes de build
./gradlew assembleRelease
# APK en: app/build/outputs/apk/release/app-release.apk
```

### Tests de integración
```bash
cd backend
TEST_API_URL=http://localhost:8000/api/v1 npx jest
```

---

## Arquitectura de rutas actual

```
api.smartdd.com (internamente :8000)
  ├── /api/v1/auth/*          → backend (port 3000)
  ├── /api/v1/qr/*            → backend
  ├── /api/v1/ring/*          → backend
  ├── /api/v1/session/*       → backend
  ├── /api/v1/user/*          → backend
  ├── /api/v1/admin/*         → backend
  ├── /ws                     → WebSocket backend
  └── /                       → admin-panel (port 81)

sga-sp.com (puerto 80)        → Apache (proyecto externo)
spi.sga-sp.com (puerto 80)    → Apache
caja.sga-sp.com (puerto 80)   → Apache
evolution.sga-sp.com:80       → Apache → evolution-api:8080
```

---

## Puertos en uso

| Puerto | Servicio | Nota |
|---|---|---|
| 80 | Apache | sga-sp.com, evolution-api |
| 8000 | nginx → signaling (3000) | Nuestra API |
| 81 | nginx → admin-panel (80) | Admin web |
| 82 | nginx → jitsi-web (80) | Jitsi Meet |
| 8081 | jitsi-web (interno) | No expuesto directo |
| 8080 | evolution-api | Apache proxy |
| 6379 | evolution-redis | No nuestro |

---

## Credenciales de desarrollo

Ver `credenciales.md` para detalles completos de usuarios, tokens y acceso a servicios.

---

## Próxima sesión

### Objetivo: Firebase real + pruebas en dispositivo

Dependencias: acceso SSH al servidor (`192.168.100.101`), Firebase Console, dispositivo Android.

### Paso 1 — Firebase Console (30 min)
- [ ] Crear proyecto Firebase `smartdd`
- [ ] Registrar app Android (`com.smartdd.app`) → descargar `google-services.json`
- [ ] Crear service account → generar clave privada JSON → `backend/serviceAccountKey.json`
- [ ] Habilitar Cloud Messaging (FCM) para Android
- [ ] Copiar `google-services.json` → `android/app/google-services.json`
- [ ] Configurar `FIREBASE_SERVICE_ACCOUNT` + `FCM_PROJECT_ID` en servidor

### Paso 2 — Prueba APK en dispositivo (1 h)
- [ ] `scp` del `app-debug.apk` al dispositivo
- [ ] Instalar y probar: registro → login → escanear QR → timbre → llamada audio/video → chat → perfil
- [ ] Verificar notificaciones FCM entrantes (después de google-services.json real)

### Paso 3 — Optimizar tamaño APK (30 min)
- [ ] Agregar `splits { abi { ... } }` o `bundle` en `build.gradle.kts`
- [ ] Verificar reducción de tamaño (240MB → ~60MB por ABI)

### Paso 4 — Release APK (1 h)
- [ ] Crear keystore de release
- [ ] `./gradlew assembleRelease`
- [ ] Probar APK release en dispositivo

### Paso 5 — Play Store (2 h, si aplica)
- [ ] Crear listing en Google Play Console
- [ ] Capturas de pantalla, descripción, icono
- [ ] Subir APK firmado
- [ ] Configurar Play Billing: productos PRO, B2P

### Paso 6 — Post-deploy (30 min)
- [ ] Let's Encrypt SSL
- [ ] Cloudflare tunnel
- [ ] DNS: `api.smartdd.com` → IP servidor
- [ ] Backups automáticos PostgreSQL
- [ ] Monitoreo (health endpoint + cron + alerta)
