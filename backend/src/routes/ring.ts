import { Router } from "express";
import { z } from "zod";
import { prisma } from "../services/prisma";
import { getWebSocketServer } from "../services/websocket";
import { AuthRequest } from "../middleware/auth";
import { RingLogger } from "../services/ringLogger";

export const ringRouter = Router();

const ringSchema = z.object({
  qrId: z.string().uuid(),
});

const respondSchema = z.object({
  sessionId: z.string().uuid(),
  action: z.enum(["accept", "reject"]),
  mode: z.enum(["chat", "audio", "video"]).optional(),
});

// POST /api/v1/ring — Emisor toca timbre
ringRouter.post("/", async (req: AuthRequest, res) => {
  try {
    const data = ringSchema.parse(req.body);

    const qr = await prisma.qrCode.findUnique({
      where: { uuid: data.qrId },
      include: { user: { include: { config: true } } },
    });

    if (!qr || !qr.active) {
      RingLogger.error("qr_lookup", null, `QR ${data.qrId} inválido o desactivado`);
      res.status(404).json({ error: "QR inválido o desactivado" });
      return;
    }

    // Verificar sesión activa existente
    const existingSession = await prisma.ringSession.findFirst({
      where: {
        qrId: qr.id,
        status: { in: ["PENDING", "PREVIEW", "ACTIVE"] },
      },
    });

    if (existingSession) {
      RingLogger.existingSession(existingSession.id, existingSession.status, req.userId!);
      res.status(409).json({
        error: "Ya hay una sesión activa para este QR",
        session: { id: existingSession.id, roomId: existingSession.uuid, status: existingSession.status },
      });
      return;
    }

    RingLogger.ringInitiated(data.qrId, req.userId!, qr.user.id);

    // Crear sesión
    const session = await prisma.ringSession.create({
      data: {
        uuid: crypto.randomUUID(),
        qrId: qr.id,
        emisorId: req.userId!,
        emisorName: req.body.emisorName || "Visitante",
        receptorId: qr.user.id,
        status: "PENDING",
      },
    });

    RingLogger.sessionCreated(session.id, session.uuid, session.emisorName);

    // Notificar al Receptor vía WebSocket
    const wss = getWebSocketServer();
    const receptorWsConnected = wss.isUserConnected(qr.user.id);
    RingLogger.wsSentToReceptor(session.id, qr.user.id, receptorWsConnected);
    wss.sendToUser(qr.user.id, {
      type: "incoming_ring",
      sessionId: session.id,
      roomId: session.uuid,
      emisorName: session.emisorName,
      previewVideo: true,
    });

    // También intentar notificación push si no está conectado
    const devicesSent = await wss.notifyUser(qr.user.id, {
      type: "incoming_ring",
      sessionId: session.id,
      roomId: session.uuid,
    });
    RingLogger.pushSent(session.id, qr.user.id, devicesSent);

    // Timeout automático
    const timeoutMs = (qr.user.config?.timeoutSeconds || 60) * 1000;
    setTimeout(async () => {
      const current = await prisma.ringSession.findUnique({ where: { id: session.id } });
      if (current && ["PENDING", "PREVIEW"].includes(current.status)) {
        await prisma.ringSession.update({
          where: { id: session.id },
          data: { status: "TIMEOUT" },
        });
        RingLogger.sessionTimedOut(session.id);
        wss.sendToUser(session.emisorId, { type: "ring_timeout", sessionId: session.id });
        wss.sendToUser(session.receptorId, { type: "ring_timeout", sessionId: session.id });
      }
    }, timeoutMs);

    res.status(201).json({
      session: {
        id: session.id,
        roomId: session.uuid,
        status: session.status,
      },
    });
  } catch (err) {
    if (err instanceof z.ZodError) {
      RingLogger.error("zod_validation", null, err.errors.map(e => e.message).join(", "));
      res.status(400).json({ error: "Datos inválidos", details: err.errors });
      return;
    }
    RingLogger.error("ring_create", null, (err as Error).message);
    console.error("[Ring] Error:", err);
    res.status(500).json({ error: "Error al procesar el timbre" });
  }
});

