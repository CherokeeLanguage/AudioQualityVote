# Spec — production packaging via the gretty application-export workflow (buildProduct)

**Provenance:** recorded developer directive 2026-11-02 (cherokeelanguage-devops#13 re-scope
discussion): "this should use the built-in gretty application export workflow if it doesn't
already" — verified it does not (repo-wide grep for `buildProduct`/`archiveProduct`/product
configuration: zero matches; gretty 3.1.9 is applied only as a dev-run engine and war builder;
today's production path is `deploy-dict.sh` scp'ing the war onto a stock Tomcat 9.0.7). Split
directive: "let's split it into two specs" — this is the packaging half; the configuration half
is #25 (env-only application configuration). Devops consumer: cherokeelanguage-devops#13
(revised scope) packages this exported product into the deployed image.

Repository: CherokeeLanguage/AudioQualityVote (trunk `main` @ `13e0d56`, verified 2026-11-02).

## Problem

The containerized deployment needs a self-contained, runnable application artifact. The repo
builds `AudioQualityVote.war` but has no export/packaging workflow for a deployable tree; the
developer directs the gretty application-export workflow as the packaging. Adopting it means the
deployment consumes the exported product (start scripts + baked server config + embedded
container runtime + the war) instead of hand-assembling a war onto a stock Tomcat base.

## Verified facts this spec builds on (research card `gretty-product-export-workflow` in the devops store, 2026-11-02; gretty source at tag v3.1.9)

- The repo pins `org.gretty` **3.1.9** (settings.gradle `pluginManagement`; wrapper Gradle
  9.8.2). Configureless `buildProduct` works on any gretty-applying project and inherits the
  project's `gretty { }` settings; run on `:audio-quality-vote-war` it bundles exactly that
  webapp (at the root project it would bundle all three gretty webapps — wrong shape).
