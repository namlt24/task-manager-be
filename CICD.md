# CI/CD — auto-deploy to a laptop "server"

Push to `master` → GitHub builds/tests on a cloud runner → if green, a **self-hosted runner on the
laptop** pulls both repos and runs `docker compose up -d --build`. Works behind home NAT (the runner
dials out to GitHub; no inbound ports needed).

```
push task-manager-be  ─┐
                       ├─► [GitHub Actions: build & test] ─► [self-hosted runner on laptop] ─► docker compose up -d --build
push task-manager-fe  ─┘   (fe build → repository_dispatch → be deploy workflow)
```

Workflows: `task-manager-be/.github/workflows/deploy.yml` (CI + deploy) and
`task-manager-fe/.github/workflows/ci-deploy.yml` (CI + dispatch).

---

## One-time setup on the laptop (the "server")

1. **Install Docker** and make it start on boot:
   - Windows/Mac: Docker Desktop → Settings → General → *Start Docker Desktop when you log in*
     (and set the laptop to log in / stay on).
   - Linux: `curl -fsSL https://get.docker.com | sh` then `sudo systemctl enable --now docker`.

2. **Clone both repos + first run** (see `DEPLOY.md`):
   ```bash
   mkdir ~/task-manager && cd ~/task-manager
   git clone https://github.com/namlt24/task-manager-be.git
   git clone https://github.com/namlt24/task-manager-fe.git
   cd task-manager-be && cp .env.example .env   # edit secrets
   docker compose up -d --build
   ```
   > The deploy job expects this exact layout under `~/task-manager`. If you use another path,
   > set a repo **Variable** `DEPLOY_DIR` (Settings → Secrets and variables → Actions → Variables).

3. **Install the GitHub self-hosted runner** for **task-manager-be**:
   GitHub → repo **task-manager-be** → Settings → Actions → Runners → **New self-hosted runner**.
   Follow the shown download + `./config.sh` (Linux/Mac) or `./config.cmd` (Windows) commands
   (they include a one-time token). Run it as the **same user that owns `~/task-manager`**, and
   install it **as a service** so it survives reboots:
   - Linux/Mac: `sudo ./svc.sh install && sudo ./svc.sh start`
   - Windows: during `config.cmd` choose “Run as a service”, or `.\svc.cmd install && .\svc.cmd start`.
   The runner user must be able to run Docker (Linux: add to the `docker` group; Win/Mac: Docker
   Desktop running for that user).

## One-time setup on GitHub

4. **Create a token so the frontend repo can trigger the backend deploy:**
   GitHub → Settings → Developer settings → **Fine-grained personal access token** → Repository
   access = **task-manager-be**, Permissions = **Contents: Read and write**. Copy the token.
5. Add it as a secret in **task-manager-fe**: Settings → Secrets and variables → Actions →
   **New repository secret** → name `DEPLOY_PAT`, value = the token.

## Test it
- Push any commit to `master` of either repo → repo **Actions** tab shows the run →
  build (cloud) then **Deploy to laptop** (self-hosted) → the laptop rebuilds and restarts the stack.
- Check on the laptop: `docker compose ps` and `docker compose logs -f backend`.

## Notes / trade-offs
- The laptop must be **on, logged in, with Docker running** for deploys to land (queued otherwise).
- Builds happen **on the laptop** (Angular + Maven, ~3–5 min, CPU/RAM heavy) — fine for a dev laptop.
- `.env` on the laptop is never touched by deploys (`git reset --hard` ignores untracked files).
- To pause auto-deploy, stop the runner service; to deploy manually, use **Run workflow** (workflow_dispatch).
