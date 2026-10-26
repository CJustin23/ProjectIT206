# =============================================================================
# DEVELOPMENT image — chạy Vite dev server, hot reload qua volume mount
# (Sprint 4 sẽ thay bằng image production: vite build -> nginx tĩnh)
# =============================================================================
FROM node:22-alpine
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install
COPY . .
EXPOSE 5173
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
