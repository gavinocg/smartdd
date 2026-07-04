import { useState, useEffect } from "react";
import { get } from "../api/client";

interface LogEntry {
  id: string;
  action: string;
  targetId: string | null;
  details: unknown;
  createdAt: string;
  admin: { name: string; email: string };
}

interface LogsResponse {
  logs: LogEntry[];
  pagination: { page: number; limit: number; total: number; totalPages: number };
}

const actionLabels: Record<string, string> = {
  change_plan: "Cambio de plan",
  suspend_user: "Suspensión",
  unsuspend_user: "Reactivación",
};

export default function Logs() {
  const [data, setData] = useState<LogsResponse | null>(null);
  const [page, setPage] = useState(1);
  const [error, setError] = useState("");

  useEffect(() => {
    get<LogsResponse>(`/admin/logs?page=${page}&limit=50`)
      .then(setData)
      .catch((e) => setError(e.message));
  }, [page]);

  if (error) return <div style={{ color: "#c53030" }}>{error}</div>;
  if (!data) return <div style={{ color: "#718096" }}>Cargando...</div>;

  return (
    <div>
      <h2 style={{ marginTop: 0 }}>Registro de Actividad</h2>
      <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
        {data.logs.map((log) => (
          <div key={log.id} style={{ background: "white", padding: "0.75rem 1rem", borderRadius: "8px", boxShadow: "0 1px 3px rgba(0,0,0,0.06)", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <div>
              <div style={{ fontWeight: 600, fontSize: "0.9rem" }}>{actionLabels[log.action] || log.action}</div>
              <div style={{ fontSize: "0.8rem", color: "#718096" }}>
                {log.admin?.name || "—"} ({log.admin?.email || "—"})
              </div>
            </div>
            <div style={{ fontSize: "0.8rem", color: "#a0aec0" }}>
              {new Date(log.createdAt).toLocaleString()}
            </div>
          </div>
        ))}
      </div>
      {data.pagination.totalPages > 1 && (
        <div style={{ display: "flex", justifyContent: "center", gap: "0.5rem", marginTop: "1rem" }}>
          <button disabled={page <= 1} onClick={() => setPage(page - 1)} style={btnStyle}>Anterior</button>
          <span style={{ padding: "0.5rem", color: "#718096" }}>{page} / {data.pagination.totalPages}</span>
          <button disabled={page >= data.pagination.totalPages} onClick={() => setPage(page + 1)} style={btnStyle}>Siguiente</button>
        </div>
      )}
    </div>
  );
}

const btnStyle: React.CSSProperties = {
  padding: "0.5rem 1rem", background: "#48bb78", color: "white", border: "none", borderRadius: "6px", cursor: "pointer", fontWeight: 600,
};
