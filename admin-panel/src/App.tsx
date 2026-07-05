import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider, useAuth } from "./context/AuthContext";
import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Users from "./pages/Users";
import UserDetail from "./pages/UserDetail";
import Logs from "./pages/Logs";
import Console from "./pages/Console";
import Config from "./pages/Config";
import RingDebug from "./pages/RingDebug";
import ReceptorSimulator from "./pages/ReceptorSimulator";
import Layout from "./components/Layout";
import type { ReactNode } from "react";

function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth();
  if (loading) return <div style={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100vh", color: "#718096" }}>Cargando...</div>;
  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== "admin") return <div style={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100vh", color: "#e53e3e" }}>Acceso denegado</div>;
  return <>{children}</>;
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Dashboard />} />
            <Route path="users" element={<Users />} />
            <Route path="users/:id" element={<UserDetail />} />
            <Route path="logs" element={<Logs />} />
            <Route path="console" element={<Console />} />
            <Route path="config" element={<Config />} />
            <Route path="ring-debug" element={<RingDebug />} />
            <Route path="receptor-sim" element={<ReceptorSimulator />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
