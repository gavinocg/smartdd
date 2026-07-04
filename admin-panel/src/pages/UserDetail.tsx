import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getUser, changePlan, toggleSuspend } from "../api/users";

export default function UserDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [user, setUser] = useState<any>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!id) return;
    getUser(id).then((data) => setUser(data.user)).catch((e) => setError(e.message));
  }, [id]);

  if (error) return <div style={{ color: "#c53030" }}>{error}</div>;
  if (!user) return <div style={{ color: "#718096" }}>Cargando...</div>;

  async function handlePlan() {
    const plans = ["FREE", "PRO", "B2P"];
    const idx = plans.indexOf(user.plan);
    const next = plans[(idx + 1) % plans.length];
    setBusy(true);
    try {
      await changePlan(id!, next);
      setUser({ ...user, plan: next });
    } catch (e: any) {
      setError(e.message);
    } finally { setBusy(false); }
  }

  async function handleSuspend() {
    setBusy(true);
    try {
      await toggleSuspend(id!);
      setUser({ ...user, active: !user.active });
    } catch (e: any) { setError(e.message); }
    finally { setBusy(false); }
  }

  return (
    <div>
      <button onClick={() => navigate("/users")} style={{ background: "none", border: "none", color: "#48bb78", cursor: "pointer", marginBottom: "1rem", padding: 0, fontSize: "0.9rem" }}>
        &larr; Volver a usuarios
      </button>
      {error && <div style={{ background: "#fed7d7", color: "#c53030", padding: "0.5rem", borderRadius: "6px", marginBottom: "1rem" }}>{error}</div>}
      <div style={{ background: "white", padding: "1.5rem", borderRadius: "10px", boxShadow: "0 2px 4px rgba(0,0,0,0.06)" }}>
        <h2 style={{ margin: "0 0 0.25rem" }}>{user.name}</h2>
        <p style={{ color: "#718096", margin: "0 0 1.5rem" }}>{user.email}</p>
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem", marginBottom: "1.5rem" }}>
          <div><strong>Plan:</strong> {user.plan}</div>
          <div><strong>Rol:</strong> {user.role}</div>
          <div><strong>Estado:</strong> <span style={{ color: user.active ? "#38a169" : "#e53e3e" }}>{user.active ? "Activo" : "Suspendido"}</span></div>
          <div><strong>Creado:</strong> {new Date(user.createdAt).toLocaleDateString()}</div>
        </div>
        <div style={{ display: "flex", gap: "0.75rem" }}>
          <button disabled={busy} onClick={handlePlan} style={{ ...btnStyle, background: "#4299e1" }}>
            Cambiar plan ({user.plan})
          </button>
          <button disabled={busy} onClick={handleSuspend} style={{ ...btnStyle, background: user.active ? "#e53e3e" : "#38a169" }}>
            {user.active ? "Suspender" : "Reactivar"}
          </button>
        </div>
      </div>
    </div>
  );
}

const btnStyle: React.CSSProperties = {
  padding: "0.5rem 1rem", color: "white", border: "none", borderRadius: "6px", cursor: "pointer", fontWeight: 600, fontSize: "0.9rem",
};
