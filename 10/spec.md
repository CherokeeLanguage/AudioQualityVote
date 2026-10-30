# Spec: Deploy scripts run CWD-independent (build/upload from the script directory)

Intent (developer-stated, verbatim): after a real invocation from the repository root
(`audio-quality-vote-war/deploy-dict.sh`) failed with `scp: stat local
"build/libs/AudioQualityVote.war": No such file or directory` — a defect observed on the
post-merge deliverable of `AudioQualityVote#8` — the developer instructed: "fix it"; and
"create the spec and fix it".

## Problem

The `#8` deliverable scripts use relative paths to locate the built war
(`build/libs/AudioQualityVote.war`) and the tool invocation assumes the invocation context is
the script's own directory. Run from the repository root, gradle builds through `settings.gradle`
but the war lands at `audio-quality-vote-war/build/libs/`, while scp resolves `build/libs/`
against the caller's CWD — build succeeds, upload fails, deploy publishes nothing.

## Approach (amended per developer directives)

Single deploy script: "we only need the one deploy script", and the destination site is the
dictionary site — "the site is dict, not clcom". The repo therefore carries exactly one
deploy script, named `audio-quality-vote-war/deploy-dict.sh`, publishing `AudioQualityVote.war`
to `vhost.cherokeedictionary.net` (the context that host actually serves — verified live during
`#8`). `deploy-clcom.sh` and the `CED-AudioQualityVote` context (a `vhost.cherokeelessons.com`
artifact) are removed; no CED war has ever existed on the new host, and no other repo surface
references either (verified by grep at spec time).

The script pins its own working directory before any build/upload step:
`cd "$(dirname "$(readlink -f "$0")")"` immediately after the `set -o pipefail` guard —
readlink resolves invocation via symlinks/relative path, so the script behaves identically
invoked from anywhere. Build, scp, and ssh steps are unchanged (`#8` shape intact).

## Amendment (developer directive, same scope): deployment config rides the deploy

Directive: "then have the deploy script copy this file to the remote and rename it on the
remote when deployed" — referencing the operator-maintained `AudioQualityVote-prod.properties`
(root of the repo, git-ignored, prod-tuned values). The deploy script therefore
also ships deployment configuration: it refuses to deploy without a readable
`../AudioQualityVote-prod.properties` (relative to the script dir), then `scp`s a copy to the
remote webapps folder as `AudioQualityVote.properties.tmp`, remote-renames it to
`AudioQualityVote.properties`, **before** the war's scp/rename — so a fresh context init
always finds current config, preserving the same atomicity shape as the war publish. The
`#8` war mechanism, R1 pin, R3 single-script, and all guards stay untouched.

## Requirements

- R1. The deploy script (`audio-quality-vote-war/deploy-dict.sh` — the repo's one deploy
  script) SHALL resolve its artifacts and upload from its own directory, independently of the
  caller's working directory: the directory pin is part of the script, before the first
  build/upload step.
- R2. The `#8` contract is preserved: exactly one deploy script,
  `mconrad@vhost.cherokeedictionary.net:/opt/tomcat/webapps/`, `AudioQualityVote.war` deploy
  name, tmp-file scp + remote `mv` rename, `set -e`/`pipefail`/trap guards,
  `gradle clean build war`.
- R3. The `clcom` surface is retired per the developer's naming directive: `deploy-clcom.sh`
  no longer exists, the repo contains exactly one deploy script, and no live repo surface
  references `clcom` or `CED-AudioQualityVote`.
- R4. The deploy SHALL ship deployment configuration from the operator-maintained
  repo-root `AudioQualityVote-prod.properties`: the script aborts (non-zero, before any
  upload) when that file is missing or unreadable; otherwise it scp-copies it to the remote
  webapps folder as `AudioQualityVote.properties.tmp` and remote-renames it to
  `AudioQualityVote.properties`, executed before the war's scp/rename.

## Success criteria

| ID | Criterion | Class | Verification instrument |
|----|-----------|-------|-------------------------|
| SC-1 | Invoked from the repository root, the deliverable script reaches the upload step with the artifact path resolved from the script's directory (no `cwd`-relative miss) | Behavioral | Run `audio-quality-vote-war/deploy-dict.sh` from the repo root under a temporary ssh-config host override (unreachable target, committed content unedited): observe `gradle clean build war` BUILD SUCCESSFUL and the scp step actually attempting the upload (found the war — the `#8` failure shape `scp: stat local ... No such file or directory` does not recur) → scp fails against the unreachable host → script exits non-zero, `mv` never issued, live host untouched; override removed afterwards |
| SC-2 | The deploy script carries the full `#8` target/mechanism contract | Structural | Read of `deploy-dict.sh` post-change: host, symlinked path, `AudioQualityVote.war` deploy name, tmp+`mv` consecutive, guards and build step intact; grep retired refs (`vhost.cherokeelessons.com`, `/var/lib/tomcat9`) → 0 matches |
| SC-3 | Exactly one deploy script, dict-named, with no clom/CED residue | Structural | Filesystem + grep: `audio-quality-vote-war/deploy-dict.sh` exists; `deploy-clcom.sh` absent; grep `deploy-clcom|clcom|CED-AudioQualityVote` across the repo's deploy surfaces (shell scripts and gradle files, excluding attic, build outputs, issue-store) → 0 matches (the in-app `Login.ui.xml` privacy-policy link at `cherokeelessons.com` is a consumer-facing web link in deployed app content, outside the deploy surface and out of this change's scope) |
| SC-4 | The deploy ships the deployment config per R4 | Behavioral | Run `deploy-dict.sh` from the repo root under a temporary ssh-config host override (unreachable target, committed content unedited), with `AudioQualityVote-prod.properties` present: observe the script reach the config scp — the upload attempt targets `AudioQualityVote.properties.tmp` (config copy precedes the war upload) → scp fails against the unreachable host → script exits non-zero, no rename issued remotely, no war upload attempted; then re-run with the file renamed away: the script exits non-zero immediately with its not-found error, before any build/upload. Override removed afterwards |
| SC-5 | The config path is keyed to the script directory | Structural | Read of `deploy-dict.sh`: the properties path derives from the pinned script dir (`../AudioQualityVote-prod.properties` relative to `$SCRIPT_DIR`), not the caller's CWD; the not-readable guard precedes `gradle clean build war` |

## Traceability
R1 ← the observed root-invocation failure; R2 ← `#8` contract preservation (gap coverage);
R3 ← "we only need the one deploy script" + "the site is dict, not clcom";
R4 ← "have the deploy script copy this file to the remote and rename it on the remote when deployed".
SC-1→R1; SC-2→R2; SC-3→R3; SC-4→R4; SC-5→R4.

Byline: 🤖 Co-authored with AI: OpenCode (huggingface/zai-org/GLM-5.3-Flash)