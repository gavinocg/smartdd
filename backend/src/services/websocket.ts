import { Server as HttpServer } from "http";
import { WebSocketServer as WSServer, WebSocket } from "ws";
import { verify } from "jsonwebtoken";
import { prisma } from "./prisma";
import { redis } from "./redis";
import { notifyUser } from "./fcm";

interface AuthenticatedSocket extends WebSocket {
  userId?: string;
  isAlive?: boolean;
}

interface WSMessage {
  type: string;
  [key: string]: unknown;
}

export class WebSocketServer {
  private wss: WSServer | null = null;
  private httpServer: HttpServer;

  // Mapa de userId → WebSocket (conexión activa)
  private connections = new Map<string, Set<AuthenticatedSocket>>();

  constructor(httpServer: HttpServer) {
    this.httpServer = httpServer;
  }

  initialize() {
    this.wss = new WSServer({ server: this.httpServer, path: "/ws" });

    this.wss.on("connection", (socket: AuthenticatedSocket, req) => {
      console.log("[WS] Nueva conexión desde:", req.socket.remoteAddress);

      socket.isAlive = true;

      // Heartbeat
      socket.on("pong", () => {
        socket.isAlive = true;
      });

      // Autenticación
      socket.on("message", async (data) => {
        try {
          const message: WSMessage = JSON.parse(data.toString());

          if (message.type === "auth") {
            await this.handleAuth(socket, message);
          } else if (socket.userId) {
            await this.handleMessage(socket, message);
          } else {
            this.send(socket, { type: "error", message: "No autenticado. Envía { type: 'auth', token }" });
          }
        } catch (err) {
          console.error("[WS] Error al procesar mensaje:", err);
          this.send(socket, { type: "error", message: "Mensaje inválido" });
        }
      });

      socket.on("close", () => {
        if (socket.userId) {
          const userSockets = this.connections.get(socket.userId);
          if (userSockets) {
            userSockets.delete(socket);
            if (userSockets.size === 0) {
              this.connections.delete(socket.userId);
            }
          }
        }
      });

      socket.on("error", (err) => {
        console.error("[WS] Error de socket:", err.message);
      });
    });

    // Heartbeat interval
    const heartbeat = setInterval(() => {
      if (!this.wss) return;
      this.wss.clients.forEach((ws) => {
        const socket = ws as AuthenticatedSocket;
        if (socket.isAlive === false) {
          return socket.terminate();
        }
        socket.isAlive = false;
        socket.ping();
      });
    }, 30000);

    this.wss.on("close", () => {
      clearInterval(heartbeat);
    });

    console.log("[WS] WebSocket server inicializado en /ws");
  }

  private async handleAuth(socket: AuthenticatedSocket, message: WSMessage) {
    const token = message.token as string;
    if (!token) {
      this.send(socket, { type: "error", message: "Token requerido" });
      return;
    }

    try {
      const decoded = verify(token, process.env.JWT_SECRET || "secret") as { userId: string };
      socket.userId = decoded.userId;

      // Registrar conexión
      if (!this.connections.has(decoded.userId)) {
        this.connections.set(decoded.userId, new Set());
      }
      this.connections.get(decoded.userId)!.add(socket);

      this.send(socket, { type: "authenticated", userId: decoded.userId });
      console.log(`[WS] Usuario autenticado: ${decoded.userId}`);
    } catch {
      this.send(socket, { type: "error", message: "Token inválido o expirado" });
    }
  }

  private async handleMessage(socket: AuthenticatedSocket, message: WSMessage) {
    switch (message.type) {
      case "ring":
        await this.handleRing(socket, message);
        break;
      case "respond":
        await this.handleRespond(socket, message);
        break;
      case "cancel_ring":
        await this.handleCancelRing(socket, message);
        break;
      case "chat_message":
        await this.handleChatMessage(socket, message);
        break;
      default:
        this.send(socket, { type: "error", message: `Tipo de mensaje desconocido: ${message.type}` });
    }
  }

  private async handleRing(socket: AuthenticatedSocket, message: WSMessage) {
    const { qrId } = message as unknown as { qrId: string };

    const qr = await prisma.qrCode.findUnique({
      where: { uuid: qrId },
      include: { user: true },
    });

    if (!qr || !qr.active) {
      this.send(socket, { type: "error", message: "QR inválido o desactivado" });
      return;
    }

    // Crear sesión
    const session = await prisma.ringSession.create({
      data: {
        uuid: crypto.randomUUID(),
        qrId: qr.id,
        emisorId: socket.userId!,
        receptorId: qr.userId,
        status: "PENDING",
      },
    });

    // Notificar al Receptor
    this.sendToUser(qr.userId, {
      type: "incoming_ring",
      sessionId: session.id,
      roomId: session.uuid,
      emisorId: socket.userId,
      previewVideo: true,
    });

    // Confirmar al Emisor
    this.send(socket, {
      type: "ring_sent",
      sessionId: session.id,
      roomId: session.uuid,
    });

    // Timeout automático
    const config = await prisma.userConfig.findUnique({ where: { userId: qr.userId } });
    const timeoutMs = (config?.timeoutSeconds || 60) * 1000;

    setTimeout(async () => {
      const current = await prisma.ringSession.findUnique({ where: { id: session.id } });
      if (current && current.status === "PENDING") {
        await prisma.ringSession.update({
          where: { id: session.id },
          data: { status: "TIMEOUT" },
        });

        this.sendToUser(socket.userId!, { type: "ring_timeout", sessionId: session.id });
        this.sendToUser(qr.userId, { type: "ring_timeout", sessionId: session.id });
      }
    }, timeoutMs);
  }

