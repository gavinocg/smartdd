import "dotenv/config";
import express from "express";
import cors from "cors";
import helmet from "helmet";
import compression from "compression";
import { createServer } from "http";
import { WebSocketServer } from "./services/websocket";
import { prisma } from "./services/prisma";
import { redis } from "./services/redis";
import { authRouter } from "./routes/auth";
import { qrRouter } from "./routes/qr";
import { ringRouter } from "./routes/ring";
import { configRouter } from "./routes/config";
import { planRouter } from "./routes/plan";
import { adminRouter } from "./routes/admin";
import { deviceRouter } from "./routes/device";
import { authMiddleware } from "./middleware/auth";

const app = express();
const httpServer = createServer(app);
const PORT = parseInt(process.env.PORT || "3000", 10);

// ─── Middleware global ──────────────────────────────
app.use(helmet({ contentSecurityPolicy: false }));
app.use(cors({ origin: "*" }));
app.use(compression());
app.use(express.json({ limit: "1mb" }));

// ─── Health check ───────────────────────────────────
app.get("/api/v1/health", (_req, res) => {
  res.json({ status: "ok", timestamp: new Date().toISOString() });
});

// ─── Rutas públicas ────────────────────────────────
app.use("/api/v1/auth", authRouter);
app.use("/api/v1/qr", qrRouter);

// ─── Rutas protegidas ──────────────────────────────
app.use("/api/v1/ring", authMiddleware, ringRouter);
app.use("/api/v1/user/config", authMiddleware, configRouter);
app.use("/api/v1/user/plan", authMiddleware, planRouter);
app.use("/api/v1/devices", authMiddleware, deviceRouter);
app.use("/api/v1/admin", authMiddleware, adminRouter);

// ─── Manejo de errores ─────────────────────────────
app.use((err: any, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
  console.error("[ERROR]", err);
  const status = err.status || 500;
  res.status(status).json({
    error: err.message || "Error interno del servidor",
    ...(process.env.NODE_ENV === "development" && { stack: err.stack }),
  });
});

// ─── Inicializar servicios ─────────────────────────
async function bootstrap() {
  try {
    // Verificar conexiones
    await prisma.$connect();
    console.log("[DB] PostgreSQL conectado");

    await redis.ping();
    console.log("[Redis] Conexión establecida");

    // Inicializar WebSocket
    const wss = new WebSocketServer(httpServer);
    wss.initialize();
    console.log("[WS] WebSocket server inicializado");

    // Iniciar servidor HTTP
    httpServer.listen(PORT, "0.0.0.0", () => {
      console.log(`[Server] SMARTDD Signaling corriendo en puerto ${PORT}`);
    });
  } catch (error) {
    console.error("[FATAL] Error al iniciar el servidor:", error);
    process.exit(1);
  }
}

// ─── Graceful shutdown ─────────────────────────────
process.on("SIGTERM", async () => {
  console.log("[Shutdown] Recibida señal SIGTERM");
  await prisma.$disconnect();
  redis.disconnect();
  httpServer.close();
  process.exit(0);
});

process.on("SIGINT", async () => {
  console.log("[Shutdown] Recibida señal SIGINT");
  await prisma.$disconnect();
  redis.disconnect();
  httpServer.close();
  process.exit(0);
});

bootstrap();
