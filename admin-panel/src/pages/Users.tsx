import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { getUsers, createUser } from "../api/users";
import type { User } from "../api/auth";

export default function Users() {
  const [users, setUsers] = useState<User[]>([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [search, setSearch] = useState("");
  const [plan, setPlan] = useState("");
  const [error, setError] = useState("");
  const [showModal, setShowModal] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    getUsers(page, search || undefined, plan || undefined)
      .then((data) => { setUsers(data.users); setTotalPages(data.pagination.totalPages); })
      .catch((e) => setError(e.message));
  }, [page, search, plan]);

  if (error) return <div style={{ color: "#c53030" }}>{error}</div>;

  return (
    <div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1rem" }}>
        <h2 style={{ margin: 0 }}>Usuarios</h2>
        <button onClick={() => setShowModal(true)} style={{ ...btnStyle, background: "#48bb78" }}>+ Crear usuario</button>
      </div>
      <div style={{ display: "flex", gap: "0.75rem", marginBottom: "1rem" }}>
        <input
          style={inputStyle}
          placeholder="Buscar por nombre o email..."
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(1); }}
        />
        <select style={inputStyle} value={plan} onChange={(e) => { setPlan(e.target.value); setPage(1); }}>
          <option value="">Todos los planes</option>
          <option value="FREE">FREE</option>
          <option value="PRO">PRO</option>
          <option value="B2P">B2P</option>
        </select>
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
        {users.map((u) => (
          <div
            key={u.id}
            style={{ background: "white", padding: "1rem", borderRadius: "8px", boxShadow: "0 1px 3px rgba(0,0,0,0.06)", cursor: "pointer", display: "flex", justifyContent: "space-between", alignItems: "center" }}
            onClick={() => navigate(`/users/${u.id}`)}
          >
            <div>
              <div style={{ fontWeight: 600 }}>{u.name}</div>
              <div style={{ fontSize: "0.85rem", color: "#718096" }}>{u.email}</div>
            </div>
            <div style={{ display: "flex", gap: "0.5rem", alignItems: "center" }}>
              <span style={{
                padding: "2px 8px", borderRadius: "12px", fontSize: "0.75rem", fontWeight: 600,
                background: u.plan === "PRO" ? "#c6f6d5" : u.plan === "B2P" ? "#e9d8fd" : "#edf2f7",
                color: u.plan === "PRO" ? "#276749" : u.plan === "B2P" ? "#553c9a" : "#4a5568",
              }}>{u.plan}</span>
              <span style={{ color: u.active ? "#38a169" : "#e53e3e", fontSize: "0.85rem" }}>
                {u.active ? "Activo" : "Suspendido"}
              </span>
            </div>
          </div>
        ))}
      </div>
      {totalPages > 1 && (
        <div style={{ display: "flex", justifyContent: "center", gap: "0.5rem", marginTop: "1rem" }}>
          <button disabled={page <= 1} onClick={() => setPage(page - 1)} style={btnStyle}>Anterior</button>
          <span style={{ padding: "0.5rem", color: "#718096" }}>{page} / {totalPages}</span>
          <button disabled={page >= totalPages} onClick={() => setPage(page + 1)} style={btnStyle}>Siguiente</button>
        </div>
      )}

      {showModal && <CreateUserModal onClose={() => setShowModal(false)} onCreated={() => { setShowModal(false); getUsers(page, search || undefined, plan || undefined).then(d => { setUsers(d.users); setTotalPages(d.pagination.totalPages); }); }} />}
    </div>
  );
}

function CreateUserModal({ onClose, onCreated }: { onClose: () => void; onCreated: () => void }) {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [plan, setPlan] = useState("FREE");
  const [role, setRole] = useState("user");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      await createUser({ name, email, password, plan, role });
      onCreated();
    } catch (err: any) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.4)", display: "flex", alignItems: "center", justifyContent: "center", zIndex: 1000 }} onClick={onClose}>
      <div style={{ background: "white", padding: "2rem", borderRadius: "12px", width: "420px", maxWidth: "90vw" }} onClick={e => e.stopPropagation()}>
        <h3 style={{ margin: "0 0 1.25rem" }}>Crear usuario</h3>
        {error && <div style={{ background: "#fed7d7", color: "#c53030", padding: "0.5rem", borderRadius: "6px", marginBottom: "1rem", fontSize: "0.9rem" }}>{error}</div>}
        <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
          <input style={inputStyle} placeholder="Nombre" value={name} onChange={e => setName(e.target.value)} required />
          <input style={inputStyle} placeholder="Email" type="email" value={email} onChange={e => setEmail(e.target.value)} required />
          <input style={inputStyle} placeholder="Contraseña" type="password" value={password} onChange={e => setPassword(e.target.value)} required minLength={6} />
          <div style={{ display: "flex", gap: "0.5rem" }}>
            <select style={{ ...inputStyle, flex: 1 }} value={plan} onChange={e => setPlan(e.target.value)}>
              <option value="FREE">FREE</option>
              <option value="PRO">PRO</option>
              <option value="B2P">B2P</option>
            </select>
            <select style={{ ...inputStyle, flex: 1 }} value={role} onChange={e => setRole(e.target.value)}>
              <option value="user">Usuario</option>
              <option value="admin">Admin</option>
              <option value="business">Business</option>
            </select>
          </div>
          <div style={{ display: "flex", gap: "0.5rem", justifyContent: "flex-end", marginTop: "0.5rem" }}>
            <button type="button" onClick={onClose} style={{ ...btnStyle, background: "#a0aec0" }}>Cancelar</button>
            <button type="submit" disabled={busy} style={{ ...btnStyle, background: "#48bb78" }}>{busy ? "Creando..." : "Crear"}</button>
          </div>
        </form>
      </div>
    </div>
  );
}

const inputStyle: React.CSSProperties = {
  padding: "0.5rem 0.75rem", border: "1px solid #e2e8f0", borderRadius: "8px", fontSize: "0.9rem", flex: 1,
};
const btnStyle: React.CSSProperties = {
  padding: "0.5rem 1rem", color: "white", border: "none", borderRadius: "6px", cursor: "pointer", fontWeight: 600,
};