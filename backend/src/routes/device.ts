import { Router } from "express";
import { z } from "zod";
import { prisma } from "../services/prisma";
import { AuthRequest } from "../middleware/auth";

export const deviceRouter = Router();

const registerDeviceSchema = z.object({
  token: z.string().min(1),
  platform: z.enum(["android"]).optional().default("android"),
});

// POST /api/v1/devices — Registrar token FCM
deviceRouter.post("/", async (req: AuthRequest, res) => {
  try {
    const data = registerDeviceSchema.parse(req.body);

    // Verificar límite según plan
    const user = await prisma.user.findUnique({ where: { id: req.userId } });
    if (!user) {
      res.status(404).json({ error: "Usuario no encontrado" });
      return;
    }

    if (user.plan === "FREE") {
      const activeDevices = await prisma.device.count({
        where: { userId: req.userId, active: true },
      });
      if (activeDevices >= 1) {
        res.status(403).json({
          error: "Límite de dispositivos alcanzado. Los usuarios Free pueden tener 1 dispositivo.",
        });
        return;
      }
    }

    // Buscar si el token ya existe para este usuario (actualizar en lugar de duplicar)
    const existing = await prisma.device.findFirst({
      where: { userId: req.userId, token: data.token },
    });

    if (existing) {
      await prisma.device.update({
        where: { id: existing.id },
        data: { active: true, platform: data.platform },
      });
    } else {
      await prisma.device.create({
        data: {
          userId: req.userId!,
          token: data.token,
          platform: data.platform,
        },
      });
    }

    res.status(201).json({ success: true });
  } catch (err) {
    if (err instanceof z.ZodError) {
      res.status(400).json({ error: "Datos inválidos", details: err.errors });
      return;
    }
    console.error("[Device] Error:", err);
    res.status(500).json({ error: "Error al registrar dispositivo" });
  }
});

// DELETE /api/v1/devices/:id — Desvincular dispositivo
deviceRouter.delete("/:id", async (req: AuthRequest, res) => {
  try {
    const device = await prisma.device.findUnique({
      where: { id: req.params.id as string },
    });

    if (!device || device.userId !== req.userId) {
      res.status(404).json({ error: "Dispositivo no encontrado" });
      return;
    }

    await prisma.device.update({
      where: { id: device.id },
      data: { active: false },
    });

    res.json({ success: true });
  } catch (err) {
    console.error("[Device] Error al eliminar:", err);
    res.status(500).json({ error: "Error al eliminar dispositivo" });
  }
});

// GET /api/v1/devices — Listar dispositivos del usuario
deviceRouter.get("/", async (req: AuthRequest, res) => {
  try {
    const devices = await prisma.device.findMany({
      where: { userId: req.userId, active: true },
      select: { id: true, platform: true, createdAt: true },
    });

    res.json({ devices });
  } catch (err) {
    console.error("[Device] Error al listar:", err);
    res.status(500).json({ error: "Error al listar dispositivos" });
  }
});
