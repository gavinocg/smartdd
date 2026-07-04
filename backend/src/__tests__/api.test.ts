import "dotenv/config";

const BASE_URL = process.env.TEST_API_URL || "http://localhost:8000/api/v1";
const TEST_EMAIL = `test_${Date.now()}@smartdd.test`;
const TEST_PASSWORD = "Test123456!";
let authToken: string;
let qrUuid: string;
let qrId: string;
let sessionId: string;
let roomId: string;

function skipIfNoServer(): boolean {
  return !process.env.TEST_API_URL && !process.env.CI;
}

beforeAll(async () => {
  if (skipIfNoServer()) {
    console.warn("⚠️  No TEST_API_URL set — skipping integration tests. Run with: TEST_API_URL=http://localhost:8000/api/v1 npx jest");
    return;
  }
});

describe("Health", () => {
  it("GET /health returns 200", async () => {
    if (skipIfNoServer()) return;
    const res = await fetch(`${BASE_URL}/health`);
    expect(res.status).toBe(200);
    const body = await res.json();
    expect(body.status).toBe("ok");
  });
});

describe("Auth", () => {
  it("POST /auth/register creates a user", async () => {
    if (skipIfNoServer()) return;
    const res = await fetch(`${BASE_URL}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name: "Test User", email: TEST_EMAIL, password: TEST_PASSWORD }),
    });
    expect(res.status).toBe(201);
  });

  it("POST /auth/register rejects duplicate email", async () => {
    if (skipIfNoServer()) return;
    const res = await fetch(`${BASE_URL}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ name: "Test User", email: TEST_EMAIL, password: TEST_PASSWORD }),
    });
    expect(res.status).toBe(409);
  });

  it("POST /auth/login returns token", async () => {
    if (skipIfNoServer()) return;
    const res = await fetch(`${BASE_URL}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: TEST_EMAIL, password: TEST_PASSWORD }),
    });
    expect(res.status).toBe(200);
    const body = await res.json();
    expect(body.token).toBeDefined();
    expect(body.user).toBeDefined();
    authToken = body.token;
  });
});

describe("QR Codes", () => {
  it("POST /qr creates a QR code", async () => {
    if (skipIfNoServer() || !authToken) return;
    const res = await fetch(`${BASE_URL}/qr`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${authToken}` },
      body: JSON.stringify({ lat: -23.5505, lng: -46.6333, radiusMeters: 50 }),
    });
    expect(res.status).toBe(201);
    const body = await res.json();
    expect(body.uuid).toBeDefined();
    expect(body.id).toBeDefined();
    qrUuid = body.uuid;
    qrId = body.id;
  });

  it("GET /qr returns user QR codes", async () => {
    if (skipIfNoServer() || !authToken) return;
    const res = await fetch(`${BASE_URL}/qr`, {
      headers: { Authorization: `Bearer ${authToken}` },
    });
    expect(res.status).toBe(200);
    const body = await res.json();
    expect(Array.isArray(body)).toBe(true);
    expect(body.length).toBeGreaterThanOrEqual(1);
  });
});

describe("Ring Session", () => {
  it("POST /ring/init creates a session", async () => {
    if (skipIfNoServer() || !authToken) return;
    const res = await fetch(`${BASE_URL}/ring/init`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${authToken}` },
      body: JSON.stringify({ qrUuid, emisorName: "Door Visitor" }),
    });
    expect(res.status).toBe(201);
    const body = await res.json();
    expect(body.sessionId).toBeDefined();
    expect(body.roomId).toBeDefined();
    sessionId = body.sessionId;
    roomId = body.roomId;
  });
});

describe("Config", () => {
  it("GET /config returns user config", async () => {
    if (skipIfNoServer() || !authToken) return;
    const res = await fetch(`${BASE_URL}/config`, {
      headers: { Authorization: `Bearer ${authToken}` },
    });
    expect(res.status).toBe(200);
    const body = await res.json();
    expect(body.defaultMode).toBeDefined();
  });

  it("PUT /config updates user config", async () => {
    if (skipIfNoServer() || !authToken) return;
    const res = await fetch(`${BASE_URL}/config`, {
      method: "PUT",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${authToken}` },
      body: JSON.stringify({ defaultMode: "AUDIO" }),
    });
    expect(res.status).toBe(200);
  });
});

describe("Devices", () => {
  it("POST /devices registers an FCM token", async () => {
    if (skipIfNoServer() || !authToken) return;
    const res = await fetch(`${BASE_URL}/devices`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${authToken}` },
      body: JSON.stringify({ token: "test-fcm-token-123", platform: "android" }),
    });
    expect([200, 201]).toContain(res.status);
  });

  it("GET /devices lists devices", async () => {
    if (skipIfNoServer() || !authToken) return;
    const res = await fetch(`${BASE_URL}/devices`, {
      headers: { Authorization: `Bearer ${authToken}` },
    });
    expect(res.status).toBe(200);
    const body = await res.json();
    expect(Array.isArray(body)).toBe(true);
  });
});
