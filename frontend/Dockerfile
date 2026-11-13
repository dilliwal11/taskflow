# Multi-stage build: compile the Angular app with Node, serve the static files with nginx.

FROM node:22-alpine AS build
WORKDIR /app
# Copy the manifests first so "npm ci" is cached until dependencies change.
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
