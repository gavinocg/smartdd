import "dotenv/config";
import bcrypt from "bcryptjs";
import { prisma } from "./services/prisma";

async function seed() {
  console.log("[Seed] Iniciando seed de base de datos...");

  // Crear admin por defecto
  const adminEmail = process.env.ADMIN_EMAIL || "admin@smartdd.com";
  const adminPassword = process.env.ADMIN_PASSWORD || "admin123456";

  const existingAdmin = await prisma.user.findUnique({
    where: { email: adminEmail },
  });

  if (!existingAdmin) {
    const passwordHash = await bcrypt.hash(adminPassword, 12);

    const admin = await prisma.user.create({
      data: {
        name: "Administrador",
        email: adminEmail,
        passwordHash,
        plan: "PRO",
        role: "admin",
        config: {
          create: {
            defaultMode: "CHAT",
            chatEnabled: true,
            audioEnabled: true,
            videoEnabled: true,
          },
        },
      },
    });

    console.log(`[Seed] Admin creado: ${admin.email}`);
    console.log(`[Seed] Email: ${adminEmail}`);
    console.log(`[Seed] Password: ${adminPassword}`);
    console.log(`[Seed] **CAMBIAR LA CONTRASEÑA EN PRODUCCIÓN**`);
  } else {
    console.log(`[Seed] Admin ya existe: ${adminEmail}`);
  }

  // Crear un usuario de prueba
  const testEmail = "test@smartdd.com";
  const existingTest = await prisma.user.findUnique({
    where: { email: testEmail },
  });

  if (!existingTest) {
    const passwordHash = await bcrypt.hash("test123456", 12);

    await prisma.user.create({
      data: {
        name: "Usuario Test",
        email: testEmail,
        passwordHash,
        plan: "FREE",
        role: "user",
        config: {
          create: {
            defaultMode: "CHAT",
            chatEnabled: true,
            audioEnabled: true,
            videoEnabled: true,
          },
        },
      },
    });

    console.log(`[Seed] Usuario test creado: ${testEmail}`);
  } else {
    console.log(`[Seed] Usuario test ya existe: ${testEmail}`);
  }

  console.log("[Seed] Seed completado.");
  await prisma.$disconnect();
}

seed().catch((err) => {
  console.error("[Seed] Error:", err);
  prisma.$disconnect();
  process.exit(1);
});
