import { WebSocket } from "ws";

export interface MonitorEvent {
  id: string;
  ts: string;
  type: string;
  severity: "info" | "success" | "warning" | "error";
  source: string;
  message: string;
  details?: Record<string, unknown>;
}

class AdminMonitor {
  private clients = new Set<WebSocket>();
  private history: MonitorEvent[] = [];
  private maxHistory = 500;

  addClient(ws: WebSocket) {
    this.clients.add(ws);
    ws.on("close", () => this.clients.delete(ws));
    ws.on("error", () => this.clients.delete(ws));
    this.send(ws, { type: "init", events: this.history.slice(-50) });
  }

  private send(ws: WebSocket, msg: object) {
    if (ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(msg));
  }

  broadcast(event: Omit<MonitorEvent, "id" | "ts">) {
    const entry: MonitorEvent = {
      id: crypto.randomUUID(),
      ts: new Date().toISOString(),
      ...event,
    };
    this.history.push(entry);
    if (this.history.length > this.maxHistory) this.history.shift();
    const msg = JSON.stringify({ type: "event", event: entry });
    for (const ws of this.clients) {
      if (ws.readyState === WebSocket.OPEN) ws.send(msg);
    }
  }

  getRecent(limit = 200): MonitorEvent[] {
    return this.history.slice(-limit);
  }

  getClientsCount(): number {
    return this.clients.size;
  }
}

export const adminMonitor = new AdminMonitor();
