import { useState, useEffect, useCallback } from "react";
import { get, post } from "../api/client";

interface Session {
  id: string;
  uuid: string;
  status: string;
  emisorName: string | null;
  emisorId: string;
  receptorId: string;
  createdAt: string;
  qr: { uuid: string };
}

export default function ReceptorSimulator() {
  const [sessions, setSessions] = useState<Session[]>([]);
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState<string | null>(null);

  const fetchSessions = useCallback(async () => {
    try {
      const data = await get<{ sessions: Session[] }>("/admin/test/pending-sessions");
      setSessions(data.sessions);
    } catch (e: any) {
      setError(e.message);
    }
  }, []);

  useEffect(() => {
    fetchSessions();
    const interval = setInterval(fetchSessions, 3000);
    return () => clearInterval(interval);
  }, [fetchSessions]);

  async function accept(sessionId: string, mode: string) {
    setBusyId(sessionId);
    setError("");
    try {
      await post("/admin/test/accept-ring", { sessionId, mode });
      fetchSessions();
    } catch (e: any) {
      setError(e.message);
    } finally {
      setBusyId(null);
    }
  }

  async function reject(sessionId: string) {
    setBusyId(sessionId);
    setError("");
    try {
      await post("/admin/test/reject-ring", { sessionId });
      fetchSessions();
    } catch (e: any) {
      setError(e.message);
    } finally {
      setBusyId(null);
    }
  }

  return (
    <div>
      <h2 style={{ marginTop: 0 }}>Simulador de Receptor</h2>
      <p style={{ color: "#718096", fontSize: "0.9rem", marginBottom: "1rem" }}>
        Esta página permite al admin simular al receptor para probar el flujo del timbre.
        Las sesiones PENDING se actualizan automáticamente cada 3 segundos.
      </p>

      {error && <div style={{ background: "#fed7d7", color: "#c53030", padding: "0.5rem", borderRadius: "6px", marginBottom: "1rem" }}>{error}</div>}

      {sessions.length === 0 ? (
        <div style={{ background: "white", padding: "2rem", borderRadius: "10px", textAlign: "center", color: "#a0aec0" }}>
          No hay sesiones PENDING. Escanea un QR y toca el timbre desde la app.
        </div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
          {sessions.map((s) => (
            <div key={s.id} style={{ background: "white", padding: "1rem 1.25rem", borderRadius: "10px", boxShadow: "0 2px 4px rgba(0,0,0,0.06)" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: "0.75rem" }}>
                <div>
                  <div style={{ fontWeight: 600, fontSize: "0.95rem" }}>
                    Sesión: <code style={{ color: "#48bb78" }}>{s.id.slice(0, 8)}</code>
                  </div>
                  <div style={{ fontSize: "0.85rem", color: "#718096", marginTop: "0.25rem" }}>
                    Emisor: <strong>{s.emisorName || "Visitante"}</strong> (ID: {s.emisorId.slice(0, 8)})
                  </div>
                </div>
                <span style={{
                  padding: "2px 10px", borderRadius: "12px", fontSize: "0.75rem", fontWeight: 600,
                  background: "#fefcbf", color: "#975a16",
                }}>PENDING</span>
              </div>
              <div style={{ display: "flex", gap: "0.5rem", flexWrap: "wrap" }}>
                <button disabled={busyId === s.id} onClick={() => accept(s.id, "video")} style={{ ...btnStyle, background: "#48bb78" }}>
                  {busyId === s.id ? "Aceptando..." : "Aceptar Video"}
                </button>
                <button disabled={busyId === s.id} onClick={() => accept(s.id, "audio")} style={{ ...btnStyle, background: "#4299e1" }}>
                  Aceptar Audio
                </button>
                <button disabled={busyId === s.id} onClick={() => accept(s.id, "chat")} style={{ ...btnStyle, background: "#ed8936" }}>
                  Aceptar Chat
                </button>
                <button disabled={busyId === s.id} onClick={() => reject(s.id)} style={{ ...btnStyle, background: "#e53e3e" }}>
                  Rechazar
                </button>
              </div>
              <div style={{ fontSize: "0.8rem", color: "#a0aec0", marginTop: "0.5rem" }}>
                Creada: {new Date(s.createdAt).toLocaleTimeString()}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

const btnStyle: React.CSSProperties = {
  padding: "0.4rem 0.85rem", color: "white", border: "none", borderRadius: "6px", cursor: "pointer", fontWeight: 600, fontSize: "0.85rem",
};