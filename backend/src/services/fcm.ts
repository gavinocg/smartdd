import { prisma } from "./prisma";

let fcmInitialized = false;
// eslint-disable-next-line @typescript-eslint/no-explicit-any
let fcmApp: any = null;

type FirebaseAdmin = {
  default: {
    apps: any[];
    initializeApp: (opts: any) => void;
    credential: { cert: (sa: any) => any; applicationDefault: () => any };
    messaging: () => { send: (msg: any) => Promise<any> };
  };
};

async function getFirebaseApp() {
  if (fcmInitialized) return true;

  try {
    const mod = (await import("firebase-admin")) as unknown as FirebaseAdmin;
    const admin = mod.default;

    if (admin.apps && admin.apps.length > 0) { fcmApp = admin.apps[0]; fcmInitialized = true; return true; }

    if (process.env.FIREBASE_SERVICE_ACCOUNT) {
      const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
      admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
      console.log("[FCM] Inicializado con FIREBASE_SERVICE_ACCOUNT");
    } else if (process.env.FIREBASE_SERVICE_ACCOUNT_PATH) {
      admin.initializeApp({ credential: admin.credential.applicationDefault(), projectId: process.env.FCM_PROJECT_ID });
      console.log("[FCM] Inicializado con ruta: " + process.env.FIREBASE_SERVICE_ACCOUNT_PATH);
    } else if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
      admin.initializeApp({ credential: admin.credential.applicationDefault() });
      console.log("[FCM] Inicializado con GOOGLE_APPLICATION_CREDENTIALS");
    } else {
      console.log("[FCM] No hay configuración de Firebase. Push notificaciones inactivas.");
      return false;
    }

    fcmInitialized = true;
    return true;
  } catch (err) {
    console.error("[FCM] Error al inicializar Firebase:", err);
    return false;
  }
}

export async function sendPushNotification(
  token: string,
  title: string,
  body: string,
  data?: Record<string, string>
): Promise<boolean> {
  const ready = await getFirebaseApp();
  if (!ready) return false;

  try {
    const mod = (await import("firebase-admin")) as unknown as FirebaseAdmin;
    await mod.default.messaging().send({
      token,
      notification: { title, body },
      data,
      android: {
        priority: "high",
        notification: {
          channelId: data?.type === "incoming_ring" ? "ring_notifications" : "chat_notifications",
          priority: "high",
          sound: "default",
          visibility: 1,
        },
      },
    });
    return true;
  } catch (err: any) {
    if (err.code === "messaging/registration-token-not-registered") {
      // Token inválido, desactivar dispositivo
      try {
        await prisma.device.updateMany({
          where: { token, active: true },
          data: { active: false },
        });
      } catch (_) {}
    }
    console.error("[FCM] Error al enviar notificación:", err.code || err.message);
    return false;
  }
}

export async function notifyUser(
  userId: string,
  title: string,
  body: string,
  data?: Record<string, string>
): Promise<number> {
  try {
    const devices = await prisma.device.findMany({
      where: { userId, active: true },
    });

    if (devices.length === 0) return 0;

    let sent = 0;
    for (const device of devices) {
      const ok = await sendPushNotification(device.token, title, body, data);
      if (ok) sent++;
    }
    return sent;
  } catch (err) {
    console.error("[FCM] Error al notificar usuario:", err);
    return 0;
  }
}
