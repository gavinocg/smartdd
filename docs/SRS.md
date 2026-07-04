# SRS — Software Requirements Specification
## SMARTDD (Smart Ding Dong)

**Versión:** 1.0
**Fecha:** 2026-06-29
**Autor:** Equipo SMARTDD

---

## 1. Introducción

### 1.1 Propósito

Este documento especifica los requisitos funcionales y no funcionales de SMARTDD (Smart Ding Dong), una aplicación Android que permite establecer un vínculo de comunicación seguro entre un usuario receptor y múltiples emisores mediante un código QR con realidad aumentada simulada. El objetivo es reemplazar los timbres convencionales y sistemas de notificación presencial por un medio digital innovador.

### 1.2 Alcance

El sistema comprende una aplicación Android, un servidor de señalización backend, un panel de administración web, e infraestructura de comunicación WebRTC auto-gestionada. El documento cubre la versión MVP (Producto Mínimo Viable) con proyección a futuro B2P.

### 1.3 Definiciones y acrónimos

| Término | Definición |
|---|---|
| **QR** | Código de respuesta rápida (Quick Response). Matriz bidimensional que almacena información |
| **RA** | Realidad Aumentada. Superposición de elementos virtuales sobre el mundo real |
| **RA Simulada** | Técnica que superpone modelos 3D sobre el preview de cámara sin usar ARCore, basada en detección de marcadores QR |
| **WebRTC** | Web Real-Time Communication. Protocolo para comunicación peer-to-peer de audio/video |
| **Receptor** | Usuario que genera el QR, recibe notificaciones de timbre y responde a las solicitudes |
| **Emisor** | Usuario que escanea el QR, ve el timbre virtual 3D y se comunica con el Receptor |
| **P2P** | Peer-to-Peer. Comunicación directa entre dispositivos |
| **B2P** | Business to Person. Modelo de negocio donde el negocio se comunica con el cliente |
| **Jitsi Meet** | Plataforma open-source de videoconferencia WebRTC (Apache 2.0) |
| **TURN** | Traversal Using Relays around NAT. Protocolo para relay de tráfico WebRTC cuando P2P no es posible |
| **STUN** | Session Traversal Utilities for NAT. Protocolo para descubrir dirección pública en WebRTC |
| **JVB** | Jitsi Video Bridge. Componente que gestiona el streaming de video multiparte |

### 1.4 Referencias

- IEEE 830-1998 — Recommended Practice for Software Requirements Specifications
- Android SDK Documentation (API 26+)
- Jitsi Meet SDK Documentation (Apache 2.0)
- WebRTC RFC 8825-8830
- Cloudflare Tunnel Documentation
- QR Code ISO/IEC 18004

### 1.5 Resumen del documento

La sección 2 describe la perspectiva general del producto y sus actores. La sección 3 detalla los requisitos funcionales. La sección 4 cubre los requisitos no funcionales. La sección 5 describe las interfaces. La sección 6 detalla los atributos de calidad y restricciones de diseño. La sección 7 especifica la infraestructura Docker.

---

## 2. Descripción General

### 2.1 Perspectiva del producto

SMARTDD es una aplicación Android independiente que reemplaza los timbres convencionales por un sistema digital. El flujo principal es:

1. El Receptor genera un código QR vinculado a su cuenta y geolocalización actual
2. El Receptor imprime el QR y lo coloca en un lugar visible (puerta, recepción, etc.)
3. El Emisor escanea el QR con la aplicación
4. La app valida que el Emisor esté dentro del rango de geolocalización
5. La app muestra un timbre virtual 3D superpuesto sobre el QR mediante RA simulada
6. El Emisor presiona el timbre virtual
7. El Emisor activa su cámara frontal automáticamente (preview de video)
8. El Receptor recibe la notificación con el video preview en vivo
9. El Receptor responde (Chat/Audio/Audio+Video) o Rechaza

**Visión futura B2P:** El sistema está diseñado con proyección para que negocios (patios de comida, servicios, restaurantes) puedan contratar una suscripción que les permita generar QRs ilimitados para que sus clientes (Emisores) escaneen y el negocio pueda notificar cuando un bien/servicio está listo para retiro/entrega. La base de datos y la arquitectura contemplan esta extensión desde el diseño inicial.