// POST /api/v1/ring/respond — Receptor responde
ringRouter.post("/respond", async (req: AuthRequest, res) => {
  try {
    const data = respondSchema.parse(req.body);

    const session = await prisma.ringSession.findUnique({ where: { id: data.sessionId } });

    if (!session) {
      RingLogger.error("respond_lookup", data.sessionId, "Sesión no encontrada");
      res.status(404).json({ error: "Sesión no encontrada" });
      return;
    }

    if (session.receptorId !== req.userId) {
      RingLogger.error("respond_forbidden", data.sessionId, "No eres el receptor");
      res.status(403).json({ error: "No eres el receptor de esta solicitud" });
      return;
    }

    if (session.status !== "PENDING" && session.status !== "PREVIEW") {
      RingLogger.error("respond_wrong_status", data.sessionId, `Estado actual: ${session.status}`);
      res.status(409).json({ error: `La sesión ya fue ${session.status.toLowerCase()}` });
      return;
    }

    const wss = getWebSocketServer();

    if (data.action === "accept") {
      if (!data.mode) {
        RingLogger.error("respond_no_mode", data.sessionId, "Modo no especificado");
        res.status(400).json({ error: "Modo de respuesta requerido (chat, audio o video)" });
        return;
      }

      // Validar opciones habilitadas
      const config = await prisma.userConfig.findUnique({ where: { userId: req.userId } });
      if (config) {
        const modeEnabled = {
          chat: config.chatEnabled,
          audio: config.audioEnabled,
          video: config.videoEnabled,
        };
        if (!modeEnabled[data.mode]) {
          res.status(403).json({ error: `Modo ${data.mode} deshabilitado en tu configuración` });
          return;
        }
      }

      await prisma.ringSession.update({
        where: { id: data.sessionId },
        data: {
          status: "ACTIVE",
          responseMode: data.mode.toUpperCase() as any,
          respondedAt: new Date(),
        },
      });

      RingLogger.receptorResponded(session.id, "accept", data.mode);
      RingLogger.emisorNotified(session.id, session.emisorId, "accept");

      wss.sendToUser(session.emisorId, {
        type: "ring_answered",
        sessionId: session.id,
        action: "accept",
        mode: data.mode,
      });

      res.json({
        success: true,
        session: {
          id: session.id,
          status: "ACTIVE",
          mode: data.mode,
        },
      });
    } else {
      await prisma.ringSession.update({
        where: { id: data.sessionId },
        data: { status: "REJECTED", respondedAt: new Date() },
      });

      RingLogger.receptorResponded(session.id, "reject", null);
      RingLogger.emisorNotified(session.id, session.emisorId, "reject");

      wss.sendToUser(session.emisorId, {
        type: "ring_rejected",
        sessionId: session.id,
      });

      res.json({ success: true, status: "REJECTED" });
    }
  } catch (err) {
    if (err instanceof z.ZodError) {
      RingLogger.error("respond_zod", null, err.errors.map(e => e.message).join(", "));
      res.status(400).json({ error: "Datos inválidos", details: err.errors });
      return;
    }
    RingLogger.error("respond", null, (err as Error).message);
    console.error("[Ring] Error en respond:", err);
    res.status(500).json({ error: "Error al responder" });
  }
});

// GET /api/v1/ring/session/:id — Obtener estado de sesión
ringRouter.get("/session/:id", async (req: AuthRequest, res) => {
  try {
    const session = await prisma.ringSession.findUnique({
      where: { id: req.params.id as string },
      include: {
        qr: { select: { uuid: true } },
      },
    });

    if (!session) {
      res.status(404).json({ error: "Sesión no encontrada" });
      return;
    }

    if (session.emisorId !== req.userId && session.receptorId !== req.userId) {
      res.status(403).json({ error: "No tienes acceso a esta sesión" });
      return;
    }

    res.json({ session });
  } catch (err) {
    console.error("[Ring] Error al obtener sesión:", err);
    res.status(500).json({ error: "Error al obtener sesión" });
  }
});
