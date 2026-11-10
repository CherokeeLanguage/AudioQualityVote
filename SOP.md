# SOP — AudioQualityVote project

🤖 Co-authored with AI: OpenCode (huggingface/zai-org/GLM-5.3-Flash)

## Build/run mandate (from the developer, mandatory)

- **The project root `./gradlew` wrapper is the ONLY gradle to use.** Never invoke a
  system gradle (`gradle`, `/usr/bin/gradle`) — the wrapper pins the project's build
  toolchain; a system gradle has produced real overlay-resolution failures here. This
  applies to every surface, including the deploy script.
- **Default local run mode: `./gradlew :audio-quality-vote-war:appStart`** — the server
  runs detached and stays up; halt it with `./gradlew :audio-quality-vote-war:appStop`.
- `appRun` (foreground) is a named variant only, never the default; when it is used, it
  holds the terminal and needs stdin kept open to stop cleanly — that friction is why
  `appStart` is the default.

## Local verification stack (as used for #2/#12 live tests)

1. MariaDB 10.11 on `127.0.0.1:3306`:
   `docker run -d --name aqv-mysql -e MYSQL_ROOT_PASSWORD=root_pw -e MYSQL_DATABASE=audiovote -e MYSQL_USER=audiovote -e MYSQL_PASSWORD=audiovote_pw -p 3306:3306 mariadb:10.11`
2. Configuration is environment-only (AudioQualityVote#25; no properties file
   exists any longer). Copy `.env.example` to `.env` (git-ignored), fill in the
   operator values (the full key set: `DB_*` plus `AUDIO_FOLDER`,
   `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET_FILE`/`GITHUB_CLIENT_SECRET`), and
   load it into the invoking shell:
   `set -a; . ./.env; set +a`.
3. **Gradle-daemon environment caveat (mandatory):** the Gradle daemon captures
   its environment at start — env values changed after the daemon started do
   **not** reach the forked webapp JVM. After changing any env value, either
   restart the daemon (`./gradlew --stop`) or use the daemon-free invocation
   form: `./gradlew --no-daemon :audio-quality-vote-war:appStart`.
4. `./gradlew :audio-quality-vote-war:appStart`, context at
   `http://localhost:8080/AudioQualityVote` (Tomcat 9 via gretty).
5. `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` when the default JVM is newer than
   Gradle's supported range (`Unsupported class file major version 69` = Java 25 under
   old Gradle).

## Browser verification (playwright CLI)

Browser probes for live verification run through the vendored CLI:

- **PATH-prefix invocation:** `PATH=<repo-root>/.tools/node/bin:$PATH playwright-cli …`
  — a bare invocation resolves the ambient system Node and fails at Playwright's
  version gate.
- **Browser choice:** open with `--browser chromium` — the `chrome` channel binary is
  absent here; chromium builds are cached at `~/.cache/ms-playwright/`.
- **Persistent profile:** open with `--persistent` — the daemon's profile home
  `~/.cache/ms-playwright/daemon/ud-bl-persist` carries stored logins across browser
  restarts. A custom `--profile <dir>` forks an orphan profile and loses them; headed
  (`--headed`) is required whenever a login hop is part of the verification. Named
  sessions (`-s=<name>`) run alongside any window already in use.
- **Login hop:** a completed GitHub login is a human identity action — the developer
  completes it in the visible browser window. App sessions are DB-backed session rows,
  so a server restart between probe phases does not log out the browser.
- **appStart invocation:** background it (for example
  `( ./gradlew :audio-quality-vote-war:appStart > appStart.log 2>&1 & )`) and poll the
  context URL instead of holding the calling shell.

## Versioning regime (from the developer, mandatory)

The project is evergreen: version identity advances with every merged PR — no release
train, tags, or GitHub Releases. Every PR carries exactly one SemVer bump of the
application version in its own diff; the version on trunk after a merge equals the
pre-merge trunk version incremented once:

- **MAJOR+1** (minor and patch reset) — incompatible changes.
- **MINOR+1** (patch reset) — backward-compatible new functionality.
- **PATCH+1** — backward-compatible defect fixes.