### 2.2 Funcionalidad del producto

**Flujo general:**

```
Receptor                    Servidor                    Emisor
    │                          │                          │
    ├─ Genera QR (lat/lng) ──► │                          │
    │◄── QR imagen ────────────┤                          │
    │ Imprime QR               │                          │
    │                          │                          │
    │                          │◄── Escanea QR ───────────┤
    │                          │── Valida geolocalización │
    │                          ├── QR válido ────────────►│
    │                          │                          ├─ Ve timbre 3D en RA
    │                          │                          ├─ Presiona timbre
    │                          │◄── Ring request ─────────┤
    │                          ├── Crea sala Jitsi UUID  │
    │◄── Notificación ring ────┤                          │
    │                          ├── Room ID ──────────────►│
    │                          │                          ├─ Activa cámara frontal
    │                          │                          └─ Join sala (video ON)
    │ Join sala (video OFF) ───┤                          │
    │ Preview Emisor ◄─────── P2P WebRTC ────────────────►│
    │                          │                          │
    ├─ Responde/Rchaza ──────► │                          │
    │                          ├── Response ─────────────►│
    │                          │                          │
    ├─ Chat/ Audio/Video ──── P2P WebRTC ────────────────►│
```

### 2.3 Características de usuarios

| Actor | Descripción | Roles |
|---|---|---|
| **Receptor** | Usuario dueño del QR. Puede ser dueño de casa, negocio, o cualquier persona que quiera recibir visitas | Free (1 QR, 1 dispositivo), Pro (ilimitado) |
| **Emisor** | Usuario que escanea el QR para contactar al Receptor. No requiere cuenta (solo app instalada para la experiencia RA) | Sin cuenta, usa la app |
| **Administrador** | Gestiona usuarios, planes (free/pro) y configuración global del sistema | Acceso web al admin panel |
| **Cliente B2P** (futuro) | Negocio suscriptor que genera QRs masivos para sus clientes | Plan B2P, panel web |

### 2.4 Restricciones generales

- Plataforma: Android API 26+ (Android 8.0 Oreo)
- RAM mínima: 2GB
- Dispositivos objetivo: Gama media y alta
- Cámara funcional con autoenfoque
- GPS funcional
- Conexión a internet (mínimo 3G estable)
- No requiere ARCore (RA simulada basada en detección QR)
- No requiere hardware externo ni instalación física

### 2.5 Suposiciones y dependencias

- El servicio de Jitsi Meet es auto-gestionado (self-hosted) en el servidor del equipo
- El servidor de señalización se ejecuta en Ubuntu 26.04 con Docker
- La conexión a internet es responsabilidad del usuario
- No se garantiza comunicación en redes corporativas restrictivas sin TURN
- El servicio público meet.jit.si NO se utiliza para producción

---

## 3. Requisitos Funcionales

### RF1 — Generación de código QR

**Descripción:** El Receptor genera un código QR único vinculado a su cuenta, geolocalización y dispositivos.

**Entradas:**
- Ubicación actual (latitud, longitud) obtenida del GPS del dispositivo
- Radio de validez en metros (configurable, default 50m)

**Proceso:**
- El servidor genera un UUID v4 único para el QR
- Asocia el QR al ID del Receptor, latitud, longitud y radio
- Genera una imagen PNG del QR con branding SMARTDD
- Verifica límite según plan del usuario

**Salidas:**
- Imagen del código QR (PNG/SVG)
- URL única del QR: `https://smartdd.com/qr/{uuid}`
- Datos del QR almacenados en base de datos

**Reglas de negocio:**
- Free: máximo 1 QR activo por cuenta
- Pro: cantidad ilimitada de QRs
- Un QR desactivado no puede ser escaneado
- El QR incluye metadatos en formato JSONB para futura expansión B2P

**Criterios de aceptación:**
- El QR generado es escaneable por cualquier lector QR estándar
- La imagen QR incluye identificador visual de SMARTDD
- El QR se puede descargar y compartir desde la app

---

### RF2 — Escaneo de QR con validación de geolocalización

**Descripción:** El Emisor escanea el código QR con la cámara de su dispositivo y la app valida que se encuentre efectivamente en la ubicación del QR.

