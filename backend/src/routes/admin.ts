import { Router } from "express";
import { z } from "zod";
import { prisma } from "../services/prisma";
import { redis } from "../services/redis";
import { AuthRequest, adminMiddleware } from "../middleware/auth";
import { adminMonitor } from "../services/adminMonitor";

export const adminRouter = Router();

// Todas las rutas de admin requieren rol admin
adminRouter.use(adminMiddleware);

// GET /api/v1/admin/users
adminRouter.get("/users", async (req: AuthRequest, res) => {
  try {
    const page = parseInt(req.query.page as string) || 1;
    const limit = parseInt(req.query.limit as string) || 20;
    const search = req.query.search as string;
    const plan = req.query.plan as string;

    const where: any = {};
    if (search) {
      where.OR = [
        { name: { contains: search, mode: "insensitive" } },
        { email: { contains: search, mode: "insensitive" } },
      ];
    }
    if (plan && ["FREE", "PRO", "B2P"].includes(plan.toUpperCase())) {
      where.plan = plan.toUpperCase();
    }

    const [users, total] = await Promise.all([
      prisma.user.findMany({
        where,
        skip: (page - 1) * limit,
        take: limit,
        orderBy: { createdAt: "desc" },
        select: {
          id: true,
          name: true,
          email: true,
          plan: true,
          role: true,
          active: true,
          createdAt: true,
          _count: {
            select: {
              qrCodes: { where: { active: true } },
              sessions: true,
            },
          },
        },
      }),
      prisma.user.count({ where }),
    ]);

    res.json({
      users,
      pagination: {
        page,
        limit,
        total,
        totalPages: Math.ceil(total / limit),
      },
    });
  } catch (err) {
    console.error("[Admin] Error al listar usuarios:", err);
    res.status(500).json({ error: "Error al listar usuarios" });
  }
});

// GET /api/v1/admin/users/:id
adminRouter.get("/users/:id", async (req: AuthRequest, res) => {
  try {
    const user = await prisma.user.findUnique({
      where: { id: req.params.id as string as string },
      select: {
        id: true,
        name: true,
        email: true,
        plan: true,
        role: true,
        active: true,
        createdAt: true,
        updatedAt: true,
        config: true,
        qrCodes: {
          orderBy: { createdAt: "desc" },
          take: 50,
        },
        sessions: {
          orderBy: { createdAt: "desc" },
          take: 50,
        },
      },
    });

    if (!user) {
      res.status(404).json({ error: "Usuario no encontrado" });
      return;
    }

    res.json({ user });
  } catch (err) {
    console.error("[Admin] Error al obtener usuario:", err);
    res.status(500).json({ error: "Error al obtener usuario" });
  }
});

// PUT /api/v1/admin/users/:id/plan
const changePlanSchema = z.object({
  plan: z.enum(["FREE", "PRO", "B2P"]),
});

adminRouter.put("/users/:id/plan", async (req: AuthRequest, res) => {
  try {
    const { plan } = changePlanSchema.parse(req.body);

    const user = await prisma.user.findUnique({ where: { id: req.params.id as string } });
    if (!user) {
      res.status(404).json({ error: "Usuario no encontrado" });
      return;
    }

    await prisma.user.update({
      where: { id: req.params.id as string },
      data: { plan },
    });

    // Registrar en log de auditoría
    await prisma.adminLog.create({
      data: {
        adminId: req.userId!,
        action: "change_plan",
        targetId: req.params.id as string,
        details: { from: user.plan, to: plan },
      },
    });

    res.json({ success: true, message: `Plan cambiado a ${plan}` });
  } catch (err) {
    if (err instanceof z.ZodError) {
      res.status(400).json({ error: "Plan inválido", details: err.errors });
      return;
    }
    console.error("[Admin] Error al cambiar plan:", err);
    res.status(500).json({ error: "Error al cambiar plan" });
  }
});

// PUT /api/v1/admin/users/:id/suspend
adminRouter.put("/users/:id/suspend", async (req: AuthRequest, res) => {
  try {
    const user = await prisma.user.findUnique({ where: { id: req.params.id as string } });
    if (!user) {
      res.status(404).json({ error: "Usuario no encontrado" });
      return;
    }

    const newStatus = !user.active;

    await prisma.user.update({
      where: { id: req.params.id as string },
      data: { active: newStatus },
    });

    await prisma.adminLog.create({
      data: {
        adminId: req.userId!,
        action: newStatus ? "unsuspend_user" : "suspend_user",
        targetId: req.params.id as string,
      },
    });

    res.json({
      success: true,
      message: newStatus ? "Usuario reactivado" : "Usuario suspendido",
    });
  } catch (err) {
    console.error("[Admin] Error al suspender:", err);
    res.status(500).json({ error: "Error al cambiar estado del usuario" });
  }
});

