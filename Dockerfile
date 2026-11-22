# Dendra: un solo servicio que sirve el frontend (landing, /r/:id, /panel) y la API del backend.
# Contexto de compilación: la raíz del repositorio. La app móvil no entra en la imagen.

# 1) Compilar el frontend (Vite).
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2) Backend: Node 22 ejecuta los .ts directamente, sin compilar.
FROM node:22-alpine
ENV NODE_ENV=production \
    DIRECTORIO_DATOS=/datos \
    DIRECTORIO_WEB=/app/frontend/dist
WORKDIR /app/backend
COPY backend/package.json backend/package-lock.json ./
RUN npm ci --omit=dev
COPY backend/src ./src
COPY --from=frontend /app/frontend/dist /app/frontend/dist
# /datos debe ser un volumen persistente: recompensas, cuentas del panel y claves de testnet.
VOLUME ["/datos"]
CMD ["node", "src/index.ts"]