**Entradas:**
- Imagen del QR capturada por la cámara
- Ubicación actual del Emisor (latitud, longitud)

**Proceso:**
- La app usa ML Kit Barcode Scanning para detectar y decodificar el QR
- Extrae el UUID del QR
- Envía al servidor: `POST /api/v1/qr/{uuid}/validate` con la ubicación del Emisor
- El servidor calcula la distancia geodésica entre la ubicación del QR y la del Emisor
- Si la distancia ≤ radio permitido → QR válido
- Si la distancia > radio permitido → QR inválido, se notifica al Emisor

**Salidas:**
- QR válido: permite continuar al paso de RA
- QR inválido: muestra mensaje "Debes estar en la ubicación del QR para continuar"

**Reglas de negocio:**
- El QR debe estar activo en base de datos
- El Emisor debe conceder permiso de ubicación
- Timeout de validación: 5 segundos

**Criterios de aceptación:**
- El escaneo funciona con QR impreso y en pantalla digital
- El escaneo funciona en condiciones de poca luz (con flash)
- La validación de geolocalización rechaza escaneos fuera del radio

---

### RF3 — Realidad Aumentada simulada (timbre virtual 3D)

**Descripción:** Al validar el QR, la app superpone un modelo 3D de un botón timbre sobre el código QR en el preview de la cámara. El Emisor puede presionar el botón virtual para iniciar la comunicación.

**Entradas:**
- Stream de la cámara trasera
- Coordenadas del QR detectado en el preview

**Proceso:**
- La app detecta la posición, tamaño y orientación del QR en el preview de cámara
- Usando SceneView (OpenGL), renderiza un modelo 3D de un botón timbre exactamente sobre la posición del QR
- El modelo 3D reemplaza visualmente al QR (el QR queda oculto detrás del botón 3D)
- El botón tiene animación pulsante/brillante para indicar que es interactivo
- El Emisor toca el botón en pantalla
- El botón responde con animación de hundimiento (press effect)
- Se envía `POST /api/v1/ring` al servidor

**Salidas:**
- Visualización del timbre 3D superpuesto en tiempo real
- Evento de "ring" enviado al servidor al presionar

**Reglas de negocio:**
- El botón 3D solo aparece cuando el QR está centrado y enfocado
- Si el QR sale del campo de visión, el botón desaparece y el QR se vuelve a mostrar
- El modelo 3D debe ser liviano (<500KB) para renderizado fluido en gama media

**Criterios de aceptación:**
- El botón 3D se renderiza suavemente (~30fps mínimo)
- La animación de presionar es responsiva (<100ms de latencia)
- No se requiere ARCore ni Google Play Services for AR

---

### RF4 — Solicitud de comunicación (Ring) con preview de video

**Descripción:** Al presionar el timbre virtual, el Emisor activa su cámara frontal automáticamente y se establece un preview de video unidireccional (Emisor → Receptor). El Receptor ve en vivo quién está timbrando y puede decidir responder o rechazar.

**Sub-flujo:**
1. Emisor presiona timbre 3D
2. Servidor crea una sesión con UUID de sala Jitsi
3. Servidor notifica al Receptor vía WebSocket + push notification
4. Emisor activa cámara frontal y se une a la sala Jitsi con video=ON, audio=OFF, mic=OFF
5. Receptor recibe notificación y se une a la sala Jitsi con video=OFF, audio=OFF, mic=OFF
6. Receptor ve el video en vivo del Emisor (preview unidireccional)
7. El preview se mantiene activo mientras el Receptor no decida

**Entradas:**
- ID del QR (del escaneo)
- ID del Emisor (sesión anónima o autenticada)
- Token FCM del Receptor (para notificación push)

**Proceso:**
- `POST /api/v1/ring` → servidor crea `ring_session` con estado `pending`
- Servidor envía evento WebSocket `incoming_ring` al Receptor con: roomId, emisorInfo, previewVideo: true
- Si el Receptor no tiene WebSocket activo, se envía notificación push FCM

**Salidas:**
- Receptor: pantalla de "llamada entrante" con preview de video
- Emisor: pantalla de "esperando respuesta" con su video frontal

**Reglas de negocio:**
- El preview de video se mantiene hasta que el Receptor responda o la sesión expire
- Timeout de preview: 60 segundos (configurable)
- El Emisor no puede ver al Receptor durante el preview
- El Emisor puede cancelar la solicitud en cualquier momento

