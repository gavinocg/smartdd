import { prisma } from "./prisma";

let playInitialized = false;

async function getPlayClient() {
  if (playInitialized) return true;

  try {
    const { google } = await import("googleapis");

    if (process.env.GOOGLE_APPLICATION_CREDENTIALS || process.env.FIREBASE_SERVICE_ACCOUNT) {
      const auth = new google.auth.GoogleAuth({
        credentials: process.env.FIREBASE_SERVICE_ACCOUNT
          ? JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)
          : undefined,
        scopes: ["https://www.googleapis.com/auth/androidpublisher"],
      });
      google.androidpublisher({ version: "v3", auth });
      playInitialized = true;
      console.log("[Play] Google Play Developer API inicializada");
      return true;
    }

    console.log("[Play] No hay credenciales de Google Play. Verificación desactivada.");
    return false;
  } catch (err) {
    console.error("[Play] Error al inicializar Google Play API:", err);
    return false;
  }
}

export async function verifyPurchase(
  purchaseToken: string,
  productId: string
): Promise<{ verified: boolean; message: string }> {
  const ready = await getPlayClient();
  if (!ready) {
    // En desarrollo, aceptar cualquier compra
    console.log("[Play] ⚠️  Modo desarrollo: compra aceptada sin verificar");
    return { verified: true, message: "Verificación omitida (desarrollo)" };
  }

  try {
    const { google } = await import("googleapis");
    const auth = new google.auth.GoogleAuth({
      scopes: ["https://www.googleapis.com/auth/androidpublisher"],
    });

    const androidPublisher = google.androidpublisher({ version: "v3", auth });

    // Verificar suscripción
    const packageName = "com.smartdd.app";
    const res = await androidPublisher.purchases.subscriptions.get({
      packageName,
      subscriptionId: productId,
      token: purchaseToken,
    });

    const data = res.data;
    if (data.paymentState === 1 && data.expiryTimeMillis) {
      const expiresAt = new Date(parseInt(data.expiryTimeMillis));
      if (expiresAt > new Date()) {
        return { verified: true, message: `Suscripción válida hasta ${expiresAt.toISOString()}` };
      }
      return { verified: false, message: "Suscripción expirada" };
    }
    return { verified: false, message: "Pago no confirmado" };
  } catch (err: any) {
    console.error("[Play] Error al verificar compra:", err.message);
    return { verified: false, message: `Error de verificación: ${err.message}` };
  }
}
