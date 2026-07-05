import { appendFileSync, existsSync, mkdirSync, readFileSync, unlinkSync } from "fs";
import { join } from "path";

const LOG_DIR = process.env.RING_LOG_DIR || "/tmp";
const LOG_FILE = join(LOG_DIR, "ring_handshake.log");
const MAX_LINES = 500;

function ensureLogFile() {
  const dir = LOG_DIR;
  if (!existsSync(dir)) mkdirSync(dir, { recursive: true });
}

function trimLog() {
  if (!existsSync(LOG_FILE)) return;
  const lines = readFileSync(LOG_FILE, "utf-8").split("\n");
  if (lines.length > MAX_LINES) {
    const trimmed = lines.slice(lines.length - MAX_LINES).join("\n");
    writeLogRaw(trimmed);
  }
}

function writeLogRaw(content: string) {
  ensureLogFile();
  try { appendFileSync(LOG_FILE, content); } catch {}
}

function timestamp() {
  return new Date().toISOString();
}

function log(entry: Record<string, unknown>) {
  const line = JSON.stringify({ ts: timestamp(), ...entry }) + "\n";
  writeLogRaw(line);
  trimLog();
  console.log("[RingLog]", JSON.stringify(entry));
}

export const RingLogger = {
  // 1. Emisor inicia timbre
  ringInitiated(qrUuid: string, emisorId: string, receptorId: string) {
    log({ event: "ring_initiated", qrUuid, emisorId, receptorId });
  },

  // 2. Sesión creada
  sessionCreated(sessionId: string, roomId: string, emisorName: string | null) {
    log({ event: "session_created", sessionId, roomId, emisorName: emisorName || "Visitante" });
  },

  // 3. WebSocket enviado al receptor
  wsSentToReceptor(sessionId: string, receptorId: string, wsConnected: boolean) {
    log({ event: "ws_sent_to_receptor", sessionId, receptorId, wsConnected });
  },

  // 4. Notificación push enviada
  pushSent(sessionId: string, receptorId: string, deviceCount: number) {
    log({ event: "push_sent", sessionId, receptorId, deviceCount });
  },

  // 5. Sesión existente encontrada (409)
  existingSession(sessionId: string, status: string, emisorId: string) {
    log({ event: "existing_session", sessionId, status, emisorId });
  },

  // 6. Receptor recibe incoming_ring (vía WS)
  receptorNotified(sessionId: string, receptorId: string) {
    log({ event: "receptor_notified", sessionId, receptorId });
  },

  // 7. Receptor responde
  receptorResponded(sessionId: string, action: string, mode: string | null) {
    log({ event: "receptor_responded", sessionId, action, mode });
  },

  // 8. Emisor notificado de la respuesta
  emisorNotified(sessionId: string, emisorId: string, action: string) {
    log({ event: "emisor_notified", sessionId, emisorId, action });
  },

  // 9. Timeout
  sessionTimedOut(sessionId: string) {
    log({ event: "session_timeout", sessionId });
  },

  // 10. Error
  error(stage: string, sessionId: string | null, message: string) {
    log({ event: "error", stage, sessionId, message });
  },

  // Leer log
  getLog(): string {
    if (!existsSync(LOG_FILE)) return "[]";
    try {
      const raw = readFileSync(LOG_FILE, "utf-8");
      const lines = raw.trim().split("\n").filter(Boolean);
      return "[" + lines.join(",") + "]";
    } catch { return "[]"; }
  },

  clearLog() {
    try { if (existsSync(LOG_FILE)) unlinkSync(LOG_FILE); } catch {}
  },
};
