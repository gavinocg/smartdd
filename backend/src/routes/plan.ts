import { Router } from "express";
import { prisma } from "../services/prisma";
import { verifyPurchase } from "../services/play";
import { AuthRequest } from "../middleware/auth";

export const planRouter = Router();

// GET /api/v1/user/plan
planRouter.get("/", async (req: AuthRequest, res) => {
  try {
    const user = await prisma.user.findUnique({
      where: { id: req.userId },
      select: {
        plan: true,
        _count: {
          select: {
            qrCodes: { where: { active: true } },
            devices: { where: { active: true } },
          },
        },
      },
    });

    if (!user) {
      res.status(404).json({ error: "Usuario no encontrado" });
      return;
    }

    const limits = user.plan === "FREE"
      ? { qrLimit: 1, deviceLimit: 1 }
      : { qrLimit: -1, deviceLimit: -1 }; // -1 = ilimitado

    res.json({
      plan: user.plan,
      ...limits,
      currentQRs: user._count.qrCodes,
      currentDevices: user._count.devices,
    });
  } catch (err) {
    console.error("[Plan] Error:", err);
    res.status(500).json({ error: "Error al obtener plan" });
  }
});

// POST /api/v1/user/upgrade — Actualizar a Pro (integrado con Google Play Billing)
planRouter.post("/upgrade", async (req: AuthRequest, res) => {
  try {
    const { purchaseToken, productId } = req.body;

    if (!purchaseToken || !productId) {
      res.status(400).json({ error: "Datos de compra requeridos" });
      return;
    }

    const result = await verifyPurchase(purchaseToken, productId);
    if (!result.verified) {
      res.status(402).json({ error: result.message });
      return;
    }

    const user = await prisma.user.update({
      where: { id: req.userId },
      data: { plan: "PRO" },
      select: { id: true, name: true, email: true, plan: true },
    });

    res.json({ user, message: "Cuenta actualizada a Pro" });
  } catch (err) {
    console.error("[Plan] Error al actualizar:", err);
    res.status(500).json({ error: "Error al actualizar plan" });
  }
});

// POST /api/v1/user/cancel — Cancelar Pro (vuelve a Free)
planRouter.post("/cancel", async (req: AuthRequest, res) => {
  try {
    // Desactivar QRs excedentes (dejar solo 1)
    const qrs = await prisma.qrCode.findMany({
      where: { userId: req.userId, active: true },
      orderBy: { createdAt: "desc" },
      skip: 1, // Mantener el más reciente
    });

    if (qrs.length > 0) {
      await prisma.qrCode.updateMany({
        where: { id: { in: qrs.map((q) => q.id) } },
        data: { active: false },
      });
    }

    const user = await prisma.user.update({
      where: { id: req.userId },
      data: { plan: "FREE" },
      select: { id: true, name: true, email: true, plan: true },
    });

    res.json({
      user,
      message: "Plan cancelado. Los QRs adicionales han sido desactivados.",
    });
  } catch (err) {
    console.error("[Plan] Error al cancelar:", err);
    res.status(500).json({ error: "Error al cancelar plan" });
  }
});