- The product tree: `run.sh`/`start.sh`/`stop.sh`/`restart.sh`, `VERSION.txt`,
  `conf/server.json` (server + webapp config baked at build time), `runner/`, `starter/`,
  `servletContainer/` (embedded Tomcat jars — 3.1.9's tomcat9 pins **9.0.118**),
  `webapps/AudioQualityVote.war`, plus `extraResources/` for the GWT build output referenced
  from `server.json`.
- The tomcat9 product does **not** bundle a real Tomcat distribution — it bundles embedded
  Tomcat driven by gretty's `GrettyStarter`; the product root acts as CATALINA_BASE
  (`temp/`, `work/` created at runtime — the image must allow those writes; devops#13's concern).
- Launch scripts pass no environment through (`JAVA_OPTS` is never read); JVM args come from
  `server.json`'s `serverConfig.jvmArgs`, baked from the war module's `gretty { jvmArgs }`.
  The JVM process still inherits the container's environment — the env-var configuration
  (#19/#25) works under this packaging unchanged.
- Scripts run the server in the foreground, but the two start paths differ: **`start.sh` is the
  container-safe foreground path** (plain block on the server thread); **`run.sh` is the
  interactive path** — the baked `server.json` carries no `interactiveMode`, so the default
  `stopOnKeyPress` applies and the starter reads stdin: under headless stdin (a container
  default) it reads EOF and stops itself immediately after starting. The deployment-shaped run
  uses `start.sh`; `stop`/`restart` go through a negotiated service port; the starter derives
  paths from its own jar location (the tree is relocatable, CWD-independent).
- Version landscape: gretty 4.x/5.x sampled lines (v4.1.10, v5.1.0, master) dropped tomcat9
  support — the coupling note rests on those sampled tags (the research card's disclosed
  sampling limit), with the repo's existing pin (3.1.9) as the operative fact: the pinned
  release is the tomcat9-capable line this packaging uses. Adopting the tomcat9 product accepts
  that coupling. Side effect: embedded Tomcat 9.0.118 upgrades the old host's stock Tomcat
  9.0.7.

## Required end state

- **R-1 — The production artifact is the gretty product export:** invoked through the
  repository's own wrapper as
  `./gradlew :audio-quality-vote-ui:gwtCompile && ./gradlew :audio-quality-vote-war:buildProduct`
  — two wrapper invocations in order, producing the deployable product tree for the war
  webapp alone. (Revised 2026-11-03 on implementation discovery: gretty 3.1.9 captures the
  webapp's `extraResourceBase` at build *configuration* time, so a cold single invocation
  — the GWT output directory not yet existing — silently exports a product without
  `extraResources/` and without `server.json`'s `extraResourceBases`; a second invocation
  in the same tree includes both. The GWT-compile first step makes the export reproducible
  from a pristine checkout; no build-file change is needed.)
- **R-2 — Production JVM args:** the war module's `gretty { jvmArgs }` becomes the production
  set — `-Xmx512M` (the stack's container posture) and `-Dfile.encoding=UTF-8`; the dev-time
  `-Ddebug=true` is removed. Dev `appStart` keeps working with the production set.
- **R-3 — Context continuity:** the exported `server.json` carries
  `webApps[0].contextPath = "AudioQualityVote"` (unchanged from the module's existing
  `contextPath` setting) — the segment the devops routing line forwards and the basis of the
  table-prefix continuity.
- **R-4 — Provenance recorded:** the product's `VERSION.txt` carries the project version
  (semver-per-PR, per the merged #15/#17 work); the build records the gretty version (3.1.9)
  and the embedded-Tomcat version (9.0.118) it packaged — the containerization's provenance
  inputs.
- **R-5 — No packaging regression:** the war module's other gretty settings (`httpPort = 8080`,
  `servletContainer = 'tomcat9'`, overlays, `extraResourceBase`, war name
  `AudioQualityVote.war`) are unchanged; the export adds no new repository dependencies and no
  application code changes.

## Success criteria

| ID | Criterion | Class | Instrument |
|---|---|---|---|
| SC-1 | The wrapper-invoked export produces the product tree | structural | run the R-1 two-step invocation (`./gradlew :audio-quality-vote-ui:gwtCompile` then `./gradlew :audio-quality-vote-war:buildProduct` — the repo wrapper; captured output) **from a pristine shallow checkout**, so the reproducibility the revision records is itself verified; parse `build/output/`: start/run/stop/restart scripts, `conf/server.json`, `runner/`, `starter/`, `servletContainer/` (tomcat-embed 9.0.118 jars), `webapps/AudioQualityVote.war`, `extraResources/` (carrying the GWT build output — the client delivery the product serves via `server.json`'s `extraResourceBases`), `VERSION.txt` |
| SC-2 | The baked server config is the production shape | structural | parse `conf/server.json`: `contextPath == "AudioQualityVote"`, `httpPort == 8080`, `jvmArgs` carries `-Xmx512M` and `-Dfile.encoding=UTF-8` and carries no `-Ddebug=true` |
| SC-3 | The exported product runs and serves, and its container-shaped shutdown behavior is observed | behavioral | (after #19/#25 land — the composition this smoke verifies) run the product **containerized** (a throwaway container wrapping the product tree) with `start.sh` as the entry command, against a throwaway MariaDB **seeded to match the CSV's rendering semantics** — the CSV renders one row per **distinct aid carrying at least one non-zero vote** (entries grouped by aid; zero-valued entries render nothing), and each rendered row reads the aid's `_audio` row, so the seed is: `_audio` rows for the voted aids **plus** non-zero vote values for them (a votes-only seed makes the endpoint error, not 200); with the `DB_*` environment (per #19) and the application env keys (per #25), properties file absent: `GET /AudioQualityVote/` → 200 (GWT shell); `GET /AudioQualityVote/api/audio/votes/list/AudioQualityVotes.csv` → 200 whose data-row count equals the number of distinct seeded voted aids; the log carries the clean ingest-scan lines and no config errors; **then stop the container (`docker stop`-equivalent SIGTERM) and record the shutdown behavior** (clean exit / hang / kill — G-1's resolution) in the captured log |
| SC-4 | Provenance recorded | structural | `VERSION.txt` carries the project version; the build log/records state gretty 3.1.9 and embedded Tomcat 9.0.118 matching the pinned plugin line |
| SC-5 | No packaging regression | structural | `git diff` over the change: build-file edits confined to the war module's `jvmArgs` plus the root `build.gradle` version line (the SOP semver-per-PR regime mandates exactly one version bump in every PR's diff — the declared exception to the confinement, per merged #15/#17 precedent); all other gretty settings and application sources untouched; zero new dependencies |
| SC-6 | The dev-run path keeps working with the production jvmArgs (R-2's preserved behavior) | behavioral | with the changed `jvmArgs` in place and the properties/env setup the dev-run documentation states, `./gradlew :audio-quality-vote-war:appStart` (the repo wrapper) launches and the GWT shell serves 200 on the dev port; the launch and fetch are captured |

## Out of scope

- Docker image assembly, compose service, host provisioning, routing (devops#13's deliverables).
- The env-var configuration surface (#19, #25) and the DB migration (#19).
- The legacy `deploy-dict.sh` path's behavior on the old host (deprecation-noticed by #25; the
  cutover replaces the old deployment).
- Tomcat 10/11 + jakarta modernization (rejected — the recorded as-is decision stands).
- gretty version upgrades (the sampled 4.x/5.x lines drop tomcat9 support; the pinned 3.1.9 is
  the accepted coupling).

## Open items

- **O-1 — Gradle 9.8.2 × gretty 3.1.9 `buildProduct` compatibility:** verified working for the
  dev tasks per the repo's SOP, but the export task combination was not executed in research
  (read-only session; source-level layout verification only). The SC-1 build run is the
  resolving instrument; a verified failure of the sanctioned wrapper invocation is
  report-with-evidence-and-halt — never a substitute toolchain.
- **G-1 — SIGTERM behavior of the embedded-Tomcat runner under `docker stop`** (graceful
  shutdown vs the `stop.sh` service-port path) — untested in research; SC-3's instrument carries
  the containerized stop observation that resolves it (the captured shutdown behavior is the
  answer devops#13's container design consumes).

## Traceability

| Source | Requirement | Criteria |
|---|---|---|
| "use the built-in gretty application export workflow" | R-1 | SC-1 |
| "minimal needed changes" | R-5; out-of-scope bounds | SC-5 |
| Stack posture (512 MiB; the devops container limits) | R-2 | SC-2 |
| Segment continuity (devops#13 R-2 grounds) | R-3 | SC-2, SC-3 |
| Versioning discipline (semver-per-PR, merged #17) | R-4 | SC-4 |
| #19/#25 (env-only config) | SC-3's smoke composition | SC-3 |
| Research card (gretty facts, version landscape — with its disclosed sampling limits) | R-1's shape; the coupling note | SC-1, SC-4 |
| Dev-run continuity (R-2's appStart clause) | R-2 | SC-6 |

🤖 Co-authored with AI: OpenCode (huggingface/zai-org/GLM-5.3-Flash)

