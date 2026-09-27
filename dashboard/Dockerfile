# © 2026 Équipe projet n°3 — ENSA. Tous droits réservés. Voir le fichier LICENSE.
# --- Étape 1 : build (Internet nécessaire au moment du build uniquement) ---
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

# --- Étape 2 : service statique nginx (non root) sur le port 8501 ---
FROM nginxinc/nginx-unprivileged:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8501
