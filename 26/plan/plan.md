# Plan — #26 production packaging via the gretty application-export workflow (buildProduct)

**Spec:** `.issues/26/spec.md` (remote: CherokeeLanguage/AudioQualityVote#26). Every item
traces to a spec SC. Build tooling: the repository's own `./gradlew` wrapper (Gradle 9.8.2,
gretty 3.1.9 pinned in `settings.gradle` pluginManagement). No new dependencies; no
application code changes (R-5). The Item 2 version bump is the SOP semver-per-PR regime's
mandate, declared inside SC-5 as the confinement's stated exception (spec revised in place
2026-11-03 on plan-validation finding 1).

## Item 1 — Production `jvmArgs` on the war module (R-2) → SC-2, SC-6

- **Deliverable:** `audio-quality-vote-war/build.gradle`'s `gretty { jvmArgs }` becomes
  `['-Dfile.encoding=UTF-8', '-Xmx512M']` — the dev-time `-Ddebug=true` removed; nothing
  else in the block changes. The same block feeds dev `appStart` (no separate dev/prod
  split — the spec's R-2 states the production set *is* the module's set) and is baked
  into the exported `conf/server.json` as `serverConfig.jvmArgs`.
- **RED:** `grep -n 'debug=true' audio-quality-vote-war/build.gradle` matches (current
  dev arg present); the exported `server.json` from the current tree would carry
  `-Ddebug=true` in `serverConfig.jvmArgs`.
- **GREEN:** the edit above.
- **Instrument:** `grep` shows no `debug=true` and exactly the two production args in the
  war module's `jvmArgs`; the SC-6 run (Item 4's dev-run leg) proves `appStart` still
  launches and serves with the changed args.

## Item 2 — SemVer bump for the PR (R-4; SOP regime) → SC-4

- **Deliverable:** root `build.gradle`'s `allprojects { version = ... }` line advances
  `1.0.0` → `1.1.0` (backward-compatible new functionality — the packaging workflow).
- **RED:** current trunk version is `1.0.0`; the SOP requires this PR's diff to carry the
  bump, so the pre-change state fails the regime check.
- **GREEN:** the edit above.
- **Instrument:** `grep -n "version = '1.1.0'" build.gradle`; the Item 3 export's
  `VERSION.txt` shows `1.1.0`.

## Item 3 — The export run and product-tree verification (R-1, R-3) → SC-1, SC-2, SC-4

- **Deliverable:** run `./gradlew :audio-quality-vote-war:buildProduct` (the repo
  wrapper; gretty's configureless export on the war module inherits its gretty block) and
  verify the product tree: start/run/stop/restart scripts, `conf/server.json`
  (`contextPath == "AudioQualityVote"`, `httpPort == 8080`, `jvmArgs` = the Item 1
  production set, no `-Ddebug=true`), `runner/`, `starter/`, `servletContainer/`
  (tomcat-embed 9.0.118 jars), `webapps/AudioQualityVote.war`, `extraResources/`
  carrying the GWT build output, `VERSION.txt` carrying `1.1.0`. The build log records
  the gretty version (3.1.9) and the packaged embedded-Tomcat version (9.0.118) — R-4's
  provenance inputs (the plugin pin in `settings.gradle` and the exported container jars
  are the recorded evidence).
- **RED:** `buildProduct` has never been run in this repository (spec's verified fact:
  zero `buildProduct` references repo-wide; no product tree exists) — the tree is absent.
- **GREEN:** no repository edit beyond Items 1–2 is expected; the export is a build
  action. If the Gradle 9.8.2 × gretty 3.1.9 export combination fails (spec O-1), that is
  a verified failure of the sanctioned wrapper invocation — report-with-evidence-and-halt,
  never a substitute toolchain.
- **Instrument:** captured `./gradlew :audio-quality-vote-war:buildProduct` output +
  scripted parse of the product tree: the named entries exist; `conf/server.json` parses
  with the stated values; `servletContainer/` contains tomcat-embed jars of version
  9.0.118; `VERSION.txt` equals `1.1.0`.

## Item 4 — Behavioral verification: the exported product serves, and the container-shaped shutdown is observed (R-1, R-2, R-3) → SC-3, SC-6

- **Deliverable:** two executed runs.
  1. **SC-6 dev-run leg:** with the Item 1 args in place and the SOP's env setup
     (`.env` loaded, `DB_*` + application keys, throwaway MariaDB on 127.0.0.1:3306),
     the SOP's mandatory daemon-environment caveat is honored — the daemon captures its
     environment at start, so either restart it (`./gradlew --stop`) or run daemon-free
     (`./gradlew --no-daemon :audio-quality-vote-war:appStart`). The launch and
     `GET http://localhost:8080/AudioQualityVote/` → 200 are captured (launch log + HTTP
     capture).
  2. **SC-3 containerized leg** (behavioral class — runs through the behavioral-testing
     skill's discipline, real remote-API shape): a throwaway container wrapping the
     exported product tree runs `start.sh` as the entry command, with a throwaway MariaDB
     **seeded to match the CSV's rendering semantics** — `_audio` rows for the voted aids
     **plus** non-zero vote values for them (a votes-only seed makes the endpoint error,
     not 200; entries grouped by aid, zero-valued entries render nothing, each rendered
     row reads its aid's `_audio` row) — with the `DB_*` environment and the application
     env keys, no properties file: `GET /AudioQualityVote/` → 200 (GWT shell);
     `GET /AudioQualityVote/api/audio/votes/list/AudioQualityVotes.csv` → 200 whose
     data-row count equals the number of distinct seeded voted aids; log shows the clean
     ingest-scan lines and no config errors. Then stop the container (SIGTERM,
     `docker stop`-equivalent) and record the shutdown behavior (clean exit / hang /
     kill — G-1's resolution) in the captured log.
- **RED:** neither run exists in the pre-change tree (no product to containerize; the
  dev-run posture carries `-Ddebug=true`).
- **GREEN:** the product of Items 1–3, executed.
- **Instrument:** captured command output both legs: dev leg's appStart log + 200
  capture; container leg's docker run/stop log, the two HTTP captures with the row-count
  assertion, and the recorded shutdown behavior.
- **Sequencing note:** both legs need the same seed; the container leg's MariaDB is
  throwaway (container-scoped), the dev leg's is the SOP local stack shape — one shared
  seed script, applied per throwaway store.

## Item 5 — No-packaging-regression check (R-5) → SC-5

- **Deliverable:** the change's diff review — build-file edits confined to the war
  module's `jvmArgs` (Item 1) and the root version line (Item 2, the SOP-mandated bump);
  all other gretty settings (`httpPort = 8080`, `servletContainer = 'tomcat9'`,
  overlays, `extraResourceBase`, `contextPath`, war name) untouched; application sources
  untouched; zero new dependencies.
- **RED:** n/a — this is the verification gate, not a code change; the pre-change diff is
  empty by definition.
- **GREEN:** the accumulated diff of Items 1–2 only.
- **Instrument:** `git diff` over the branch against trunk, inspected at verify time.

## Dependency order

Item 1 → Item 2 → Item 3 (export consumes both) → Item 4 (executes the exported
product) → Item 5 (diff review). Items 1 and 2 are independent edits but both precede the
export so the baked config and `VERSION.txt` are verified in one run.