**Criterios de aceptación:**
- La latencia del preview es <500ms
- La notificación push llega en <5 segundos (condición normal de red)
- El preview se corta si el Emisor cancela

---

### RF4.1 — Configuración del Receptor

**Descripción:** El Receptor puede configurar cómo desea responder a las solicitudes de timbre.

**Entradas:**
- Modo de respuesta por defecto: Chat, Audio, o Audio+Video
- Opciones habilitadas: checkboxes independientes para Chat, Audio, Audio+Video

**Proceso:**
- El Receptor accede a la pantalla de Configuración
- Selecciona el modo por defecto (radio button)
- Activa/desactiva opciones de respuesta (toggle switches)
- Guarda la configuración → `PUT /api/v1/user/config`

**Salidas:**
- Configuración almacenada en base de datos (tabla `user_configs`)
- La pantalla de Incoming Call se ajusta según la configuración

**Reglas de negocio:**
- Al menos una opción debe estar habilitada
- Si la opción por defecto se deshabilita, se selecciona automáticamente la primera opción disponible

**Criterios de aceptación:**
- Los cambios se reflejan inmediatamente en la próxima solicitud entrante
- La interfaz de Incoming Call solo muestra los botones de opciones habilitadas

---

### RF4.2 — Respuesta del Receptor

**Descripción:** El Receptor responde a la solicitud de timbre eligiendo entre las opciones habilitadas, o rechaza la comunicación.

**Entradas:**
- ID de sesión (ring_session)
- Acción: `accept` o `reject`
- Modo (si acepta): `chat`, `audio`, o `video`

**Proceso:**
- Receptor presiona uno de los botones en la pantalla Incoming Call
- `POST /api/v1/respond` con `{ sessionId, action, mode }`
- Servidor actualiza estado de la sesión y notifica al Emisor vía WebSocket

**Si action=accept:**
- Chat: ambos abandonan sala Jitsi, se abre interfaz de chat de texto
- Audio: `setAudioMuted(false)` en ambos, permanecen en sala Jitsi
- Audio+Video: `setAudioMuted(false)` + `setVideoMuted(false)` en ambos, permanecen en sala Jitsi

**Si action=reject:**
- Ambos abandonan sala Jitsi
- Emisor ve pantalla "Solicitud rechazada"

**Salidas:**
- Sesión actualizada en base de datos
- Comunicación establecida o rechazada según la acción

**Reglas de negocio:**
- El modo de respuesta debe ser uno de los habilitados en la configuración del Receptor
- Si el Emisor ya canceló, el servidor responde con error "Sesión expirada"

**Criterios de aceptación:**
- La transición de preview a comunicación completa es <500ms
- El chat de texto se abre instantáneamente (sin recargar WebRTC)
- Al rechazar, ambas partes reciben notificación inmediata

---

### RF5 — Gestión de cuenta y planes

**Descripción:** Los usuarios pueden registrarse, iniciar sesión, y gestionar su plan (Free/Pro). El plan determina los límites de QRs y dispositivos.

**Entradas:**
- Registro: nombre, email, contraseña
- Login: email, contraseña

**Proceso:**
- El usuario se registra con email y contraseña
- El servidor crea la cuenta con plan Free por defecto
- El usuario puede actualizar a Pro mediante Google Play Billing
- El servidor verifica el plan en cada operación que tenga límites

**Planes:**

| Característica | Free | Pro |
|---|---|---|
| QRs activos | 1 | Ilimitados |
| Dispositivos | 1 | Ilimitados |
| Preview de video | Sí | Sí |
| Configuración de respuesta | Sí | Sí |
| Soporte | Email | Prioritario |

**Salidas:**
- Token JWT para autenticación
- Perfil de usuario con plan actual

**Reglas de negocio:**
- El límite de Free se verifica en el servidor, no en el cliente
- Al cambiar de Free a Pro, se liberan todos los límites inmediatamente
- Al cambiar de Pro a Free (cancelación), se debe desactivar QRs hasta dejar 1

**Criterios de aceptación:**
- Un usuario Free no puede generar más de 1 QR activo
- Un usuario Pro puede generar QRs sin límite
- La cancelación de Pro desactiva QRs adicionales automáticamente

