# ====================================================================
# LifeLink - Frontend Multi-Stage Dockerfile (React + Vite + Nginx)
# ====================================================================

# --------------------------------------------------------------------
# Stage 1: Build React Production Bundle
# --------------------------------------------------------------------
FROM node:20-alpine AS builder

WORKDIR /app

# Cache package dependencies
COPY package.json package-lock.json ./
RUN npm ci

# Copy frontend source code
COPY . .

# Optional build arg for API base URL (empty by default to use Nginx reverse proxy /api and /ws)
ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# Build optimized distribution files
RUN npm run build

# --------------------------------------------------------------------
# Stage 2: High-Performance Lightweight Nginx Server
# --------------------------------------------------------------------
FROM nginx:1.27-alpine AS runner

# Remove default static pages
RUN rm -rf /usr/share/nginx/html/*

# Copy built production assets
COPY --from=builder /app/dist /usr/share/nginx/html

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose HTTP port
EXPOSE 80

# Container healthcheck testing /health endpoint
HEALTHCHECK --interval=20s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q --spider http://localhost:80/health || exit 1

# Start Nginx in foreground
CMD ["nginx", "-g", "daemon off;"]
