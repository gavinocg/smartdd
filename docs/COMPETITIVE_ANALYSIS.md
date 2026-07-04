# Análisis Competitivo — SMARTDD (Smart Ding Dong)

**Versión:** 1.0
**Fecha:** 2026-06-29

---

## 1. Competidores identificados

| App | URL | Modelo | Comunicación | ¿App Emisor? | Geolocalización | RA |
|---|---|---|---|---|---|---|
| **QRBells** | qrbells.com | Free | Audio/Video | No (web) | Sí | ❌ |
| **Darwaza** | doorcalling.com | Free | Voz walkie + Chat | No (web) | No | ❌ |
| **KnockQR** | knockqr.com | Free | Video llamada | No (web) | No | ❌ |
| **DWAR** | dwarbell.com | Free | Video/Audio/Chat | No (web) | No | ❌ |
| **vDoorbell** | vdoorbell.com | Free (requiere Telegram) | Chat/Audio/Video | No (web) | Sí (radio) | ❌ |
| **DoorVi** | doorvi.com | Free | Video llamada | No (web) | No | ❌ |
| **NoBell** | Google Play | Free | Video/Chat | No (web) | Sí | ❌ |
| **QRBELL** | qrbell.co.in | ₹21-101/mes | Video/Audio | No (web) | No | ❌ |
| **Q-Bell** | q-bell.com | £4.95/mes | Notificación + Chat | Sí | Sí | ❌ |
| **RingMi** | ringmi.sudosan.com | Free | Chat/Video | No (web) | Sí | ❌ |
| **VBell** | doorbellio.com | Free | Notificación | No (web) | No | ❌ |
| **Bell** | bellapp.buzz | Free/Pro+ | Notificación | Sí | Sí | ❌ |

---

## 2. Análisis de características

### 2.1 Lo que todos tienen en común
- Generación de QR vinculado a cuenta
- Impresión del QR para colocar en puerta
- Notificación push al Receptor cuando alguien escanea
- Comunicación básica (chat o llamada)
- Sin necesidad de hardware

### 2.2 Lo que NINGÚN competidor tiene
| Característica | SMARTDD | Competidores |
|---|---|---|
| **Realidad Aumentada simulada** | ✅ Botón timbre 3D sobre QR | ❌ |
| **Preview de video antes de responder** | ✅ Receptor ve al Emisor en vivo | ❌ |
| **RA sin ARCore** | ✅ Funciona sin Google Play Services for AR | ❌ (no aplica) |
| **App requerida para Emisor** | ✅ Sí (necesaria para RA) | ❌ (solo web) |
| **Configuración granular de respuesta** | ✅ Default + opciones activables individualmente | ❌ |
| **Jitsi self-hosted** | ✅ Control total de infraestructura | ❌ (usan servicios cloud) |
| **Modelo Free/Pro/B2P** | ✅ 3 planes con proyección | ❌ (solo free o free+pro) |

### 2.3 Diferenciadores clave de SMARTDD

#### Diferenciador 1: Experiencia de Realidad Aumentada
- **Competidores:** El Emisor escanea un QR y es redirigido a una página web estática con un botón 2D
- **SMARTDD:** El Emisor escanea el QR y ve un timbre virtual 3D flotando sobre el QR en el mundo real, con animaciones y feedback háptico
- **Valor:** Experiencia inmersiva e innovadora que genera engagement y diferenciación de marca

#### Diferenciador 2: Preview de video del Emisor
- **Competidores:** El Receptor recibe solo nombre/foto del visitante
- **SMARTDD:** El Receptor ve en vivo la cámara frontal del Emisor antes de decidir si responder
- **Valor:** Seguridad y control total sobre quién contacta al Receptor

#### Diferenciador 3: Control granular de respuesta
- **Competidores:** Modo fijo (solo llamada o solo chat)
- **SMARTDD:** El Receptor configura por defecto y activa/desactiva individualmente Chat, Audio y Audio+Video
- **Valor:** Flexibilidad total para el usuario

#### Diferenciador 4: Infraestructura propia
- **Competidores:** Dependencia de servicios cloud de terceros
- **SMARTDD:** Jitsi self-hosted en servidor propio, control total de datos y privacidad
- **Valor:** Cumplimiento de regulaciones de privacidad, sin costos recurrentes de infraestructura de terceros

