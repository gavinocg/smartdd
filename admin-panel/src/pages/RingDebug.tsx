import { useState, useEffect, useCallback } from "react";

interface RingLogEntry {
  ts: string;
  event: string;
  sessionId?: string;
  qrUuid?: string;
  emisorId?: string;
  receptorId?: string;
  wsConnected?: boolean;
  deviceCount?: number;
  action?: string;
  mode?: string;
  message?: string;
  stage?: string;
}

export default function RingDebug() {
  const [logs, setLogs] = useState<RingLogEntry[]>([]);
  const [error, setError] = useState("");

  const fetchLogs = useCallback(() => {
    fetch("http://192.168.100.101:8000/api/v1/debug/ring")
      .then((r) => r.json())
      .then((data) => setLogs(Array.isArray(data) ? data.reverse() : []))
      .catch((e) => setError(e.message));
  }, []);

  useEffect(() => {
    fetchLogs();
    const interval = setInterval(fetchLogs, 5000);
    return () => clearInterval(interval);
  }, [fetchLogs]);

  const eventColor: Record<string, string> = {
    ring_initiated: "#4299e1",
    session_created: "#48bb78",
    ws_sent_to_receptor: "#667eea",
    push_sent: "#9f7aea",
    existing_session: "#ecc94b",
    receptor_notified: "#38b2ac",
    receptor_responded: "#48bb78",
    emisor_notified: "#4299e1",
    session_timeout: "#fc8181",
    error: "#fc8181",
  };

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" }}>
        <h2 style={{ margin: 0 }}>Ring Debug Log</h2>
        <button onClick={fetchLogs} style={{ padding: "6px 16px", borderRadius: "6px", border: "1px solid #e2e8f0", background: "white", cursor: "pointer", fontSize: "0.85rem" }}>Recargar</button>
      </div>

      {error && <div style={{ padding: "0.75rem", background: "#fff5f5", borderRadius: "6px", color: "#fc8181", marginBottom: "1rem" }}>{error}</div>}

      {logs.length === 0 && (
        <div style={{ color: "#718096", textAlign: "center", padding: "2rem" }}>
          No hay registros de handshake. Presiona "Tocar timbre" en la app para generar eventos.
        </div>
      )}

      <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
        {logs.map((entry, i) => (
          <div key={i} style={{ display: "flex", gap: "0.75rem", padding: "8px 12px", background: "white", borderRadius: "6px", alignItems: "center", borderLeft: `4px solid ${eventColor[entry.event] || "#a0aec0"}` }}>
            <span style={{ color: "#718096", fontSize: "0.8rem", minWidth: "75px" }}>
              {new Date(entry.ts).toLocaleTimeString()}
            </span>
            <span style={{ fontWeight: 600, fontSize: "0.85rem", color: eventColor[entry.event] || "#4a5568", minWidth: "140px" }}>
              {entry.event.replace(/_/g, " ")}
            </span>
            <span style={{ flex: 1, fontSize: "0.85rem", color: "#4a5568" }}>
              {entry.event === "ring_initiated" && `QR ${entry.qrUuid?.slice(0, 8)}... → emisor ${entry.emisorId?.slice(0, 8)}...`}
              {entry.event === "session_created" && `Sesión ${entry.sessionId?.slice(0, 8)}... creada`}
              {entry.event === "ws_sent_to_receptor" && `WS ${entry.wsConnected ? "conectado ✓" : "desconectado ✗"} → receptor ${entry.receptorId?.slice(0, 8)}...`}
              {entry.event === "push_sent" && `Push enviado a ${entry.deviceCount} dispositivo(s)`}
              {entry.event === "existing_session" && `Sesión ${entry.sessionId?.slice(0, 8)}... ya existe`}
              {entry.event === "receptor_responded" && `${entry.action === "accept" ? "ACEPTÓ" : "RECHAZÓ"} ${entry.mode ? `(${entry.mode})` : ""}`}
              {entry.event === "session_timeout" && `Timeout — sesión ${entry.sessionId?.slice(0, 8)}...`}
              {entry.event === "error" && `${entry.stage}: ${entry.message}`}
              {!["ring_initiated", "session_created", "ws_sent_to_receptor", "push_sent", "existing_session", "receptor_responded", "session_timeout", "error"].includes(entry.event) && `${entry.message || ""}`}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