Levels follow SemVer's own definitions (https://semver.org/), applied by the PR author.
The version subject is the deployed application as a whole; backward-compatibility is
judged against its user-visible behavior, REST API, and stored data. The version's
source of truth is the single `version` declaration in the root `build.gradle`
(the `allprojects` block — explicit wiring; Gradle propagates no root version without
it), with every project in the build resolving it. The first PR under this regime set
the initial version (0.1.0, AudioQualityVote#15) instead of bumping.

## Deployment

`audio-quality-vote-war/deploy-dict.sh` — the repo's single deploy script — may be
invoked from anywhere (it pins its own directory). It deploys the containerized
application to the serving stack's Docker host, `vhost-58.cherokeedictionary.net`:
it builds with the project-root wrapper, ships the built product into the stack's
build context (`/srv/stack/audio-quality-vote/build`), and rebuilds/restarts the
stack's `audio-quality-vote` compose service. The container image packages the
gretty application-export product (AudioQualityVote#26's deliverable); the compose
service itself is devops#13's deliverable. No properties file ships and no
configuration values ride the script — the stack's compose service delivers
the environment keys (`DB_*` plus the application keys `AUDIO_FOLDER`,
`GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET_FILE`) and secret files by path
(key names and paths only). The legacy flow (properties file + war scp to
`vhost.cherokeedictionary.net`'s Tomcat) is superseded — see also the
deprecation notice in `deploy-dict.sh`'s header (AudioQualityVote#25).

Every deploy step here is gated by the standing pre-flight/post-flight
container-check gate (the section below — cherokeelanguage-devops#56).

### Host roles (2026-11-09, operator directive — AudioQualityVote#30)

`vhost-58.cherokeedictionary.net` (104.251.216.58, the delivered Docker
serving stack) is the **prod host** — `deploy-dict.sh`'s target above is
the production deployment. `vhost-175.cherokeedictionary.net`
(63.142.255.175, = ced1) is the **dev host** — dev-stack work lands
there, not on the prod host. Org-side fact record: cherokeelanguage-devops
`docs/infrastructure/host-roles-vhost-58-vhost-175.md`.

## Standing pre-flight/post-flight container-check gate (2026-11-10 — sibling of cherokeelanguage-devops#56)

The rule of record for every deploy and container-work step on the vhost-58
serving stack is the standing pre-flight/post-flight container check — defined
once, org-side, in cherokeelanguage-devops'
[container-sop-checks.md](https://github.com/CherokeeLanguage/cherokeelanguage-devops/blob/main/docs/runbooks/container-sop-checks.md)
(spec cherokeelanguage-devops#56, the response to the devops#54 empty-200
outage). This SoP states **when** it runs in this repo's deploy flow; the
check's definitions and instrument (`container-sop-checks.sh`) live only in
that master page — linked, never copied.

### When the check runs in this repo's deploy flow

The gate is two-phase, and its exit code is the gate:

- **Pre-flight:** `bash container-sop-checks.sh` must exit **0** before any
  deploy step starts — before any `deploy-dict.sh` invocation and before the
  first step of the aqv switchover sequence below. A non-zero pre-flight
  result means: stop, do not deploy.
- **Post-flight:** the same check is re-run after the deploy step completes
  and must exit **0** — a non-zero post-flight result means the work is not
  done.
- **Capture-with-work:** both captured runs — pre-flight and post-flight —
  are recorded with the work (on the deploy's issue) alongside the other
  verification captures.

### This repo's container/route inventory on the shared stack

- **`aqv`** — the AudioQualityVote vote application
  (`aqv:<version>`, service `aqv`, container `aqv`), serving behind the
  **`/AudioQualityVote`** route on `cherokeedictionary.net` (the Caddy edge on
  `vhost-58`) — the route the master page's body-level sweep includes.
- **Networks:** `aqv` also sits on the **`backend`** network — it is the
  role-check client the master page's backend check probes for MariaDB
  reachability (`mariadb:3306` TCP connect from a backend-attached app
  container).

## aqv container switchover (zero-connection-loss image replacement behind Caddy)

This is this repository's instance of the org runbook for zero-connection-loss
container switchover on the vhost-58 serving stack — cherokeelanguage-devops#42
(the blue-green SoP of record for every container on the stack); the aqv
procedure below conforms to it. The problem it retires: every aqv replacement
path recreates the container in place — compose stops the old container before
its replacement is healthy, so the `/AudioQualityVote` route 502s for the
recreate+boot window. The rule: **at no point is there a moment without a
healthy upstream behind Caddy.**

### The two recorded compose arrangements (reconciliation)

The repo records two compose arrangements for this application; the
switchover sequence operates on the serving one:

- **Serving arrangement — the devops-staged tree `/srv/audioqualityvote`**
  (service `aqv`, `container_name: aqv`, image `aqv:<version>`). This is the
  arrangement that serves traffic: the phase-4 post-state capture of
  2026-11-03 (cherokeelanguage-devops#13's `phase4-poststate-final-mconrad.txt`
  record — `docker ps` showing `aqv aqv:1.1.0
  Up`, the staged compose.yaml with service `aqv`, and the Caddyfile grep
  showing `reverse_proxy aqv:8080`) and the probe instrument
  (`audioqualityvote-vhost-58-probe.sh`, which inspects `docker`'s `aqv`
  container and the `/srv/audioqualityvote` tree) both record it as the
  deployed state, and `aqv` is the container name the Caddyfile dials.
  The canonical-recreate step of the sequence below maps onto this
  arrangement.
- **The repo deploy-script tree** — `/srv/stack/audio-quality-vote` (service
  `audio-quality-vote`, per `audio-quality-vote-war/deploy-dict.sh` and the
  Deployment section above). Its standing: **superseded as the serving
  arrangement** — it is not the compose project Caddy's upstream resolves
  through. The script's war-ship step remains the path that delivers the
  built product into the aqv image's build context (the image packages the
  gretty application-export product, AudioQualityVote#26); image changes for
  the serving arrangement ride the `aqv:<version>` pinning convention the
  serving arrangement's stage script records
  (`audioqualityvote-vhost-58-stage.sh`).

### The switchover sequence (operator-executed, on `vhost-58`)

For an image change to version `<new>` (prior image `aqv:<old>`):

1. **Build/ship the new image.** Produce the product export through the
   project wrapper (`./gradlew` only — the build/run mandate above), ship it
   into the aqv image build context, and build/tag it `aqv:<new>` on the
   host (per `audioqualityvote-vhost-58-stage.sh`'s image-build step). The
   `aqv:<old>` image stays on the host — retained until the full gate
   passes (rollback rule below).
2. **Boot `aqv-next` alongside.** Run the new image as container `aqv-next`
   from a compose file derived from the serving arrangement's
   `/srv/audioqualityvote/compose.yaml` with: `container_name: aqv-next`,
   `image: aqv:<new>`, and no change to anything else — the same env contract
   (the operator-held `.env` values `GITHUB_CLIENT_ID` and `DB_USER`
   interpolated the same way, plus the fixed values `DB_HOST=mariadb`,
   `DB_PORT=3306`, `DB_NAME=audio_quality_vote`, `AUDIO_FOLDER=/srv/ingest`),
   the same secrets delivered by path (`DB_PASSWORD_FILE=/run/secrets/db_password`,
   `GITHUB_CLIENT_SECRET_FILE=/run/secrets/github_client_secret` — secret
   values are never in any repo artifact; key names and paths only), the same
   external networks `proxy` and `backend`, the same
   `mem_limit`/`memswap_limit` 512m, the same read-only ingest mount
   `/srv/audioqualityvote/ingest:/srv/ingest:ro`, and no host ports (zero
   port conflict by design). `docker compose up -d aqv-next`.
3. **Health-gate `aqv-next`.** Poll until an HTTP check of the
   `/AudioQualityVote` context path from inside the container network
   (e.g. `docker exec aqv-next curl -fsS -o /dev/null -w '%{http_code}'
   http://localhost:8080/AudioQualityVote/`) returns a 200-class code —
   several consecutive successes, not one.
4. **Verify `aqv-next` serves directly** through the `proxy` network before
   touching Caddy (a request against `http://aqv-next:8080/AudioQualityVote/`
   from another container on that network, e.g. caddy's namespace via
   `docker compose -f /srv/caddy/compose.yaml exec caddy curl -fsS -o
   /dev/null -w '%{http_code}' http://aqv-next:8080/AudioQualityVote/`).
5. **Atomic Caddy flip.** In `/srv/caddy/caddy/Caddyfile`, the aqv handle
   block is one named matcher carrying both path terms:

       @aqv path /AudioQualityVote /AudioQualityVote/*
       handle @aqv {
           reverse_proxy aqv:8080
       }

   Change **only** the upstream token — `reverse_proxy aqv:8080` →
   `reverse_proxy aqv-next:8080` — nothing else in the block. Then validate
   and reload:

       docker compose -f /srv/caddy/compose.yaml exec caddy caddy validate --config /etc/caddy/Caddyfile   # must exit 0
       docker compose -f /srv/caddy/compose.yaml exec caddy caddy reload --config /etc/caddy/Caddyfile      # graceful reload — in-flight requests preserved

   If `caddy validate` exits non-zero, fix the Caddyfile or revert the edit
   and re-validate — never reload an invalid config.
6. **Canonical recreate.** Traffic now serves from `aqv-next`; recreate the
   canonical `aqv` container with the new image (update the serving
   arrangement's `/srv/audioqualityvote/compose.yaml` `image:` to
   `aqv:<new>`, then `docker compose -f /srv/audioqualityvote/compose.yaml
   up -d aqv` — the recreate is invisible to users, who are on `aqv-next`).
7. **Flip back.** Verify the recreated `aqv` with steps 3–4's checks against
   `aqv`, then reverse step 5's token (`aqv-next:8080` → `aqv:8080`), again
   `caddy validate` exit 0 then graceful reload.
8. **Drain.** After the flip back, stop and remove `aqv-next`
   (`docker compose -f <next-compose-file> down`). The `aqv:<new>` image is
   now the serving image.
9. **Full gate.** The `/AudioQualityVote` route's checks through the edge
   (a request to `https://<host>/AudioQualityVote/` returning 200-class
   responses, plus the probe instrument's aqv sections
   `audioqualityvote-vhost-58-probe.sh`).

### Continuity bar and rollback rule

- **Continuity bar:** a request loop through the edge across the whole
  flip window (steps 5–7) reports **zero failed requests**. This is the
  acceptance gate for the procedure — run it (a tight loop of edge requests
  to `/AudioQualityVote` across the flip, counting non-200 responses), and
  record the capture on the deploy's issue.
- **Rollback rule:** the prior image tag `aqv:<old>` is retained on the host
  until the full gate passes. A failed gate flips traffic back — reverse the
  step-5/step-7 token edit (validate + graceful reload) so Caddy serves the
  still-healthy container, and never perform manual server surgery
  (no ad-hoc container surgery, no firewall/DNS changes) as a substitute.

## Logging (2026-11-10 — cherokeelanguage-devops#48 delivery)

- **Container log mirror:** `/var/log/containers/aqv/current.log` on
  `vhost-58` (the prod host per Host roles above). The `vector` stream
  shipper (cherokeelanguage-devops#48) captures the `aqv` container's
  stdout/stderr verbatim, one event per line; the mirror persists across
  container recreation and daily rotation (30-day history, dated copies).
- **Contract:** console-first — stdout/stderr stream capture requires no
  per-repo configuration; a file-based log instrument this app enables in
  the future must mount under the `/var/log/containers/` tree to inherit
  the drop-in's rotation.
- **Runbook of record:** cherokeelanguage-devops
  `docs/runbooks/logging-vhost-58.md` (cherokeelanguage-devops#48) —
  deploy, probe, and rollback for the logging stack live there.

## Failure policy (from the developer, mandatory)

Hiding or papering over errors is a serious policy violation. Failure branches surface as
real HTTP errors (see `RestApiImpl.login` — 400/401/403/500/502 by branch) and emit `AQV
`-prefixed `System.err` diagnostics; login never masks a failure as a null/silent 204.