---

### RF6 — Panel de Administración

**Descripción:** Panel web para que el administrador del sistema gestione usuarios, planes y configuración global.

**Funcionalidades:**

| Módulo | Descripción |
|---|---|
| **Dashboard** | Cards con estadísticas: usuarios totales, activos, plan free vs pro, QRs generados hoy |
| **Usuarios** | Tabla con búsqueda y filtros. Acciones: cambiar plan (free/pro), suspender, eliminar |
| **QRs** | Vista de todos los QRs generados, filtro por usuario, posibilidad de desactivar |
| **Configuración global** | Radio de geolocalización default, timeout de sesión, etc. |
| **Auditoría** | Log de acciones del admin con timestamp y detalle |

**Acceso:**
- Ruta protegida con rol `admin`
- Autenticación con JWT + middleware de rol
- Subdominio: `admin.smartdd.com`

**Salidas:**
- Interfaz web responsiva para gestión de la plataforma

**Criterios de aceptación:**
- Solo usuarios con rol `admin` pueden acceder
- Las acciones quedan registradas en log de auditoría
- Cambiar un usuario de Free a Pro se refleja inmediatamente

---

### RF7 — Visión futura: API B2P (referencia conceptual)

**Descripción:** A futuro, SMARTDD ofrecerá un plan B2P para negocios (patios de comida, restaurantes, servicios) que permita generar QRs ilimitados para que los clientes (Emisores) escaneen y el negocio pueda comunicar que el bien/servicio está listo.

**Concepto:**
- El negocio se suscribe al plan B2P
- Genera QRs para cada mesa, puesto o servicio
- Cliente escanea QR → toca timbre → negocio recibe notificación "Cliente en mesa X espera"
- Cuando el bien/servicio está listo, el negocio se comunica con el cliente vía Chat/Audio/Notificación

**Diseño preparado desde el inicio:**
- Tabla `qr_codes` con campo `metadata` (JSONB) para futuros datos como `table_number`, `business_id`
- JWT con campo `role` soportando `business`
- Tabla `plans` desacoplada con seed data: `free`, `pro`, `b2p`
- Endpoints versionados (`/api/v1/`) para añadir v2 sin romper v1

---

## 4. Requisitos No Funcionales

### 4.1 Seguridad

| Requisito | Descripción |
|---|---|
| **RNF-SEG-01** | Toda comunicación WebRTC debe usar cifrado DTLS/SRTP (obligatorio en WebRTC) |
| **RNF-SEG-02** | Las contraseñas deben almacenarse con hash bcrypt (costo 12) |
| **RNF-SEG-03** | Los tokens JWT deben expirar en 24 horas (refresh token en 7 días) |
| **RNF-SEG-04** | El servidor de señalización debe validar todos los inputs (sanitización) |
| **RNF-SEG-05** | No se deben exponer direcciones IP ni información sensible entre Emisor y Receptor |
| **RNF-SEG-06** | La geolocalización se usa solo para validar rango, no se comparte entre usuarios |
| **RNF-SEG-07** | Las salas Jitsi usan UUIDv4 como nombre, no predecible por terceros |

### 4.2 Privacidad

| Requisito | Descripción |
|---|---|
| **RNF-PRI-01** | No se almacenan conversaciones de audio/video en el servidor |
| **RNF-PRI-02** | Los mensajes de chat se almacenan solo localmente en el dispositivo |
| **RNF-PRI-03** | La geolocalización no se persiste después de validar el QR |
| **RNF-PRI-04** | El preview de video es en vivo, no se graba ni almacena |
| **RNF-PRI-05** | El Receptor puede bloquear a un Emisor (futuro) |

### 4.3 Rendimiento

| Requisito | Descripción |
|---|---|
| **RNF-REN-01** | La app debe funcionar fluidamente en dispositivos con 2GB RAM |
| **RNF-REN-02** | El renderizado del botón 3D debe mantener mínimo 30fps |
| **RNF-REN-03** | La latencia de preview de video debe ser <500ms |
| **RNF-REN-04** | La app debe cargar en <3 segundos en conexión 4G |
| **RNF-REN-05** | El escaneo QR debe detectar en <1 segundo |