  private async handleRespond(socket: AuthenticatedSocket, message: WSMessage) {
    const { sessionId, action, mode } = message as unknown as {
      sessionId: string;
      action: "accept" | "reject";
      mode?: "chat" | "audio" | "video";
    };

    const session = await prisma.ringSession.findUnique({
      where: { id: sessionId },
    });

    if (!session || session.status !== "PENDING") {
      this.send(socket, { type: "error", message: "Sesión inválida o expirada" });
      return;
    }

    if (action === "accept") {
      if (!mode) {
        this.send(socket, { type: "error", message: "Modo de respuesta requerido" });
        return;
      }

      // Validar que el modo esté habilitado en la config del Receptor
      const config = await prisma.userConfig.findUnique({ where: { userId: socket.userId! } });
      if (config) {
        const modeEnabled = {
          chat: config.chatEnabled,
          audio: config.audioEnabled,
          video: config.videoEnabled,
        };
        if (!modeEnabled[mode]) {
          this.send(socket, { type: "error", message: `Modo ${mode} no habilitado en tu configuración` });
          return;
        }
      }

      await prisma.ringSession.update({
        where: { id: sessionId },
        data: {
          status: "ACTIVE",
          responseMode: mode.toUpperCase() as any,
          respondedAt: new Date(),
        },
      });

      this.sendToUser(session.emisorId, {
        type: "ring_answered",
        sessionId,
        action: "accept",
        mode,
      });

      this.send(socket, { type: "response_sent", sessionId });
    } else {
      await prisma.ringSession.update({
        where: { id: sessionId },
        data: { status: "REJECTED", respondedAt: new Date() },
      });

      this.sendToUser(session.emisorId, {
        type: "ring_rejected",
        sessionId,
      });

      this.send(socket, { type: "response_sent", sessionId });
    }
  }

  private async handleCancelRing(socket: AuthenticatedSocket, message: WSMessage) {
    const { sessionId } = message as unknown as { sessionId: string };

    const session = await prisma.ringSession.findUnique({ where: { id: sessionId } });
    if (!session || session.emisorId !== socket.userId) {
      this.send(socket, { type: "error", message: "No puedes cancelar esta sesión" });
      return;
    }

    await prisma.ringSession.update({
      where: { id: sessionId },
      data: { status: "REJECTED" },
    });

    this.sendToUser(session.receptorId, {
      type: "ring_cancelled",
      sessionId,
    });

    this.send(socket, { type: "ring_cancelled", sessionId });
  }

  private async handleChatMessage(socket: AuthenticatedSocket, message: WSMessage) {
    const { sessionId, text } = message as unknown as { sessionId: string; text: string };

    const session = await prisma.ringSession.findUnique({ where: { id: sessionId } });
    if (!session || session.status !== "ACTIVE") {
      this.send(socket, { type: "error", message: "Sesión no activa" });
      return;
    }

    const isEmisor = session.emisorId === socket.userId;
    const targetId = isEmisor ? session.receptorId : session.emisorId;

    this.sendToUser(targetId, {
      type: "chat_message",
      sessionId,
      from: isEmisor ? "emisor" : "receptor",
      text,
      timestamp: new Date().toISOString(),
    });
  }

  // ─── Helpers ────────────────────────────────────

  private send(socket: WebSocket, message: object) {
    if (socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify(message));
    }
  }

  sendToUser(userId: string, message: object) {
    const sockets = this.connections.get(userId);
    if (sockets) {
      sockets.forEach((socket) => {
        this.send(socket, message);
      });
    }
  }

  isUserConnected(userId: string): boolean {
    const sockets = this.connections.get(userId);
    return sockets !== undefined && sockets.size > 0;
  }

  // Enviar notificación push si no está conectado
  async notifyUser(userId: string, message: object): Promise<number> {
    // Verificar si tiene WebSocket activo
    if (this.connections.has(userId)) {
      this.sendToUser(userId, message);
      return 0;
    }

    // Enviar FCM push notification
    const msg = message as Record<string, string>;
    if (msg.type === "incoming_ring") {
      return await notifyUser(
        userId,
        "🔔 Alguien toca el timbre",
        msg.emisorName || "Alguien está en la puerta",
        {
          type: "incoming_ring",
          sessionId: msg.sessionId || "",
          roomId: msg.roomId || "",
          emisorName: msg.emisorName || "",
        }
      );
    }
    return 0;
  }
}

// Singleton
let instance: WebSocketServer | null = null;

export function getWebSocketServer(httpServer?: HttpServer): WebSocketServer {
  if (!instance && httpServer) {
    instance = new WebSocketServer(httpServer);
  }
  if (!instance) {
    throw new Error("WebSocketServer no inicializado. Proporciona httpServer en la primera llamada.");
  }
  return instance;
}
