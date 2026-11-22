# Dendra: un solo servicio con la web (landing, /r/:id, /panel) y la API de recompensas.
# Contexto de compilación: la carpeta recompensas/.

# 1) Compilar la web (Vite).
FROM node:22-alpine AS web
WORKDIR /app/web
COPY web/package.json web/package-lock.json ./
RUN npm ci
COPY web/ ./
RUN npm run build

# 2) Servidor: Node 22 ejecuta los .ts directamente, sin compilar.
FROM node:22-alpine
ENV NODE_ENV=production \
    DIRECTORIO_DATOS=/datos \
    DIRECTORIO_WEB=/app/web/dist
WORKDIR /app/server
COPY server/package.json server/package-lock.json ./
RUN npm ci --omit=dev
COPY server/src ./src
COPY --from=web /app/web/dist /app/web/dist
# /datos debe ser un volumen persistente: guarda recompensas, cuentas de rescate y claves de testnet.
VOLUME ["/datos"]
CMD ["node", "src/index.ts"]
