import { Router } from "express";
import fs from "fs";

export const debugRouter = Router();

const LOG_FILE = "/tmp/smartdd_debug.log";

debugRouter.post("/log", (req, res) => {
  try {
    const content = req.body?.content || JSON.stringify(req.body);
    const ts = new Date().toISOString();
    const line = `[${ts}] ${content}\n---\n`;
    fs.appendFileSync(LOG_FILE, line);
    res.json({ ok: true });
  } catch (err) {
    res.status(500).json({ error: "Failed to write log" });
  }
});

debugRouter.get("/log", (_req, res) => {
  try {
    const data = fs.readFileSync(LOG_FILE, "utf-8");
    res.type("text/plain").send(data);
  } catch {
    res.type("text/plain").send("No logs available");
  }
});

debugRouter.delete("/log", (_req, res) => {
  try { fs.unlinkSync(LOG_FILE); } catch {}
  res.json({ ok: true });
});
