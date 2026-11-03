# Plan — #19 env-variable database connections (the `DB_*` interface)

**Spec:** `.issues/19/spec.md` (round-2 validation PASS, 2026-11-03). Every item traces to a
spec SC. Build/test tooling: the repository's own `./gradlew` wrapper (Java 8 source level,
JUnit 4.13.2). No new dependencies; no DI-framework introduction (spec Approach).

## Item 1 — Env-based `State` configuration (R-1, R-2, R-3) → SC-1, SC-4

- **Deliverable:** In `AudioQualityVoteDao.State`, replace `loadPropertiesFile`'s properties
  read with environment reads at the same lazy first-use point (`onDemand` calls it before
  pool construction): read `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` (each required — missing
  key throws the `IllegalStateException` configuration-error path, logged, no silent
  fallback); resolve the password exclusively: `DB_PASSWORD_FILE` (read file, strip trailing
  newline(s)) XOR `DB_PASSWORD`; both or neither set is a configuration error. Assemble the
  JDBC URL in code: `jdbc:mariadb://<DB_HOST>:<DB_PORT>/<DB_NAME>?allowMultiQueries=true` —
  no other parameter. Set the pool's `connectionTimeout` to a finite value (10s — revised
  2026-11-03 on the developer's directive: a database that cannot be connected to fails in
  bounded time, SC-8; `0` waits indefinitely). Delete the `jdbc.*` properties extraction and
  the `properties` field
  (dead code rides this change, R-4). `HikariConfig` driver class and every other pool
  setting unchanged.
- **RED:** a new unit test (`StateConfigTest`, db module test sourceset) asserting
  `State`'s resolution: (a) with both `DB_PASSWORD` and `DB_PASSWORD_FILE` set → throws
  with the exclusivity message; (b) with neither set → throws; (c) with a missing
  `DB_HOST` → throws; (d) with a complete key set → the assembled URL equals
  `jdbc:mariadb://h:1/db?allowMultiQueries=true` and password-from-file strips a trailing
  newline. Fails to compile/pass against current code (fields/method don't exist in this form).
- **GREEN:** implement the resolution in `State`; keep a package-visible seam
  (e.g. `State.resolveConfig(Map<String,String> env)`) so the test drives resolution without
  touching the process environment; `loadPropertiesFile` (renamed, e.g. `loadConfig`) delegates
  to it with `System.getenv()`.
- **Instrument:** `./gradlew :audio-quality-vote-db:test` — the new test passes; source parse
  over the db module shows zero `jdbc.*` property reads remaining (grep), and the URL assembly
  carries exactly the scheme + `allowMultiQueries=true` (grep).

## Item 2 — DB tests route through the env contract (R-6) → SC-3, SC-6

- **Deliverable:** Revise `UidByEmailTest`: no hardcoded URL/user/password; resolve
  connection parameters through `AudioQualityVoteDao.State`'s env resolution (the same
  URL-assembly code the application uses — R-6/SC-3's requirement) from the `DB_*` keys;
  when any required key is absent, the class skips via `org.junit.Assume` with a stated
  reason ("DB_* environment keys not set — see SOP local-run procedure"). Add the
  multi-statement proof test: invoke the DAO's `addPendingEntry` (the two-statement
  `@SqlUpdate`) through the assembled URL against the local test MariaDB; assert both
  statements' effects commit (pending row inserted, carries `txt`/`file`). `allowMultiQueries`
  is proven, not assumed — the same statement errors without the parameter.
- **RED:** the multi-statement test against the current hardcoded-URL harness either fails or
  doesn't exist; the hardcoded constants violate the no-credentials structural check.
- **GREEN:** the revision above.
- **Instrument:** with the local test MariaDB's `DB_*` keys exported: `./gradlew
  :audio-quality-vote-db:test` passes including the multi-statement test; with the keys
  unset: the same task reports skips with the stated reason (captured output both ways).

## Item 3 — `.env` / `.env.example` split (R-5) → SC-5

- **Deliverable:** `.gitignore` gains `.env` (operator-maintained, never committed);
  `.env.example` (tracked) carries placeholder-only `DB_*` entries: `DB_HOST`, `DB_PORT`,
  `DB_NAME`, `DB_USER`, and `DB_PASSWORD_FILE` (production form) commented as the exclusive
  alternative to an uncommented `DB_PASSWORD` placeholder (local-dev form). No real values
  anywhere (R-8).
- **RED:** `.gitignore` lacks the entry; `.env.example` absent.
- **GREEN:** as stated.
- **Instrument:** `git check-ignore .env` reports ignored; `git ls-files .env.example` reports
  tracked; scan of both files shows placeholders only.

## Item 4 — Deploy script rewrite to the container path (R-7) → SC-7

- **Deliverable:** Rewrite `audio-quality-vote-war/deploy-dict.sh`: no properties-file
  shipping, no war scp to the old Tomcat host; deploy the application container to
  `vhost-58.cherokeedictionary.net` — the serving stack's Docker host — with env/secret
  delivery by key name and path only. The container image packages #26's gretty exported
  product, so the script's build step targets the exported product's future location and the
  script is sequenced with #26/devops#13 (SC-7's instrument explicitly excludes the
  end-to-end run until #26's export lands). Update SOP.md's Deployment section to the new
  shape (container deployment via vhost-58; the legacy properties/war flow removed).
- **RED:** current script ships the properties file and war to `vhost` (grep shows both);
  SC-7's parse fails.
- **GREEN:** the rewrite; the script documents (comments) the #26 sequencing dependency.
- **Instrument:** script parse/grep — zero `scp`/`ssh` of a properties file or `.war`, target
  host is `vhost-58.cherokeedictionary.net`, container + env/secret key names and paths only;
  bash `-n` syntax check passes.

## Item 5 — Behavioral smoke + env matrix (SC-2, SC-4) and structural secret scan

- **Deliverable:** Execute the behavioral instruments: throwaway MariaDB container on the
  backend port; launch the application with the `DB_*` environment set and a properties file
  carrying no `jdbc.*` keys; `GET /AudioQualityVote/api/audio/votes/list/AudioQualityVotes.csv`
  → 200 with the CSV header row; captured log carries no configuration-error lines. Then the
  three-run password   matrix: both forms set → configuration error, no connection attempt;
  neither set → configuration error; exactly one set (file form) → connection succeeds.
  Then the SC-8 run: wrong password via `DB_PASSWORD_FILE` → the CSV request errors within
  the pool's connection timeout (bounded).
  Finally the structural scan (SC-5): detect-secrets-class scan over the change's diff → zero
  hits.
- **RED:** N/A (execution of spec instruments, not code).
- **GREEN:** N/A.
- **Instrument:** captured command output per run (HTTP status + body header, log excerpt,
  matrix outcomes, scan output), recorded in the issue's validation record.

## Dependency order

Item 1 → Item 2 (tests consume Item 1's resolution seam) → Item 3 (independent, rides
anywhere before verify) → Item 4 (independent) → Item 5 (consumes Items 1–3's delivered
state). Structural checks (compile, full test suite, script syntax) run across the whole
change at the end.
