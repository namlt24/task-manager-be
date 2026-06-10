# Deploy — Task Manager

Full-stack deployment with Docker Compose. The `docker-compose.yml` in this repo builds the
backend (this repo) and the frontend (the sibling `task-manager-fe` repo) and runs PostgreSQL,
Redis and Kafka. Flyway migrates the database automatically on startup.

## Requirements
- A Linux server with Docker + Docker Compose plugin (`curl -fsSL https://get.docker.com | sh`).
- ~4 GB RAM (Kafka + Postgres + JVM + the in-server build). Port 80 open (`ufw allow 80`).

## Steps
```bash
# 1) Clone BOTH repos side by side
mkdir ~/task-manager && cd ~/task-manager
git clone https://github.com/namlt24/task-manager-be.git
git clone https://github.com/namlt24/task-manager-fe.git

# 2) Configure secrets
cd task-manager-be
cp .env.example .env
nano .env            # set DB_PASSWORD, JWT_SECRET (openssl rand -base64 48), PUBLIC_URL, MAIL_*

# 3) Build + run (first build ~3-5 min)
docker compose up -d --build
docker compose logs -f backend     # wait for "Started TaskManagerApplication"
```
Open **http://<server-ip>** (the value of `PUBLIC_URL`).

## Update to the latest code
```bash
cd ~/task-manager/task-manager-be && git pull
cd ~/task-manager/task-manager-fe && git pull
cd ~/task-manager/task-manager-be && docker compose up -d --build
```

## Notes
- The frontend (nginx, port 80) proxies `/api` and `/api/ws` (WebSocket/realtime) to the backend —
  Postgres/Redis/Kafka are internal only, not exposed to the host.
- `PUBLIC_URL` must match what users type in the browser (drives CORS + email invite links).
- Use a strong, fresh `JWT_SECRET` — do not reuse a dev/shared one.
- **Domain + HTTPS**: put a reverse proxy (Caddy/Traefik) in front of `frontend`, set
  `PUBLIC_URL=https://your-domain` (realtime then uses `wss://` automatically).
- **Low-RAM server (2 GB)**: building Angular/Maven in-server may OOM — build images on a bigger
  machine / CI and push to a registry, then `docker compose pull` instead of `--build`.
