# -- backend Dockerfile
# Stage 1: Build React app
FROM node:20-slim AS builder
WORKDIR /app

# Copy package files first for better caching
COPY package*.json ./
RUN npm ci --no-optional --silent

# Copy source code
COPY . .

# Build the app
RUN npm run build

# Stage 2: Serve with NGINX
FROM nginx:alpine

# Copy built app
COPY --from=builder /app/dist /usr/share/nginx/html

# Security headers and permissions
RUN chown -R nginx:nginx /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]