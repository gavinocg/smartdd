import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getUser, changePlan, toggleSuspend, updateUser, resetPassword, deleteUser } from "../api/users";

export default function UserDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [user, setUser] = useState<any>(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const [editing, setEditing] = useState(false);
  const [editName, setEditName] = useState("");
  const [editEmail, setEditEmail] = useState("");
  const [editPlan, setEditPlan] = useState("");
  const [editRole, setEditRole] = useState("");

  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [newPassword, setNewPassword] = useState("");

  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  useEffect(() => {
    if (!id) return;
    getUser(id).then((data) => {
      setUser(data.user);
      setEditName(data.user.name);
      setEditEmail(data.user.email);
      setEditPlan(data.user.plan);
      setEditRole(data.user.role);
    }).catch((e) => setError(e.message));
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
      setEditPlan(next);
    } catch (e: any) { setError(e.message); }
    finally { setBusy(false); }
  }

  async function handleSuspend() {
    setBusy(true);
    try {
      await toggleSuspend(id!);
      setUser({ ...user, active: !user.active });
    } catch (e: any) { setError(e.message); }
    finally { setBusy(false); }
  }

  async function handleSave() {
    setBusy(true);
    setError("");
    try {
      const updated = await updateUser(id!, { name: editName, email: editEmail, plan: editPlan as any, role: editRole as any });
      setUser({ ...user, ...updated.user });
      setEditing(false);
    } catch (e: any) { setError(e.message); }
    finally { setBusy(false); }
  }

  async function handleResetPassword() {
    setBusy(true);
    setError("");
    try {
      await resetPassword(id!, newPassword);
      setShowPasswordModal(false);
      setNewPassword("");
    } catch (e: any) { setError(e.message); }
    finally { setBusy(false); }
  }

  async function handleDelete() {
    setBusy(true);
    setError("");
    try {
      await deleteUser(id!);
      navigate("/users");
    } catch (e: any) { setError(e.message); }
    finally { setBusy(false); }
  }

  return (
    <div>
      <button onClick={() => navigate("/users")} style={{ background: "none", border: "none", color: "#48bb78", cursor: "pointer", marginBottom: "1rem", padding: 0, fontSize: "0.9rem" }}>
        &larr; Volver a usuarios
      </button>
      {error && <div style={{ background: "#fed7d7", color: "#c53030", padding: "0.5rem", borderRadius: "6px", marginBottom: "1rem" }}>{error}</div>}

      <div style={{ background: "white", padding: "1.5rem", borderRadius: "10px", boxShadow: "0 2px 4px rgba(0,0,0,0.06)", marginBottom: "1rem" }}>
        {editing ? (
          <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
            <input style={inputStyle} value={editName} onChange={e => setEditName(e.target.value)} placeholder="Nombre" />
            <input style={inputStyle} value={editEmail} onChange={e => setEditEmail(e.target.value)} placeholder="Email" />
            <div style={{ display: "flex", gap: "0.5rem" }}>
              <select style={{ ...inputStyle, flex: 1 }} value={editPlan} onChange={e => setEditPlan(e.target.value)}>
                <option value="FREE">FREE</option>
                <option value="PRO">PRO</option>
                <option value="B2P">B2P</option>
              </select>
              <select style={{ ...inputStyle, flex: 1 }} value={editRole} onChange={e => setEditRole(e.target.value)}>
                <option value="user">Usuario</option>
                <option value="admin">Admin</option>
                <option value="business">Business</option>
              </select>
            </div>
            <div style={{ display: "flex", gap: "0.5rem" }}>
              <button disabled={busy} onClick={() => setEditing(false)} style={{ ...btnStyle, background: "#a0aec0" }}>Cancelar</button>
              <button disabled={busy} onClick={handleSave} style={{ ...btnStyle, background: "#48bb78" }}>{busy ? "Guardando..." : "Guardar"}</button>
            </div>
          </div>
        ) : (
          <>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
              <div>
                <h2 style={{ margin: "0 0 0.25rem" }}>{user.name}</h2>
                <p style={{ color: "#718096", margin: "0 0 1.5rem" }}>{user.email}</p>
              </div>
              <button onClick={() => setEditing(true)} style={{ ...btnStyle, background: "#4299e1", fontSize: "0.85rem" }}>Editar</button>
            </div>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem", marginBottom: "1.5rem" }}>
              <div><strong>Plan:</strong> {user.plan}</div>
              <div><strong>Rol:</strong> {user.role}</div>
              <div><strong>Estado:</strong> <span style={{ color: user.active ? "#38a169" : "#e53e3e" }}>{user.active ? "Activo" : "Suspendido"}</span></div>
              <div><strong>Creado:</strong> {new Date(user.createdAt).toLocaleDateString()}</div>
            </div>
          </>
        )}
      </div>

      <div style={{ display: "flex", gap: "0.75rem", flexWrap: "wrap" }}>
        <button disabled={busy} onClick={handlePlan} style={{ ...btnStyle, background: "#4299e1" }}>
          Cambiar plan ({user.plan})
        </button>
        <button disabled={busy} onClick={handleSuspend} style={{ ...btnStyle, background: user.active ? "#e53e3e" : "#38a169" }}>
          {user.active ? "Suspender" : "Reactivar"}
        </button>
        <button disabled={busy} onClick={() => setShowPasswordModal(true)} style={{ ...btnStyle, background: "#ed8936" }}>
          Resetear contraseña
        </button>
        <button disabled={busy} onClick={() => setShowDeleteConfirm(true)} style={{ ...btnStyle, background: "#c53030" }}>
          Eliminar
        </button>
      </div>

      {showPasswordModal && (
        <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.4)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 1000 }} onClick={() => setShowPasswordModal(false)}>
          <div style={{ background: "white", padding: "2rem", borderRadius: "12px", width: "360px", maxWidth: "90vw" }} onClick={e => e.stopPropagation()}>
            <h3 style={{ margin: "0 0 1rem" }}>Resetear contraseña</h3>
            <p style={{ color: "#718096", fontSize: "0.85rem", margin: "0 0 1rem" }}>Nueva contraseña para <strong>{user.name}</strong></p>
            <input style={{ ...inputStyle, width: "100%", boxSizing: "border-box" }} type="password" placeholder="Nueva contraseña (mín. 6 caracteres)" value={newPassword} onChange={e => setNewPassword(e.target.value)} minLength={6} />
            <div style={{ display: "flex", gap: "0.5rem", justifyContent: "flex-end", marginTop: "1rem" }}>
              <button onClick={() => { setShowPasswordModal(false); setNewPassword(""); }} style={{ ...btnStyle, background: "#a0aec0" }}>Cancelar</button>
              <button disabled={busy || newPassword.length < 6} onClick={handleResetPassword} style={{ ...btnStyle, background: "#ed8936" }}>{busy ? "Guardando..." : "Guardar"}</button>
            </div>
          </div>
        </div>
      )}

      {showDeleteConfirm && (
        <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.4)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 1000 }} onClick={() => setShowDeleteConfirm(false)}>
          <div style={{ background: "white", padding: "2rem", borderRadius: "12px", width: "400px", maxWidth: "90vw" }} onClick={e => e.stopPropagation()}>
            <h3 style={{ margin: "0 0 0.5rem", color: "#c53030" }}>¿Eliminar usuario?</h3>
            <p style={{ color: "#718096", fontSize: "0.9rem", margin: "0 0 1.5rem" }}>
              Se eliminarán permanentemente todos los datos de <strong>{user.name}</strong> ({user.email}):<br />
              QRs, sesiones, dispositivos y configuración. Esta acción no se puede deshacer.
            </p>
            <div style={{ display: "flex", gap: "0.5rem", justifyContent: "flex-end" }}>
              <button onClick={() => setShowDeleteConfirm(false)} style={{ ...btnStyle, background: "#a0aec0" }}>Cancelar</button>
              <button disabled={busy} onClick={handleDelete} style={{ ...btnStyle, background: "#c53030" }}>{busy ? "Eliminando..." : "Sí, eliminar"}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

const inputStyle: React.CSSProperties = {
  padding: "0.5rem 0.75rem", border: "1px solid #e2e8f0", borderRadius: "8px", fontSize: "0.9rem",
};
const btnStyle: React.CSSProperties = {
  padding: "0.5rem 1rem", color: "white", border: "none", borderRadius: "6px", cursor: "pointer", fontWeight: 600, fontSize: "0.9rem",
};