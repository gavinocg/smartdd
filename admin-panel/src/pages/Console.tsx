import { useState, useEffect, useRef } from "react";
import { get } from "../api/client";

interface MonitorEvent {
  id: string;
  ts: string;
  type: string;
  severity: "info" | "success" | "warning" | "error";
  source: string;
  message: string;
  details?: Record<string, unknown>;
}

const severityColor: Record<string, string> = {
  success: "#48bb78",
  info: "#4299e1",
  warning: "#ecc94b",
  error: "#fc8181",
};

const severityBg: Record<string, string> = {
  success: "#f0fff4",
  info: "#ebf8ff",
  warning: "#fffff0",
  error: "#fff5f5",
};

export default function Console() {
  const [events, setEvents] = useState<MonitorEvent[]>([]);
  const [filter, setFilter] = useState<string>("all");
  const [search, setSearch] = useState("");
  const [connected, setConnected] = useState(false);
  const [clients, setClients] = useState(0);
  const [testMsg, setTestMsg] = useState("");
  const bottomRef = useRef<HTMLDivElement>(null);
  const [autoScroll, setAutoScroll] = useState(true);

  useEffect(() => {
    get<{ events: MonitorEvent[]; clients: number }>("/admin/monitor/recent")
      .then((data) => { setEvents(data.events); setClients(data.clients); })
      .catch(() => {});
  }, []);

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) return;
    const ws = new WebSocket("ws://192.168.100.101:8000/ws/admin/monitor");
    ws.onopen = () => {
      ws.send(JSON.stringify({ type: "auth", token }));
    };
    ws.onmessage = (msg) => {
      try {
        const data = JSON.parse(msg.data);
        if (data.type === "authenticated") { setConnected(true); return; }
        if (data.type === "init") {
          setEvents((prev) => [...data.events, ...prev].slice(-500));
          return;
        }
        if (data.type === "event") {
          setEvents((prev) => [...prev, data.event].slice(-500));
        }
      } catch {}
    };
    ws.onclose = () => setConnected(false);
    ws.onerror = () => setConnected(false);
    return () => ws.close();
  }, []);

  useEffect(() => {
    if (autoScroll) bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [events, autoScroll]);

  const filtered = events.filter((e) => {
    if (filter !== "all" && e.severity !== filter) return false;
    if (search && !e.message.toLowerCase().includes(search.toLowerCase()) && !e.type.includes(search)) return false;
    return true;
  });

  function sendTest() {
    fetch("http://192.168.100.101:8000/api/v1/admin/monitor/test", {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${localStorage.getItem("token")}` },
      body: JSON.stringify({ severity: "info", message: testMsg || "Prueba desde consola" }),
    }).catch(() => {});
  }

  return (
    <div style={{ height: "calc(100vh - 4rem)", display: "flex", flexDirection: "column" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" }}>
        <h2 style={{ margin: 0 }}>Consola en vivo</h2>
        <div style={{ display: "flex", gap: "0.75rem", alignItems: "center", fontSize: "0.85rem" }}>
          <span style={{ color: connected ? "#48bb78" : "#fc8181", fontWeight: 600 }}>
            {connected ? "● Conectado" : "○ Desconectado"}
          </span>
          <span style={{ color: "#718096" }}>Monitores: {clients}</span>
          <span style={{ color: "#718096" }}>Eventos: {events.length}</span>
        </div>
      </div>

      <div style={{ display: "flex", gap: "0.5rem", marginBottom: "0.75rem", flexWrap: "wrap", alignItems: "center" }}>
        <div style={{ display: "flex", gap: "0.25rem" }}>
          {["all", "info", "success", "warning", "error"].map((s) => (
            <button key={s} onClick={() => setFilter(s)}
              style={{ padding: "4px 12px", borderRadius: "14px", border: "1px solid #e2e8f0", cursor: "pointer", fontSize: "0.8rem", fontWeight: filter === s ? 600 : 400, background: filter === s ? severityColor[s] || "#48bb78" : "white", color: filter === s ? "white" : "#4a5568" }}>
              {s === "all" ? "Todos" : s.charAt(0).toUpperCase() + s.slice(1)}
            </button>
          ))}
        </div>
        <input type="text" placeholder="Buscar..." value={search} onChange={(e) => setSearch(e.target.value)}
          style={{ flex: 1, minWidth: "150px", padding: "6px 12px", borderRadius: "6px", border: "1px solid #e2e8f0", fontSize: "0.85rem" }} />
        <label style={{ fontSize: "0.85rem", color: "#718096", display: "flex", alignItems: "center", gap: "4px" }}>
          <input type="checkbox" checked={autoScroll} onChange={(e) => setAutoScroll(e.target.checked)} /> Auto-scroll
        </label>
      </div>

      <div style={{ flex: 1, overflow: "auto", background: "#1a202c", borderRadius: "8px", padding: "0.5rem", fontFamily: "'JetBrains Mono', 'Fira Code', monospace", fontSize: "0.8rem" }}>
        {filtered.length === 0 && (
          <div style={{ color: "#4a5568", padding: "1rem", textAlign: "center" }}>Sin eventos. Presiona "Tocar timbre" en la app o usa el botón de prueba.</div>
        )}
        {filtered.map((e) => (
          <div key={e.id} style={{ display: "flex", gap: "0.5rem", padding: "3px 6px", borderRadius: "4px", background: severityBg[e.severity] + "11", borderLeft: `3px solid ${severityColor[e.severity]}`, marginBottom: "2px" }}>
            <span style={{ color: "#718096", minWidth: "80px", fontSize: "0.75rem" }}>
              {new Date(e.ts).toLocaleTimeString()}
            </span>
            <span style={{ color: severityColor[e.severity], minWidth: "60px", fontWeight: 600, fontSize: "0.75rem", textTransform: "uppercase" }}>
              {e.severity}
            </span>
            <span style={{ color: "#a0aec0", minWidth: "50px", fontSize: "0.75rem" }}>{e.source}</span>
            <span style={{ color: "#e2e8f0", flex: 1 }}>{e.message}</span>
          </div>
        ))}
        <div ref={bottomRef} />
      </div>

      <div style={{ display: "flex", gap: "0.5rem", marginTop: "0.5rem" }}>
        <input type="text" placeholder="Mensaje de prueba..." value={testMsg} onChange={(e) => setTestMsg(e.target.value)}
          style={{ flex: 1, padding: "6px 12px", borderRadius: "6px", border: "1px solid #e2e8f0", fontSize: "0.85rem" }} />
        <button onClick={sendTest} style={{ padding: "6px 16px", borderRadius: "6px", border: "none", background: "#4299e1", color: "white", cursor: "pointer", fontSize: "0.85rem", fontWeight: 600 }}>Enviar prueba</button>
        <button onClick={() => setEvents([])} style={{ padding: "6px 16px", borderRadius: "6px", border: "1px solid #e2e8f0", background: "white", cursor: "pointer", fontSize: "0.85rem", color: "#718096" }}>Limpiar</button>
      </div>
    </div>
  );
}
