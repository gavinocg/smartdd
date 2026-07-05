import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import type { ReactNode } from "react";

export default function Layout() {
  const { user, logout } = useAuth();

  return (
    <div style={{ display: "flex", minHeight: "100vh", background: "#f7fafc" }}>
      <Sidebar>
        <div style={{ padding: "0 1rem", marginBottom: "2rem" }}>
          <div style={{ background: "#48bb78", color: "white", padding: "4px 12px", borderRadius: "20px", fontSize: "0.8rem", display: "inline-block", fontWeight: 600 }}>SMARTDD</div>
        </div>
        <NavItem to="/">Dashboard</NavItem>
        <NavItem to="/console">Consola</NavItem>
        <NavItem to="/users">Usuarios</NavItem>
        <NavItem to="/logs">Actividad</NavItem>
        <NavItem to="/config">Configuración</NavItem>
        <NavItem to="/ring-debug">Ring Debug</NavItem>
        <div style={{ marginTop: "auto", padding: "1rem" }}>
          <div style={{ fontSize: "0.85rem", color: "#a0aec0", marginBottom: "0.5rem" }}>{user?.email}</div>
          <button onClick={logout} style={{ width: "100%", padding: "0.5rem", background: "none", border: "1px solid #e2e8f0", borderRadius: "6px", cursor: "pointer", color: "#718096", fontSize: "0.85rem" }}>Cerrar sesión</button>
        </div>
      </Sidebar>
      <main style={{ flex: 1, padding: "2rem", overflow: "auto" }}>
        <Outlet />
      </main>
    </div>
  );
}

function Sidebar({ children }: { children: ReactNode }) {
  return (
    <nav style={{ width: "220px", background: "white", borderRight: "1px solid #e2e8f0", display: "flex", flexDirection: "column", padding: "1.5rem 0" }}>
      {children}
    </nav>
  );
}

function NavItem({ to, children }: { to: string; children: ReactNode }) {
  return (
    <NavLink
      to={to}
      end={to === "/"}
      style={({ isActive }) => ({
        display: "block",
        padding: "0.65rem 1rem",
        color: isActive ? "#48bb78" : "#4a5568",
        fontWeight: isActive ? 600 : 400,
        background: isActive ? "#f0fff4" : "transparent",
        borderRight: isActive ? "3px solid #48bb78" : "3px solid transparent",
        textDecoration: "none",
        fontSize: "0.9rem",
      })}
    >
      {children}
    </NavLink>
  );
}