---

## 3. Análisis de precios del mercado

### 3.1 Precios de competidores

| App | Plan Gratuito | Plan Pago | Precio |
|---|---|---|---|
| **QRBells** | Completo (sin límite aparente) | No tiene | $0 |
| **Darwaza** | Completo ("free forever") | No tiene | $0 |
| **KnockQR** | Básico | No encontrado | ¿? |
| **DWAR** | Completo ("free forever") | No tiene | $0 |
| **vDoorbell** | Completo (usa Telegram) | No tiene | $0 |
| **DoorVi** | Completo | Stickers físicos | $ |
| **NoBell** | Completo | No tiene | $0 |
| **QRBELL** | Básico | Pro | ₹21-101/mes (~$0.25-$1.20) |
| **Q-Bell** | No tiene | Solo pago | £4.95/mes (~$6.30) |
| **RingMi** | Completo | No tiene | $0 |
| **VBell** | Completo | Stickers físicos | $ |
| **Bell** | Básico | Pro+ | Por definir |

### 3.2 Observaciones del mercado

- **El 70% de los competidores son completamente gratuitos** sin plan pago
- **El precio más alto** entre competidores directos es Q-Bell (~$6.30/mes)
- **El precio de QRBELL Pro** (~$1.20/mes) es extremadamente bajo (mercado indio)
- **Ningún competidor** ha validado un precio alto para este tipo de servicio
- **El mercado de timbres virtuales es emergente** pero con baja disposición a pagar en el segmento individual

### 3.3 Recomendación de precios SMARTDD

| Plan | Precio | Público |
|---|---|---|
| **Free** | $0 | Usuarios individuales (1 QR, 1 disp.) |
| **Pro** | $3.99/mes o $39.99/año | Usuarios avanzados |
| **B2P** (futuro) | $19.99-$199.99/mes | Negocios |

---

## 4. Análisis FODA

### Fortalezas
- Experiencia de RA única en el mercado
- Infraestructura auto-gestionada (control total)
- Preview de video del Emisor (seguridad)
- Modelo de negocio flexible (Free/Pro/B2P)
- Sin dependencia de hardware externo
- Costos de infraestructura extremadamente bajos

### Debilidades
- Marca desconocida (nueva en el mercado)
- Requiere que el Emisor tenga la app instalada (barrera de entrada)
- Sin red de usuarios establecida
- Dependencia de Jitsi Meet (proyecto open-source mantenido por 8x8)
- El preview de video requiere permisos de cámara

### Oportunidades
- Mercado de timbres virtuales en crecimiento (smart home, delivery, food courts)
- Tendencia hacia soluciones sin contacto y digitales
- Nicho B2P: patios de comida, restaurantes, clínicas, talleres
- Posible integración con sistemas de colas virtuales
- Expansión a iOS después del MVP Android
- API blanda para terceros (integración con apps de delivery, logística)

### Amenazas
- Competidores gratuitos establecidos (QRBells, Darwaza)
- Gigantes tecnológicos podrían entrar (Google, Amazon con Ring)
- Baja barrera de entrada técnica (cualquiera puede hacer un QR + notificación)
- Si Jitsi cambia su licencia o modelo de negocio
- Regulaciones de privacidad y geolocalización (GDPR, LGPD)

---

## 5. Conclusión

SMARTDD compite en un mercado con **múltiples jugadores gratuitos** pero con **ningún competidor ofreciendo RA**. La ventaja competitiva principal es la experiencia de **realidad aumentada simulada con botón 3D superpuesto sobre el QR**, que transforma el acto de escanear de una experiencia web plana a una interacción inmersiva.

### Recomendación estratégica

1. **Diferenciación por RA:** La RA debe ser el centro del marketing y la propuesta de valor
2. **Modelo freemium agresivo:** El Free debe ser suficientemente bueno para atraer usuarios masivos
3. **B2P como negocio real:** El segmento individual es difícil de monetizar; el crecimiento real está en B2P
4. **Alianzas estratégicas:** Food courts, centros comerciales, edificios de oficinas como clientes B2P iniciales
5. **Métrica de éxito:** No son los usuarios registrados, sino los QRs impresos y colocados físicamente
