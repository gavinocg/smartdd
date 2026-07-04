FROM node:22-bullseye-slim AS builder

WORKDIR /app

# Instalar dependencias
COPY package.json tsconfig.json ./
RUN npm install

# Copiar código fuente
COPY prisma ./prisma
COPY src ./src

# Generar Prisma Client y compilar TypeScript
RUN npx prisma generate
RUN npm run build

# ─── Producción ─────────────────────────────────────
FROM node:22-bullseye-slim AS production

WORKDIR /app

# Variables de entorno
ENV NODE_ENV=production
ENV PORT=3000

# Copiar archivos necesarios
COPY package.json ./
RUN npm install --only=production

COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules/.prisma ./node_modules/.prisma
COPY prisma ./prisma

# Usuario no root
USER node

EXPOSE 3000

CMD ["node", "dist/index.js"]