### 4.4 Usabilidad

| Requisito | Descripción |
|---|---|
| **RNF-USA-01** | El flujo Emisor requiere máximo 3 taps para llegar al timbre 3D |
| **RNF-USA-02** | El flujo Receptor requiere máximo 2 taps para responder/rechazar |
| **RNF-USA-03** | La interfaz debe ser intuitiva, sin necesidad de tutorial |
| **RNF-USA-04** | Los botones de respuesta deben ser grandes y claramente etiquetados |
| **RNF-USA-05** | Debe haber feedback visual inmediato para cada acción del usuario |

### 4.5 Disponibilidad

| Requisito | Descripción |
|---|---|
| **RNF-DIS-01** | El servidor de señalización debe tener uptime >99% |
| **RNF-DIS-02** | Las notificaciones push deben entregarse en <10 segundos |
| **RNF-DIS-03** | El sistema debe manejar al menos 100 solicitudes simultáneas en etapa inicial |
| **RNF-DIS-04** | Backup diario de la base de datos PostgreSQL |

### 4.6 Concurrencia

| Requisito | Descripción |
|---|---|
| **RNF-CON-01** | Varios Emisores pueden timbrar simultáneamente al mismo Receptor |
| **RNF-CON-02** | El Receptor debe ver una cola de solicitudes entrantes (futuro) |
| **RNF-CON-03** | La app debe manejar correctamente la reconexión WebSocket |

---

## 5. Interfaces

### 5.1 Interfaz de usuario (UI)

**Pantallas de la aplicación Android:**

| Pantalla | Descripción |
|---|---|
| **Splash** | Logo SMARTDD, carga inicial, verificación de sesión |
| **Onboarding** | 3 slides explicativos del funcionamiento (opcional, primera vez) |
| **Login** | Email + contraseña, botón "Registrarse", "Olvidé contraseña" |
| **Registro** | Nombre, email, contraseña, confirmar contraseña |
| **Home (Receptor)** | Lista de QRs generados, botón "Generar nuevo QR" (respetando límite), icono de perfil, acceso a configuración |
| **Home (Emisor)** | Botón grande "Escanear QR" que abre la cámara |
| **Generar QR** | Preview del QR generado, botones "Descargar", "Compartir", "Listo" |
| **Escáner QR** | Cámara fullscreen con marco guía. Al detectar QR, vibración + indicador visual |
| **Timbre RA** | Preview de cámara con botón 3D superpuesto. Instrucción: "Presiona el timbre para llamar" |
| **Esperando respuesta (Emisor)** | Video de cámara frontal en pequeño, indicador "Llamando...", botón "Cancelar" |
| **Incoming Call (Receptor)** | Video preview del Emisor (grande), botones grandes: Chat, Audio, Video, Rechazar. Nombre/ID del Emisor |
| **Chat** | Burbujas de mensajes, input de texto, botón enviar. Nombre del contacto arriba |
| **Audio Call** | Foto de perfil grande, temporizador, botón "Colgar", indicador de audio activo |
| **Video Call** | Video del otro lado (grande), video propio (picture-in-picture), botones: mute, cambiar cámara, colgar |
| **Configuración** | Selector de modo default (radio), 3 toggles para opciones habilitadas |
| **Perfil** | Nombre, email, plan actual, botón "Actualizar a Pro" |
| **Upgrade Pro** | Cards de planes (mensual/anual), precios, botón "Suscribirse" integrado con Google Play Billing |

### 5.2 Interfaz de hardware

| Componente | Uso |
|---|---|
| **Cámara trasera** | Escaneo de QR y visualización de RA |
| **Cámara frontal** | Preview de video (Emisor) y videollamada |
| **GPS** | Validación de geolocalización |
| **Micrófono** | Llamadas de audio |
| **Altavoz** | Reproducción de audio de llamadas |
| **Internet** | Comunicación con servidor y WebRTC |
| **Vibración** | Feedback háptico al escanear QR y presionar timbre |

### 5.3 Interfaz de comunicación

