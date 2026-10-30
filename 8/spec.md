# Spec: Deploy scripts for the new hosting (vhost.cherokeedictionary.net)

Intent (developer-stated): "adjust the deploy scripts to reflect the new hosting (replaces
prior hosting)"; the deploy mechanism (developer-stated): "auto scp to a temp file in the
webapps folder then run remote rename to replace the prior war"; target (developer-provided):
`mconrad@vhost.cherokeedictionary.net`; path (developer-stated): "use the symlinked path".

## Problem

The repo's deploy scripts target retired hosting:

- `audio-quality-vote-war/deploy-clcom.sh` — scp `AudioQualityVote.war` to
  `clcom@vhost.cherokeelessons.com:/var/lib/tomcat9/webapps/AudioQualityVote.tmp`, then ssh
  `mv AudioQualityVote.tmp AudioQualityVote.war`.
- `audio-quality-vote-war/deploy-dict.sh` — same target and mechanism, deploy name
  `CED-AudioQualityVote.war`.

The new hosting replaces it, verified live at spec time (read-only ssh discovery):

- SSH target: `mconrad@vhost.cherokeedictionary.net` (key-based, connected non-interactively).
- Tomcat 9 layout: `/opt/tomcat` is a symlink to `/opt/apache-tomcat-9.0.7`; the live webapps
  directory is the symlinked path **`/opt/tomcat/webapps`** (developer-directed: use the
  symlinked path, not the versioned one).
- Contents observed: `AudioQualityVote.war` (and exploded `AudioQualityVote/`) already present,
  alongside other apps (`CherokeeAnalyzer`, `ROOT`, `audio`, `cnt`) — the deployment must touch
  only its own war names.
- No `tomcat9.service` on that host (its service layout was not probed deeper; the change does
  not touch service management).

## Approach

- Both scripts keep their existing shape — `set -e`/`pipefail` guard, `gradle clean build war`,
  then scp and a single remote rename — and move wholesale to the new target. The temp-file +
  rename sequence is preserved as-is: scp lands `AudioQualityVote.tmp` (resp.
  `CED-AudioQualityVote.tmp`) in the webapps folder, and the remote `mv` over the live war name
  is the atomic publish that triggers Tomcat's redeploy. A failed scp never renames.
- `deploy-clcom.sh` target becomes
  `mconrad@vhost.cherokeedictionary.net:/opt/tomcat/webapps/AudioQualityVote.tmp` →
  `mv AudioQualityVote.tmp AudioQualityVote.war`.
- `deploy-dict.sh` target becomes the same host/path with its deploy name
  (`CED-AudioQualityVote.war`), mechanism and guard unchanged. (Judgment recorded: the
  developer directive covers "the deploy scripts" — both — as a set; each script keeps its
  deploy name, so the rename of hosting never silently changes what a script publishes.)
- No orchestration is added: no new pipeline, CI wiring, or service restarts — the change is
  exactly "adjust the deploy scripts to reflect the new hosting".

## Requirements

- R1. The deploy scripts SHALL publish to the new hosting — scp the built war to a temp file
  (`<war-name>.tmp`) inside `/opt/tomcat/webapps` on `mconrad@vhost.cherokeedictionary.net`,
  then rename it remotely (`mv <war-name>.tmp <war-name>.war`) — the rename replacing the prior
  war and triggering redeploy. `deploy-clcom.sh` publishes `AudioQualityVote.war`;
  `deploy-dict.sh` publishes `CED-AudioQualityVote.war`.
- R2. The symlinked path `/opt/tomcat/webapps` SHALL be the target path — the versioned
  `/opt/apache-tomcat-9.0.7/webapps` must not appear.
- R3. No reference to the retired hosting SHALL remain in the deploy scripts:
  `clcom@vhost.cherokeelessons.com`, `/var/lib/tomcat9` — zero matches after the change.
- R4. Publish atomicity SHALL hold: on any scp failure nothing is renamed and no prior war is
  disturbed (`set -e`/`pipefail` guard stays); the remote `mv` remains the only publish step.
- R5. Build integrity stays upstream of upload: each script's war is produced by its own
  `gradle clean build war` before any scp; no prebuilt or foreign artifact may be shipped.

## Success criteria

| ID | Criterion | Class | Verification instrument |
|----|-----------|-------|-------------------------|
| SC-1 | The deploy scripts carry the new hosting: `mconrad@vhost.cherokeedictionary.net` and the symlinked path `/opt/tomcat/webapps` in both scripts, each with its own `<war>.tmp` + remote `mv` sequence and preserved `set -e`/`pipefail` guard and `gradle clean build war` step | Structural | Read of both scripts post-change: scp line names the new host/path/temp-file, next line is the ssh `mv` to the war name; grep for `vhost.cherokeelessons.com` and `/var/lib/tomcat9` across `audio-quality-vote-war/deploy-*.sh` → 0 matches; grep for `/opt/apache-tomcat` → 0 matches |
| SC-2 | A deploy run publishes the current war to the new hosting with a successful rename | Behavioral | Prerequisite: developer's explicit go for a production deploy (this runs against the live host). Run `deploy-clcom.sh`; script exits 0; then read via ssh: `/opt/tomcat/webapps/AudioQualityVote.war` mtime updated (replaced), no `AudioQualityVote.tmp` left behind, and the other apps' wars in webapps untouched (name-listed before/after compare) |
| SC-3 | The deliverable script's upload failure publishes nothing (no rename, prior war undisturbed) | Behavioral | Run the `deliverable` — the actual changed `deploy-clcom.sh` itself — against an unreachable endpoint by pointing its host resolution at a non-existent host via ssh config (a `Host` alias overriding the target; the script's committed content is not edited for this run): observe scp fail → the script exits non-zero, the guard prevents the `mv` step from being issued, and no war rename occurs on any side. This validates the guard logic of the shipped script without touching the live host; the live-path atomicity is additionally covered by SC-2's before/after states |

## Out of scope

- Any change to service management on the new host (start/stop/restart of Tomcat), monitoring,
  or reverse-proxy configuration.
- The production OAuth app's callback configuration (issue #2's operator surface).
- CI automation of deploys.

## Impact

- Two shell scripts change target host and path; behavior, guard, deploy names, and the
  tmp+rename mechanism are preserved. Operational change: deploys now land on
  `vhost.cherokeedictionary.net`; the `cherokeelessons.com` path becomes unused.
- Risk: low surface, high consequence — a publish replaces the live war; that's why atomicity
  (R4) and the explicit-development-gate behavioral run (SC-2) are specified rather than a
  routine verify.
- Traceability: R1/R2/R3 implement the stated switch (mechanism, symlinked path, replaced
  hosting); R4/R5 preserve the failure path and build provenance (gap coverage). SC-1→R1-R3;
  SC-2→R1,R4; SC-3→R4.

Byline: 🤖 Co-authored with AI: OpenCode (huggingface/zai-org/GLM-5.3-Flash)