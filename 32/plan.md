# Plan — AudioQualityVote#32: aqv switchover SoP section in SOP.md

> Derived from `.issues/32/spec.md` (validated round-2 PASS, 2026-11-09). Documentation-only change: one file (`SOP.md`), one new section.

## Plan-stage decisions

- **Open item (blue-green tooling mode): NO in this ticket.** The switchover sequence is recorded as an operator-executed procedure in SOP.md; no aqv deploy tooling changes. A first-class blue-green mode in deploy tooling is a future cross-repo ticket (spans this repo and cherokeelanguage-devops), filed separately if wanted. This ticket's scope statement already says "No script change in this ticket."
- **R-3 serving arrangement, anchored:** the devops-staged tree `/srv/audioqualityvote` (service `aqv`, `container_name: aqv`, image `aqv:<version>`) is the serving arrangement. Evidence: devops#13's phase-4 post-state capture of 2026-11-03 (`phase4-poststate-final-mconrad.txt` — `docker ps` shows `aqv aqv:1.1.0 Up`, compose.yaml shows service `aqv`/`container_name: aqv`, Caddyfile grep shows `@aqv path /AudioQualityVote /AudioQualityVote/*` → `reverse_proxy aqv:8080`) and the probe instrument `audioqualityvote-vhost-58-probe.sh` (targets `docker inspect aqv`, `/srv/audioqualityvote` tree). The repo deploy-script tree (`/srv/stack/audio-quality-vote`, service `audio-quality-vote`, per `deploy-dict.sh`) is recorded as the alternate recreate path whose standing the section states explicitly (superseded as the serving arrangement; its war-ship flow remains the build-product path into the aqv image build context, not the serving compose project).
- **Version bump:** none — docs-only PR (precedent: #30's SOP-only PR carried no `build.gradle` bump).

## Items

### Item 1 — SC-1 (+ R-3): the switchover section with every R-1 and R-3 element

- **Deliverable:** new `SOP.md` section, "aqv container switchover (zero-connection-loss image replacement behind Caddy)", placed after the Deployment section. Content elements (all verified against the recorded sources above):
  1. Context: names the org runbook ticket cherokeelanguage-devops#42 (the aqv section conforms to it); states the gap (in-place recreate 502s `/AudioQualityVote` for the recreate+boot window).
  2. Blue-green sequence with aqv specifics: `aqv-next` alongside `aqv` — same env contract (operator-held `.env` values `GITHUB_CLIENT_ID`, `DB_USER`; same fixed env `DB_HOST=mariadb`, `DB_PORT=3306`, `DB_NAME=audio_quality_vote`, `AUDIO_FOLDER=/srv/ingest`), same secrets by path (`DB_PASSWORD_FILE=/run/secrets/db_password`, `GITHUB_CLIENT_SECRET_FILE=/run/secrets/github_client_secret` — values never in any repo artifact), same external networks `proxy` + `backend`, same `mem_limit`/`memswap_limit` 512m, same read-only ingest mount `/srv/audioqualityvote/ingest:/srv/ingest:ro`, no host ports (zero port conflict), `restart: unless-stopped`.
  3. Health gate against `aqv-next`: HTTP check on the `/AudioQualityVote` context path from inside the container network.
  4. Direct-serve verify of `aqv-next` before the flip.
  5. Atomic Caddy flip: in `/srv/caddy/caddy/Caddyfile` the `@aqv` matcher block (`@aqv path /AudioQualityVote /AudioQualityVote/*`) changes only the upstream token `reverse_proxy aqv:8080` → `aqv-next:8080`; then `docker compose -f /srv/caddy/compose.yaml exec caddy caddy validate --config /etc/caddy/Caddyfile` exit 0, then the graceful reload `... exec caddy caddy reload --config /etc/caddy/Caddyfile` (in-flight requests preserved).
  6. Canonical `aqv` recreation with the new image (mapped onto the `/srv/audioqualityvote` arrangement per R-3 — image built/pinned `aqv:<version>`, compose service `aqv` recreated while traffic serves from `aqv-next`).
  7. Flip back (`aqv-next:8080` → `aqv:8080`), verify, then drain `aqv-next` (stop/remove).
  8. Full gate: the `/AudioQualityVote` route's checks (edge request through Caddy, HTTP 200-class responses).
  9. R-3 reconciliation: both arrangements named; the serving one anchored to the two recorded evidence artifacts (phase-4 post-state capture 2026-11-03; the probe instrument's target); canonical-recreate mapped onto it; the deploy-script tree's standing stated explicitly.
- **RED:** `grep -c "switchover" SOP.md` returns 0 matches — the section does not exist.
- **GREEN:** the section is added with every element above.
- **Instrument (structural):** scripted parse of `SOP.md` checking the section exists and each R-1/R-3 element's token appears (env contract keys, secret paths, networks, limits, mount, no-host-ports, `aqv-next`, `@aqv` matcher with both path terms and single-token change, validate+reload commands, canonical recreate mapped to `/srv/audioqualityvote`, other-path standing, runbook ticket name). Output captured in the session record.

### Item 2 — SC-2: continuity bar, rollback rule, secrets-by-path

- **Deliverable:** inside the same section: (a) the continuity bar — a request loop through the edge across the flip window shows zero failed requests; (b) the rollback rule — the prior image tag is retained until the full gate passes; a failed gate flips traffic back, never manual server surgery; (c) the secrets-by-path statement — secret values never in any repo artifact; key names and paths only.
- **RED:** `grep -c "zero failed requests" SOP.md` and `grep -c "prior image tag" SOP.md` both return 0 against the post-Item-1 file — the continuity bar and rollback rule are the two statements whose absence the RED asserts, and those exact phrases are what the GREEN adds.
- **GREEN:** the statements are added as part of Item 1's section, using the phrases above.
- **Instrument (structural + content comparison):** the comparison's value sources are stated concretely, covering both sources the spec names:
  1. **The workspace-held operator values.** The git-ignored `.env` at the repo root is present in this workspace (verified 2026-11-09, 169 bytes) and is the complete set of operator-held values held in this environment; it carries the `GITHUB_CLIENT_ID` value the stage script's `.env` key set declares (plus `DB_*` values). After the edit, the instrument greps the new section for every value literal in that file and asserts zero matches. The grep runs with the values never printed — the captured output records only the per-key match count (0) and the file's presence, never the values themselves (no secrets in output).
  2. **The host-held secret values at `/srv/audioqualityvote/secrets/*`.** Those files live on the prod host and are not fetched — this session holds no instruction to read production secret material, and none is read. Their absence from the section is verified by provenance: the section is authored entirely within this session from repo artifacts (`SOP.md`, `deploy-dict.sh`, the devops stage script's recorded key names and paths) that carry no secret values; the instrument additionally asserts the section names the secrets only by key name and path (`DB_PASSWORD_FILE`, `GITHUB_CLIENT_SECRET_FILE`, `/run/secrets/db_password`, `/run/secrets/github_client_secret`, `secrets/db_password`, `secrets/github_client_secret`) by grepping for any `GITHUB_CLIENT_SECRET=` or `DB_PASSWORD=` literal-value form and asserting zero matches. The captured output records the assertion results.
  The comparison is not optional in either leg: leg 1 executes unconditionally (the file's presence is asserted, not assumed), leg 2 executes as the provenance + names-only assertions above.

### SC-3 — pending work (not an item)

SC-3 is the behavioral criterion whose evidence event is the first aqv deploy executed under the SoP; the spec's done state defines it as pending work until that deploy occurs. Nothing in this repo's change can execute it now; it is recorded as pending on the issue and is not a plan item (derivation guard: it traces to SC-3 but is not implementable in this PR).

## Dependency order

Item 1 then Item 2 — one file, one section; Item 2's content lands inside Item 1's section. No tests authored (documentation change; no SC-matching test files — instruments are scripted greps/parses run in-session, ephemeral).
