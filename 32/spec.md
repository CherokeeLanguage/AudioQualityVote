# Spec — aqv switchover SoP: zero-connection-loss image replacement behind Caddy

> Ticket AudioQualityVote#32. The org-level SoP is cherokeelanguage-devops#42 (sibling ticket). This ticket puts the aqv container's own switchover procedure in this repository's SOP.md, conforming to the org runbook.

## Problem

The aqv container's replacement paths recreate it in place. The repo's own deploy script — `audio-quality-vote-war/deploy-dict.sh`, SOP.md's "single deploy script" — builds the WAR with the project wrapper, ships it into `/srv/stack/audio-quality-vote/build`, and runs `docker compose build audio-quality-vote && docker compose up -d audio-quality-vote` against the `/srv/stack/audio-quality-vote` compose project. The devops stage script (`audioqualityvote-vhost-58-stage.sh`) separately stages `/srv/audioqualityvote/compose.yaml` (service `aqv`, `container_name: aqv` — the name the Caddyfile dials). Under either arrangement a compose-level recreate stops the old container before the new one is healthy, so the `/AudioQualityVote` route 502s for the recreate+boot window. The org SoP (devops#42) makes zero-connection-loss switchover the standard for every container on the stack, with the blue-green sequence as the app-container class's procedure.

Developer directive (2026-11-09, to all container repos): the switchover-without-connection-issues procedure becomes SoP here too — "this needs to be a rule for *ALL* containers", with the goal shape stated as the executed blue-green sequence ("At no point is there a moment without a healthy upstream behind Caddy").

## Decision

**SOP.md gains the aqv switchover section** — the blue-green sequence with the aqv container's specifics, conforming to the org runbook (devops#42).

## Developer statements (traceability base)

1. "setting up the switch over without causing connection issues needs to become SoP in this repo and all the container repos. file specs as needed across all repos." (2026-11-09)
2. "this needs to be a rule for *ALL* containers" (2026-11-09, in-session clarification)
3. The blue-green goal shape stated by the developer (2026-11-09): boot alongside → health gate → atomic flip + graceful reload → canonical recreate → flip back → drain → full gate.

## Scope

- `SOP.md` — one new section (the switchover procedure for this container).
No script change in this ticket; the stage-script seam is R-3's recorded mapping plus the open item.

## Requirements

- **R-1** `SOP.md` SHALL carry the aqv switchover section — the blue-green sequence with this container's specifics: `aqv-next` alongside the serving container (same env contract — the operator-held `.env` values and the same secrets delivered by path: `DB_PASSWORD_FILE`, `GITHUB_CLIENT_SECRET_FILE`; secret values never in any repo artifact; same `proxy`+`backend` networks; same `mem_limit`/`memswap_limit` 512m; same read-only ingest mount `/srv/audioqualityvote/ingest:/srv/ingest:ro`; no host ports), the health gate against the next container (an HTTP check on the `/AudioQualityVote` context path), the direct-serve verify, the atomic Caddy flip of the `@aqv` handle block's `reverse_proxy aqv:8080` token to `aqv-next:8080` in `/srv/caddy/caddy/Caddyfile` (the block's matcher is one named matcher carrying both path terms — `@aqv path /AudioQualityVote /AudioQualityVote/*`; only the upstream token changes) with `caddy validate` exit 0 then the graceful reload preserving in-flight requests, the canonical `aqv` recreation with the new image, the flip back, the drain, and the full gate (the `/AudioQualityVote` route's checks).
- **R-2** The section SHALL state the continuity bar and the rollback rule: a request loop through the edge across the flip window shows zero failed requests; the prior image tag is retained until the full gate passes; a failed gate flips traffic back — never manual server surgery.
- **R-3** The section SHALL record the deployment-surface reconciliation — the repo records two compose arrangements for this application and the section may not leave them unreconciled: (a) the repo's deploy script's tree (`/srv/stack/audio-quality-vote`, service `audio-quality-vote`, per `deploy-dict.sh` and SOP.md's Deployment section) and (b) the devops-staged tree (`/srv/audioqualityvote`, service `aqv` with `container_name: aqv` — the arrangement the phase-4 post-state capture of 2026-11-03 and the devops probe instrument record, and the one whose container name the Caddyfile dials). The section SHALL state which arrangement is the serving one — anchored to the recorded evidence (the phase-4 post-state capture; the probe instrument's target), never asserted from memory — SHALL map the sequence's canonical-recreate step onto that arrangement, and SHALL state the other path's standing (superseded, or the alternate recreate path the sequence covers) explicitly. An image change rides the `aqv:<version>` pinning convention the serving arrangement records.

## Success criteria

| ID | Criterion | Class | Instrument |
|---|---|---|---|
| SC-1 | `SOP.md` carries the switchover section with every R-1 element | structural | file parse against R-1 |
| SC-2 | The section states the continuity bar, the rollback rule, and the secrets-by-path rule (no secret values anywhere in the section) (R-2, R-1's secrets terms) | structural | file parse + content comparison of the section against the operator-held values at their recorded paths (`/srv/audioqualityvote/secrets/*`; the `.env` key names the stage script declares) — the section contains none of the held values |
| SC-3 | The first aqv deploy executed under the SoP carries the continuity capture — a request loop through the edge across the flip window reporting zero failed requests, recorded on this issue (until that deploy occurs, this criterion is pending work) | behavioral | executed continuity loop + capture on the issue |

## Out of scope

- The org runbook's content (devops#42's deliverable — this section conforms to it and names it).
- Making the devops stage script (or any aqv deploy tooling) execute the blue-green sequence natively — open item for this ticket's plan.
- The local build/run procedures SOP.md already records (unchanged — this ticket adds a section; the Deployment section's `deploy-dict.sh` record stays, reconciled per R-3).
- The other containers' procedures (their own repos or the org runbook).

## Open items

- Whether aqv deploy tooling gains a first-class blue-green mode (the R-3 reconciliation resolves which arrangement is canonical — the decision spans both repos) — decided at this ticket's plan stage.

## Done state

SC-1…SC-3 all green; the continuity capture from the first SoP-executed aqv deploy recorded on the issue (until that deploy, SC-3 is pending work).

## Consumers / blast radius

aqv operators and future deploys (the recreate gap on the `/AudioQualityVote` route is retired); the org runbook consumes this section as the aqv instance of the pattern. Blast radius: one documentation file in this repo; the evidence event is the flip window of an executed aqv deploy — no separate rehearsal surface exists or is created by this ticket.
