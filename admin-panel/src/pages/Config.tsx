import { useState, useEffect } from "react";
import { get, put } from "../api/client";

interface GeneralConfig {
  defaultTimeoutSeconds: number;
  defaultRadiusMeters: number;
  maintenanceMode: boolean;
  maxQrPerUser: number;
}

export default function Config() {
  const [config, setConfig] = useState<GeneralConfig | null>(null);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    get<GeneralConfig>("/admin/config/general")
      .then(setConfig)
      .catch((e) => setError(e.message));
  }, []);

  function update(key: keyof GeneralConfig, value: number | boolean) {
    if (!config) return;
    setConfig({ ...config, [key]: value });
  }

  async function save() {
    if (!config) return;
    setSaving(true);
    setMessage("");
    setError("");
    try {
      await put("/admin/config/general", config);
      setMessage("Configuración guardada correctamente");
    } catch (e: any) {
      setError(e.message);
    } finally {
      setSaving(false);
    }
  }

  if (error && !config) return <div style={{ color: "#c53030" }}>{error}</div>;
  if (!config) return <div style={{ color: "#718096" }}>Cargando...</div>;

  return (
    <div style={{ maxWidth: "600px" }}>
      <h2 style={{ marginTop: 0 }}>Configuración del sistema</h2>

      <div style={{ background: "white", padding: "1.5rem", borderRadius: "10px", boxShadow: "0 2px 4px rgba(0,0,0,0.06)" }}>
        <div style={{ marginBottom: "1.5rem" }}>
          <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: 600, color: "#4a5568" }}>
            Timeout predeterminado: <strong>{config.defaultTimeoutSeconds}s</strong>
          </label>
          <input type="range" min="10" max="300" value={config.defaultTimeoutSeconds}
            onChange={(e) => update("defaultTimeoutSeconds", parseInt(e.target.value))}
            style={{ width: "100%" }} />
          <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.8rem", color: "#a0aec0" }}>
            <span>10s</span><span>300s</span>
          </div>
        </div>

        <div style={{ marginBottom: "1.5rem" }}>
          <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: 600, color: "#4a5568" }}>
            Radio de geocerca: <strong>{config.defaultRadiusMeters}m</strong>
          </label>
          <input type="range" min="10" max="1000" value={config.defaultRadiusMeters}
            onChange={(e) => update("defaultRadiusMeters", parseInt(e.target.value))}
            style={{ width: "100%" }} />
          <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.8rem", color: "#a0aec0" }}>
            <span>10m</span><span>1000m</span>
          </div>
        </div>

        <div style={{ marginBottom: "1.5rem" }}>
          <label style={{ display: "block", marginBottom: "0.5rem", fontWeight: 600, color: "#4a5568" }}>
            Máximo QRs por usuario: <strong>{config.maxQrPerUser}</strong>
          </label>
          <input type="range" min="1" max="100" value={config.maxQrPerUser}
            onChange={(e) => update("maxQrPerUser", parseInt(e.target.value))}
            style={{ width: "100%" }} />
          <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.8rem", color: "#a0aec0" }}>
            <span>1</span><span>100</span>
          </div>
        </div>

        <div style={{ marginBottom: "1.5rem", display: "flex", alignItems: "center", gap: "0.75rem" }}>
          <label style={{ fontWeight: 600, color: "#4a5568" }}>Modo mantenimiento</label>
          <button onClick={() => update("maintenanceMode", !config.maintenanceMode)}
            style={{ padding: "6px 20px", borderRadius: "20px", border: "none", cursor: "pointer", fontWeight: 600, fontSize: "0.85rem", background: config.maintenanceMode ? "#fc8181" : "#48bb78", color: "white" }}>
            {config.maintenanceMode ? "ACTIVADO" : "DESACTIVADO"}
          </button>
          <span style={{ fontSize: "0.8rem", color: "#718096" }}>
            {config.maintenanceMode ? "Bloquea nuevos registros e inicios de sesión" : "Sistema operativo normalmente"}
          </span>
        </div>

        {message && <div style={{ padding: "0.75rem", background: "#f0fff4", borderRadius: "6px", color: "#48bb78", marginBottom: "1rem", fontSize: "0.9rem" }}>{message}</div>}
        {error && <div style={{ padding: "0.75rem", background: "#fff5f5", borderRadius: "6px", color: "#fc8181", marginBottom: "1rem", fontSize: "0.9rem" }}>{error}</div>}

        <button onClick={save} disabled={saving}
          style={{ padding: "10px 24px", borderRadius: "8px", border: "none", background: "#48bb78", color: "white", cursor: "pointer", fontWeight: 600, fontSize: "0.95rem", opacity: saving ? 0.6 : 1 }}>
          {saving ? "Guardando..." : "Guardar configuración"}
        </button>
      </div>
    </div>
  );
}
