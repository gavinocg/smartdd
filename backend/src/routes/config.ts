import { Router } from "express";
import { z } from "zod";
import { prisma } from "../services/prisma";
import { AuthRequest } from "../middleware/auth";

export const configRouter = Router();

const updateConfigSchema = z.object({
  defaultMode: z.enum(["chat", "audio", "video"]).optional(),
  chatEnabled: z.boolean().optional(),
  audioEnabled: z.boolean().optional(),
  videoEnabled: z.boolean().optional(),
  timeoutSeconds: z.number().min(10).max(300).optional(),
});

// GET /api/v1/user/config
configRouter.get("/", async (req: AuthRequest, res) => {
  try {
    let config = await prisma.userConfig.findUnique({
      where: { userId: req.userId },
    });

    // Crear configuración por defecto si no existe
    if (!config) {
      config = await prisma.userConfig.create({
        data: { userId: req.userId! },
      });
    }

    res.json({ config });
  } catch (err) {
    console.error("[Config] Error al obtener:", err);
    res.status(500).json({ error: "Error al obtener configuración" });
  }
});

// PUT /api/v1/user/config
configRouter.put("/", async (req: AuthRequest, res) => {
  try {
    const data = updateConfigSchema.parse(req.body);

    // Validar que al menos una opción esté habilitada
    const currentConfig = await prisma.userConfig.findUnique({
      where: { userId: req.userId },
    });

    const chatEnabled = data.chatEnabled ?? currentConfig?.chatEnabled ?? true;
    const audioEnabled = data.audioEnabled ?? currentConfig?.audioEnabled ?? true;
    const videoEnabled = data.videoEnabled ?? currentConfig?.videoEnabled ?? true;

    if (!chatEnabled && !audioEnabled && !videoEnabled) {
      res.status(400).json({ error: "Debes habilitar al menos una opción de respuesta" });
      return;
    }

    // Si se deshabilita el modo default actual, cambiarlo al primer disponible
    let defaultMode = data.defaultMode
      ? (data.defaultMode.toUpperCase() as "CHAT" | "AUDIO" | "VIDEO")
      : undefined;

    if (defaultMode) {
      const modeEnabled = {
        CHAT: chatEnabled,
        AUDIO: audioEnabled,
        VIDEO: videoEnabled,
      };
      if (!modeEnabled[defaultMode]) {
        defaultMode = (chatEnabled ? "CHAT" : audioEnabled ? "AUDIO" : "VIDEO") as "CHAT" | "AUDIO" | "VIDEO";
      }
    }

    const config = await prisma.userConfig.upsert({
      where: { userId: req.userId! },
      create: {
        userId: req.userId!,
        defaultMode: defaultMode || "CHAT",
        chatEnabled,
        audioEnabled,
        videoEnabled,
      },
      update: {
        ...(defaultMode && { defaultMode }),
        chatEnabled,
        audioEnabled,
        videoEnabled,
        ...(data.timeoutSeconds && { timeoutSeconds: data.timeoutSeconds }),
      },
    });

    res.json({ config });
  } catch (err) {
    if (err instanceof z.ZodError) {
      res.status(400).json({ error: "Datos inválidos", details: err.errors });
      return;
    }
    console.error("[Config] Error al actualizar:", err);
    res.status(500).json({ error: "Error al actualizar configuración" });
  }
});