// GET /api/v1/admin/stats
adminRouter.get("/stats", async (_req: AuthRequest, res) => {
  try {
    const [totalUsers, freeUsers, proUsers, todayQRs, activeSessions] = await Promise.all([
      prisma.user.count(),
      prisma.user.count({ where: { plan: "FREE" } }),
      prisma.user.count({ where: { plan: "PRO" } }),
      prisma.qrCode.count({
        where: {
          createdAt: { gte: new Date(new Date().setHours(0, 0, 0, 0)) },
        },
      }),
      prisma.ringSession.count({
        where: { status: { in: ["PENDING", "PREVIEW", "ACTIVE"] } },
      }),
    ]);

    res.json({
      totalUsers,
      freeUsers,
      proUsers,
      b2pUsers: await prisma.user.count({ where: { plan: "B2P" } }),
      todayQRs,
      activeSessions,
      totalQRs: await prisma.qrCode.count(),
      totalSessions: await prisma.ringSession.count(),
    });
  } catch (err) {
    console.error("[Admin] Error al obtener stats:", err);
    res.status(500).json({ error: "Error al obtener estadísticas" });
  }
});

// GET /api/v1/admin/config
adminRouter.get("/config", async (_req: AuthRequest, res) => {
  // Configuración global desde variables de entorno
  res.json({
    defaultRadiusMeters: parseInt(process.env.DEFAULT_RADIUS_METERS || "50"),
    defaultTimeoutSeconds: parseInt(process.env.DEFAULT_TIMEOUT_SECONDS || "60"),
    jwtExpiresIn: process.env.JWT_EXPIRES_IN || "24h",
  });
});

// GET /api/v1/admin/logs
adminRouter.get("/logs", async (req: AuthRequest, res) => {
  try {
    const page = parseInt(req.query.page as string) || 1;
    const limit = parseInt(req.query.limit as string) || 50;

    const [logs, total] = await Promise.all([
      prisma.adminLog.findMany({
        skip: (page - 1) * limit,
        take: limit,
        orderBy: { createdAt: "desc" },
        include: {
          admin: { select: { name: true, email: true } },
        },
      }),
      prisma.adminLog.count(),
    ]);

    res.json({
      logs,
      pagination: { page, limit, total, totalPages: Math.ceil(total / limit) },
    });
  } catch (err) {
    console.error("[Admin] Error al obtener logs:", err);
    res.status(500).json({ error: "Error al obtener logs" });
  }
});

// GET /api/v1/admin/monitor/recent — Eventos recientes del monitor
adminRouter.get("/monitor/recent", (_req: AuthRequest, res) => {
  res.json({ events: adminMonitor.getRecent(200), clients: adminMonitor.getClientsCount() });
});

// GET /api/v1/admin/config/general — Config global del sistema
adminRouter.get("/config/general", async (_req: AuthRequest, res) => {
  try {
    const keys = ["defaultTimeoutSeconds", "defaultRadiusMeters", "maintenanceMode", "maxQrPerUser"];
    const values: Record<string, string | null> = {};
    for (const key of keys) {
      values[key] = await redis.get(`admin:config:${key}`);
    }
    res.json({
      defaultTimeoutSeconds: parseInt(values.defaultTimeoutSeconds || "60"),
      defaultRadiusMeters: parseInt(values.defaultRadiusMeters || "50"),
      maintenanceMode: values.maintenanceMode === "true",
      maxQrPerUser: parseInt(values.maxQrPerUser || "10"),
    });
  } catch {
    // Fallback si Redis no está disponible
    res.json({
      defaultTimeoutSeconds: 60,
      defaultRadiusMeters: 50,
      maintenanceMode: false,
      maxQrPerUser: 10,
    });
  }
});

// PUT /api/v1/admin/config/general — Guardar config global
const configSchema = z.object({
  defaultTimeoutSeconds: z.number().min(10).max(300).optional(),
  defaultRadiusMeters: z.number().min(10).max(1000).optional(),
  maintenanceMode: z.boolean().optional(),
  maxQrPerUser: z.number().min(1).max(100).optional(),
});

adminRouter.put("/config/general", async (req: AuthRequest, res) => {
  try {
    const data = configSchema.parse(req.body);
    const pipeline = redis.multi();
    for (const [key, value] of Object.entries(data)) {
      pipeline.set(`admin:config:${key}`, String(value));
    }
    await pipeline.exec();

    adminMonitor.broadcast({
      type: "config_updated", severity: "info", source: "admin",
      message: `Configuración actualizada por admin`,
      details: { changes: data, adminId: req.userId! },
    });

    res.json({ success: true, message: "Configuración guardada" });
  } catch (err) {
    if (err instanceof z.ZodError) {
      res.status(400).json({ error: "Datos inválidos", details: err.errors });
      return;
    }
    console.error("[Admin] Error al guardar config:", err);
    res.status(500).json({ error: "Error al guardar configuración" });
  }
});

// POST /api/v1/admin/monitor/test — Enviar evento de prueba (para debug)
adminRouter.post("/monitor/test", (req: AuthRequest, res) => {
  const { severity, message } = req.body as { severity?: string; message?: string };
  adminMonitor.broadcast({
    type: "test_event",
    severity: (severity as any) || "info",
    source: "admin",
    message: message || "Evento de prueba desde el panel",
    details: { adminId: req.userId! },
  });
  res.json({ success: true });
});