| Protocolo | Uso | Puerto |
|---|---|---|
| **HTTPS** | API REST (signaling server) | 443 (Tunnel) |
| **WebSocket (WSS)** | Eventos en tiempo real | 443 (Tunnel) |
| **WebRTC (DTLS/SRTP)** | Audio/video P2P | Aleatorio (P2P), 10000/udp (TURN) |
| **XMPP** | Señalización interna Jitsi | 5222 (solo contenedor) |
| **TCP** | Conexión Jitsi JVB | Puerto negociado |

---

## 6. Atributos de calidad y restricciones de diseño

### 6.1 RA Simulada (no ARCore)

- No se utiliza ARCore para maximizar compatibilidad
- La RA se implementa detectando el QR con ML Kit y superponiendo el modelo 3D usando SceneView (OpenGL)
- El modelo 3D se ancla a las coordenadas del QR en el preview de cámara
- El modelo 3D debe estar en formato GLB/GLTF, optimizado para móvil (<500KB, <5k polígonos)

### 6.2 Offline parcial

- El escaneo de QR funciona sin internet (detección local en el dispositivo)
- La validación de geolocalización requiere internet (verificación en servidor)
- La comunicación requiere internet
- La app muestra estado de conexión al usuario

### 6.3 Escalabilidad

- Arquitectura preparada para migrar a VPS en el futuro
- Base de datos normalizada con índices en campos de búsqueda
- Endpoints versionados para compatibilidad hacia atrás
- El Jitsi JVB es el componente que más recursos consume; puede escalarse horizontalmente

### 6.4 Stack tecnológico

| Componente | Tecnología |
|---|---|
| **Backend** | Node.js + TypeScript + Express.js |
| **Base de datos** | PostgreSQL 16 + Prisma ORM |
| **Cache/Pub-Sub** | Redis 7 |
| **Video/Audio** | Jitsi Meet self-hosted (Docker) |
| **Android** | Kotlin + Jetpack Compose |
| **RA** | ML Kit + SceneView (OpenGL) |
| **WebSocket** | ws (nativo Node.js) |
| **Proxy** | nginx |
| **Tunnel** | Cloudflare Tunnel (cloudflared) |
| **Admin Panel** | React + Vite (futuro Next.js) |
| **Notificaciones** | Firebase Cloud Messaging |
| **Pagos** | Google Play Billing Library |
| **Infraestructura** | Docker Compose en Ubuntu 26.04 |

---

## 7. Infraestructura Docker

### 7.1 Servicios

| Contenedor | Imagen | Función |
|---|---|---|
| **postgres** | `postgres:16-alpine` | Base de datos relacional |
| **redis** | `redis:7-alpine` | Cache, pub/sub, rate limiting |
| **signaling** | `node:22-alpine` (build propio) | API REST + WebSocket server |
| **admin-panel** | React build + nginx | Panel de administración web |
| **nginx** | `nginx:alpine` | Proxy reverso para todos los servicios HTTP |
| **jitsi-web** | `jitsi/web:latest` | Interfaz web de Jitsi Meet |
| **jitsi-prosody** | `jitsi/prosody:latest` | Servidor XMPP |
| **jitsi-jicofo** | `jitsi/jicofo:latest` | Focus / orquestador de salas |
| **jitsi-jvb** | `jitsi/jvb:latest` | Video Bridge (streaming WebRTC) |
| **cloudflared** | `cloudflare/cloudflared` | Tunnel Cloudflare |

### 7.2 Redes

- Red interna `smartdd-net` (bridge)
- Todos los contenedores se comunican por nombre de servicio
- Solo nginx y JVB tienen puertos expuestos al host

### 7.3 Volúmenes

- `pgdata` → datos de PostgreSQL
- `jitsi-config` → configuración de Prosody y Jitsi
- `jitsi-recordings` → grabaciones (si se habilitan)
- `redis-data` → persistencia de Redis
- `uploads` → archivos subidos por usuarios (opcional)

### 7.4 Subdominios

| Subdominio | Destino |
|---|---|
| `api.smartdd.com` | nginx → signaling:3000 |
| `admin.smartdd.com` | nginx → admin-panel:80 |
| `jitsi.smartdd.com` | nginx → jitsi-web:80 |

---

## Historial de revisiones

| Versión | Fecha | Cambios |
|---|---|---|
| 1.0 | 2026-06-29 | Versión inicial. MVP con RF1-RF6. Arquitectura Docker completa. Visión B2P documentada conceptualmente. |
