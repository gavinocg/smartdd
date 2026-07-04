import { Router } from "express";
import { z } from "zod";
import QRCode from "qrcode";
import { prisma } from "../services/prisma";
import { authMiddleware, AuthRequest } from "../middleware/auth";

export const qrRouter = Router();

const createQRSchema = z.object({
  lat: z.number().min(-90).max(90),
  lng: z.number().min(-180).max(180),
  radius: z.number().min(10).max(1000).optional(),
});

const validateQRSchema = z.object({
  lat: z.number().min(-90).max(90),
  lng: z.number().min(-180).max(180),
});

// POST /api/v1/qr — Crear QR (autenticado)
qrRouter.post("/", authMiddleware, async (req: AuthRequest, res) => {
  try {
    const data = createQRSchema.parse(req.body);
    const userId = req.userId!;

    // Verificar límite según plan
    const user = await prisma.user.findUnique({ where: { id: userId } });
    if (!user) {
      res.status(404).json({ error: "Usuario no encontrado" });
      return;
    }

    if (user.plan === "FREE") {
      const activeQRs = await prisma.qrCode.count({
        where: { userId, active: true },
      });
      if (activeQRs >= 1) {
        res.status(403).json({
          error: "Límite alcanzado. Los usuarios Free pueden tener 1 QR activo. Actualiza a Pro para QRs ilimitados.",
        });
        return;
      }
    }

    // Crear QR
    const qr = await prisma.qrCode.create({
      data: {
        uuid: crypto.randomUUID(),
        userId,
        lat: data.lat,
        lng: data.lng,
        radiusMeters: data.radius || 50,
      },
    });

    // Generar imagen QR
    const qrUrl = `${req.protocol}://192.168.100.101:8000/api/v1/qr/${qr.uuid}`;
    const qrImage = await QRCode.toDataURL(qrUrl, {
      width: 400,
      margin: 2,
      color: { dark: "#1a1a2e", light: "#ffffff" },
    });

    res.status(201).json({
      qr: {
        id: qr.id,
        uuid: qr.uuid,
        lat: qr.lat,
        lng: qr.lng,
        radiusMeters: qr.radiusMeters,
        active: qr.active,
        createdAt: qr.createdAt,
        imageUrl: qrImage,
      },
    });
  } catch (err) {
    if (err instanceof z.ZodError) {
      res.status(400).json({ error: "Datos inválidos", details: err.errors });
      return;
    }
    console.error("[QR] Error al crear:", err);
    res.status(500).json({ error: "Error al generar QR" });
  }
});

// GET /api/v1/qr — Listar QRs del usuario (autenticado)
qrRouter.get("/", authMiddleware, async (req: AuthRequest, res) => {
  try {
    const qrs = await prisma.qrCode.findMany({
      where: { userId: req.userId },
      orderBy: { createdAt: "desc" },
      include: { _count: { select: { sessions: true } } },
    });

    res.json({ qrs });
  } catch (err) {
    console.error("[QR] Error al listar:", err);
    res.status(500).json({ error: "Error al listar QRs" });
  }
});

// GET /api/v1/qr/:uuid — Obtener QR por UUID (público)
qrRouter.get("/:uuid", async (req, res) => {
  try {
    const qr = await prisma.qrCode.findUnique({
      where: { uuid: req.params.uuid as string },
      select: { uuid: true, lat: true, lng: true, radiusMeters: true, active: true },
    });

    if (!qr) {
      res.status(404).json({ error: "QR no encontrado" });
      return;
    }

    if (!qr.active) {
      res.status(410).json({ error: "QR desactivado" });
      return;
    }

    res.json({ qr });
  } catch (err) {
    console.error("[QR] Error al obtener:", err);
    res.status(500).json({ error: "Error al obtener QR" });
  }
});

// POST /api/v1/qr/:uuid/validate — Validar geolocalización (público)
qrRouter.post("/:uuid/validate", async (req, res) => {
  try {
    const { lat, lng } = validateQRSchema.parse(req.body);

    const qr = await prisma.qrCode.findUnique({
      where: { uuid: req.params.uuid as string },
    });

    if (!qr || !qr.active) {
      res.status(404).json({ error: "QR inválido o desactivado" });
      return;
    }

    // Calcular distancia usando fórmula de Haversine
    const distance = calculateDistance(lat, lng, qr.lat, qr.lng);
    const isValid = distance <= qr.radiusMeters;

    res.json({
      valid: isValid,
      distance: Math.round(distance * 100) / 100,
      radiusMeters: qr.radiusMeters,
      message: isValid
        ? "Ubicación válida"
        : `Debes estar a menos de ${qr.radiusMeters}m del QR. Distancia actual: ${Math.round(distance)}m`,
    });
  } catch (err) {
    if (err instanceof z.ZodError) {
      res.status(400).json({ error: "Coordenadas inválidas", details: err.errors });
      return;
    }
    console.error("[QR] Error al validar:", err);
    res.status(500).json({ error: "Error al validar ubicación" });
  }
});

// DELETE /api/v1/qr/:uuid — Desactivar QR (autenticado)
qrRouter.delete("/:uuid", authMiddleware, async (req: AuthRequest, res) => {
  try {
    const qr = await prisma.qrCode.findUnique({ where: { uuid: req.params.uuid as string } });

    if (!qr) {
      res.status(404).json({ error: "QR no encontrado" });
      return;
    }

    if (qr.userId !== req.userId) {
      res.status(403).json({ error: "No puedes eliminar un QR que no te pertenece" });
      return;
    }

    await prisma.qrCode.update({
      where: { uuid: req.params.uuid as string },
      data: { active: false },
    });

    res.json({ success: true, message: "QR desactivado" });
  } catch (err) {
    console.error("[QR] Error al eliminar:", err);
    res.status(500).json({ error: "Error al desactivar QR" });
  }
});

// ─── Fórmula de Haversine ──────────────────────────
function calculateDistance(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371000; // Radio de la Tierra en metros
  const dLat = toRad(lat2 - lat1);
  const dLng = toRad(lng2 - lng1);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

function toRad(deg: number): number {
  return (deg * Math.PI) / 180;
}
