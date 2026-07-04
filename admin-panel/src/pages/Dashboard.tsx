import { useState, useEffect } from "react";
import { get } from "../api/client";

interface Stats {
  totalUsers: number;
  freeUsers: number;
  proUsers: number;
  b2pUsers: number;
  todayQRs: number;
  activeSessions: number;
  totalQRs: number;
  totalSessions: number;
}

export default function Dashboard() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    get<Stats>("/admin/stats")
      .then(setStats)
      .catch((e) => setError(e.message));
  }, []);

  if (error) return <div style={{ color: "#c53030" }}>{error}</div>;
  if (!stats) return <div style={{ color: "#718096" }}>Cargando...</div>;

  const cards = [
    { label: "Usuarios totales", value: stats.totalUsers, color: "#48bb78" },
    { label: "Plan FREE", value: stats.freeUsers, color: "#a0aec0" },
    { label: "Plan PRO", value: stats.proUsers, color: "#4299e1" },
    { label: "Plan B2P", value: stats.b2pUsers, color: "#9f7aea" },
    { label: "QRs hoy", value: stats.todayQRs, color: "#ed8936" },
    { label: "Sesiones activas", value: stats.activeSessions, color: "#fc8181" },
    { label: "Total QRs", value: stats.totalQRs, color: "#667eea" },
    { label: "Total sesiones", value: stats.totalSessions, color: "#38b2ac" },
  ];

  return (
    <div>
      <h2 style={{ marginTop: 0 }}>Dashboard</h2>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(200px, 1fr))", gap: "1rem" }}>
        {cards.map((c) => (
          <div key={c.label} style={{ background: "white", padding: "1.25rem", borderRadius: "10px", boxShadow: "0 2px 4px rgba(0,0,0,0.06)" }}>
            <div style={{ fontSize: "0.85rem", color: "#718096", marginBottom: "0.25rem" }}>{c.label}</div>
            <div style={{ fontSize: "2rem", fontWeight: 700, color: c.color }}>{c.value}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
