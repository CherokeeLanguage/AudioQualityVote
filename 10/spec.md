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

## Success criteria

| ID | Criterion | Class | Verification instrument |
|----|-----------|-------|-------------------------|
| SC-1 | Invoked from the repository root, the deliverable script reaches the upload step with the artifact path resolved from the script's directory (no `cwd`-relative miss) | Behavioral | Run `audio-quality-vote-war/deploy-dict.sh` from the repo root under a temporary ssh-config host override (unreachable target, committed content unedited): observe `gradle clean build war` BUILD SUCCESSFUL and the scp step actually attempting the upload (found the war — the `#8` failure shape `scp: stat local ... No such file or directory` does not recur) → scp fails against the unreachable host → script exits non-zero, `mv` never issued, live host untouched; override removed afterwards |
| SC-2 | The deploy script carries the full `#8` target/mechanism contract | Structural | Read of `deploy-dict.sh` post-change: host, symlinked path, `AudioQualityVote.war` deploy name, tmp+`mv` consecutive, guards and build step intact; grep retired refs (`vhost.cherokeelessons.com`, `/var/lib/tomcat9`) → 0 matches |
| SC-3 | Exactly one deploy script, dict-named, with no clom/CED residue | Structural | Filesystem + grep: `audio-quality-vote-war/deploy-dict.sh` exists; `deploy-clcom.sh` absent; grep `deploy-clcom|clcom|CED-AudioQualityVote` across the repo's deploy surfaces (shell scripts and gradle files, excluding attic, build outputs, issue-store) → 0 matches (the in-app `Login.ui.xml` privacy-policy link at `cherokeelessons.com` is a consumer-facing web link in deployed app content, outside the deploy surface and out of this change's scope) |

## Traceability
R1 ← the observed root-invocation failure; R2 ← `#8` contract preservation (gap coverage);
R3 ← "we only need the one deploy script" + "the site is dict, not clcom".
SC-1→R1; SC-2→R2; SC-3→R3.

Byline: 🤖 Co-authored with AI: OpenCode (huggingface/zai-org/GLM-5.3-Flash)