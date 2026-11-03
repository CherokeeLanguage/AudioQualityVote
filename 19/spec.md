# Spec — env-variable database connections for the containerized deployment (the `DB_*` interface)

**Provenance:** filed 2026-11-02 from cherokeelanguage-devops#12 (phase-3 database modernization;
the serving-stack configuration convention). Revised 2026-11-03 on the developer's evaluation
directives for this spec: **item (b) selected** — no properties file survives the migration; the
env semantics follow **the serving stack's standard approach for caddy + env vars for the Docker
guest** (the architecture's compose `environment:` + secrets-by-path delivery); the local-dev
`.env`/`.env.example` split is fixed; tests are revised as needed for the properties→env
migration; the deploy script changes to **deploy the container via
`vhost-58.cherokeedictionary.net`**. Companion specs in this repository: #25 (env-only
application configuration — `AUDIO_FOLDER`, `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET_FILE`;
owns the properties-mechanism retirement), #26 (gretty application-export packaging), #24
(dedicated ingest folder). Devops consumer: cherokeelanguage-devops#13 (phase 4) consumes
#19/#25/#26 as hard dependencies.

Repository: CherokeeLanguage/AudioQualityVote (trunk `main` @ `13e0d56`, verified 2026-11-03).

## Problem

The application reads its database connection from the deployed properties file:
`AppPathConfig.findConfigFile` searches a list of filesystem locations for
`{context}.properties`, and `AudioQualityVoteDao.State.loadPropertiesFile` extracts
`jdbc.url`/`jdbc.username`/`jdbc.password` from it (throwing `IllegalStateException` when the
file is unreadable). The deploy script ships the operator's `AudioQualityVote-prod.properties`
to the old Tomcat host before the war. The replacement serving stack runs this application as a
container on the Docker host `vhost-58`, on a host-local backend network where the database is
Docker DNS `mariadb:3306` — the stack's recorded convention delivers connection credentials in
the consuming app's environment (compose `environment:` + secret files by path), never as
baked-in config files. The application's gap is architectural: properties file instead of
environment.

Two facts bound the change and are verified against the code:

- **`allowMultiQueries=true` is load-bearing.** Two JDBI methods issue multi-statement SQL in a
  single `@SqlUpdate` string: `addPendingEntry` (insert-where-not-exists + update — the vote
  path) and `deleteUserById` (four delete statements). The MariaDB driver executes those only
  when the JDBC URL carries `allowMultiQueries=true`; a re-assembled URL that drops it breaks
  the vote path at runtime.
- **The pinned driver is `org.mariadb.jdbc:mariadb-java-client:2.7.0`** (`audio-quality-vote-db/build.gradle`;
  driver class `org.mariadb.jdbc.Driver` in the HikariCP config). Driver-parameter facts,
  verified against the pinned driver's tagged source (research card `mariadb-connector-j-params`
  in the devops store, 2026-11-02): the driver negotiates utf8mb4 at connection by construction,
  so the template URL's `useUnicode=true` and `characterEncoding=utf8` are not MariaDB
  Connector/J parameters at all (unknown options are silently ignored in 2.x) — MySQL-ism
  leftovers, dropped. `connectionTimeZone` is MySQL Connector/J 8.0.23+ vocabulary (MariaDB
  Connector/J only since 3.4.0) — silently ignored by 2.7.0, not used. `serverTimezone` is
  inert at 2.x defaults; absent it, the driver adopts the server's session time zone — against
  the replacement server's UTC configuration no timezone parameter is needed (the 2.x-native
  explicit-pin mechanism, if ever required, is `sessionVariables=time_zone='UTC'` — not used by
  this change). The native URL scheme is `jdbc:mariadb://` (`jdbc:mysql://` remains an accepted
  compatibility alias in 2.7.0 — not used).

## Requirements

- **R-1 — Database connection parameters come from the environment.** The application's database
  connection parameters SHALL be read from the environment: `DB_HOST`, `DB_PORT`, `DB_NAME`,
  `DB_USER`, and the password through `DB_PASSWORD_FILE` (path to an operator-held secret file —
  the production form) or `DB_PASSWORD` (direct form — local development only). Every key is
  required; there are no implicit defaults. The reads replace the `jdbc.*` properties reads in
  `AudioQualityVoteDao.State` — the same class that reads this configuration today, at the same
  (lazy, first-database-use) initialization point; no new framework, no new startup phase. A
  missing required key SHALL surface as the application's existing configuration-error failure
  (logged; database operations unavailable) — no silent fallback.
- **R-2 — Password-form exclusivity.** `DB_PASSWORD` and `DB_PASSWORD_FILE` SHALL be mutually
  exclusive: both set is a configuration error; neither set is a configuration error (R-1's
  failure path). This mirrors the official MariaDB container image's convention, whose
  entrypoint errors on the same condition ("Both $var and $fileVar are set (but are exclusive)")
  rather than picking a precedence. Secret-file contents SHALL be read with trailing newline(s)
  stripped (the Docker-secrets convention — the official image reads a secret file as
  `$(< file)`, which strips trailing newlines).
- **R-3 — The assembled JDBC URL.** The application SHALL assemble the JDBC URL in code from
  `DB_HOST`, `DB_PORT`, `DB_NAME` as
  `jdbc:mariadb://<DB_HOST>:<DB_PORT>/<DB_NAME>?allowMultiQueries=true` — the native 2.7.0
  scheme, the vote path's functional parameter preserved, and no other parameters (the
  MySQL-ism leftovers are dropped, per the driver facts above). The driver class
  (`org.mariadb.jdbc.Driver`) and the existing HikariCP pool settings are unchanged
  except one — revised 2026-11-03 on the developer's directive during verification: the
  pool's `connectionTimeout` changes from `0` (wait indefinitely) to a finite value, so a
  database that cannot be connected to (wrong credentials, unreachable host) surfaces as a
  request error in bounded time instead of an unbounded hang; the driver class and every
  other pool setting stand.
- **R-4 — No properties file survives (developer's item (b)).** The end state has no properties
  file. The DB path adds no properties-file dependency and the `jdbc.*` properties extraction is
  deleted with this change (dead code rides the change that orphans it). The file mechanism's
  retirement — removing the file-read surface, `AppPathConfig`'s file search, and migrating the
  remaining keys (`audio.folder`, `github.*`) — is #25 R-4's deliverable; the two specs sequence
  as #25's provenance records (this spec lands with or before #25). In the intermediate state
  (after #19, before #25) the file remains structurally required for the keys #25 migrates; this
  change neither extends nor preserves the DB reads from it, and the DB path works whether or
  not a properties file is present.
- **R-5 — Local development through the same env contract.** Local development SHALL deliver
  the `DB_*` keys through the same environment contract: an operator-maintained `.env` file at
  the repo root supplies the values and is **git-ignored** (the `.gitignore` entry rides this
  change); `.env.example` — **tracked**, placeholders only — is its template (the file's
  full-key documentation is #25 R-6's deliverable, which documents #19's `DB_*` keys within it;
  if this spec lands first, it creates `.env.example` carrying the `DB_*` entries and #25
  extends it). The `.env` file is consumed by the invoking shell — the application reads only
  the process environment and never parses `.env`. The values reach the gretty-forked webapp JVM
  through an env-passing invocation form: gretty 3.1.9 exposes no environment knob for the
  forked runner (verified against the v3.1.9 source — `GrettyConfig`/`ServerConfig`/
  `WebAppConfig` carry `jvmArgs`/`systemProperties` only; `DefaultLauncher` launches the runner
  via `execOperations.javaexec` and never sets `spec.environment`, so the forked JVM inherits
  the Gradle daemon's process environment). The two working forms: source `.env` into the
  invoking shell and (re)start the daemon so it captures the values
  (`set -a; . ./.env; set +a; ./gradlew --stop`, then invoke normally), or run daemonless with
  the sourced shell (`set -a; . ./.env; set +a; ./gradlew --no-daemon
  :audio-quality-vote-war:appStart`). The SOP's local-run procedure documenting this is #25
  R-4's deliverable.
- **R-6 — Tests route through the env contract.** Database-touching tests SHALL source their
  connection parameters from the same `DB_*` environment keys and the same URL assembly the
  application uses — no hardcoded connection URLs or credentials in tracked test sources
  (`UidByEmailTest` today hardcodes URL, user, and password). With the environment keys absent,
  such tests SHALL skip with a stated reason (a JUnit assumption), never fail spuriously; the
  documented local procedure (R-5) supplies the values.
- **R-7 — The deploy path deploys the container via vhost-58.** The repository's deploy script
  (`deploy-dict.sh`) SHALL be changed from the legacy flow (scp the properties file, then the
  war, to the old Tomcat host) to deploying the containerized application to
  `vhost-58.cherokeedictionary.net` — the serving stack's Docker host. The script ships no
  properties file, no war to the old host, and no configuration values (key names and paths
  only — devops#13 R-4's rule); its result is the application container running on the serving
  stack with the stack's env/secret delivery. The container image packages the gretty exported
  product (#26's deliverable), so the script rewrite sequences with #26 and against
  devops#13's compose service; the SOP's Deployment section is updated with the change.
- **R-8 — No credential material in any repository artifact.** This change introduces no secret
  material: shipped configuration and documentation carry placeholders and key names only;
  operator values live in operator-held files outside the repository (the git-ignored `.env`
  locally; secret files on the Docker host per the stack convention).

## Success criteria

| ID | Criterion | Class | Instrument |
|---|---|---|---|
| SC-1 | The DB connection resolves wholly from the `DB_*` environment keys; the assembled URL is exactly `jdbc:mariadb://<DB_HOST>:<DB_PORT>/<DB_NAME>?allowMultiQueries=true`; the driver class and pool settings are unchanged except `connectionTimeout`, which is a finite value (revised 2026-11-03) | structural | source parse over the db module: zero `jdbc.*` properties reads remain; the URL assembly carries the scheme, `allowMultiQueries=true`, and no other parameter; the HikariCP config's driver class and pool settings match trunk's except a finite `connectionTimeout` |
| SC-2 | The application connects and serves against a database reached solely through the env-provided parameters | behavioral | smoke run: throwaway MariaDB container + the application launched with the `DB_*` environment set and a properties file carrying no `jdbc.*` keys (absent entirely once #25 composes) — `GET /AudioQualityVote/api/audio/votes/list/AudioQualityVotes.csv` → 200 whose body carries the CSV header row (the header proves the votes query executed through the env-resolved pool; row-level reconciliation is devops#13's check); the captured log carries no configuration-error lines |
| SC-3 | The multi-statement vote path executes through the assembled URL — `allowMultiQueries=true` proven, not assumed | behavioral | the revised DB test executes the DAO's multi-statement update (`addPendingEntry`) through the same URL-assembly code the application uses, against the local test MariaDB: both statements' effects commit (the pending row is inserted and carries `txt`/`file`); without the parameter the same statement errors, so the passing assertion through the assembled URL is the proof |
| SC-4 | The password-form contract is enforced | behavioral | three-run env matrix on the smoke harness: both `DB_PASSWORD` and `DB_PASSWORD_FILE` set → the configuration error surfaces and no connection is attempted; neither set → the configuration error surfaces; exactly one set → the connection succeeds (the file form via SC-2's run, the direct form via the test run) |
| SC-5 | No credential material in the change's artifacts; the `.env` split is in place | structural | a detect-secrets-class scan over the change's diff returns zero hits; `.gitignore` carries the `.env` entry; `.env.example` exists (tracked) with placeholder `DB_*` entries; no shipped configuration or documentation carries an operator value |
| SC-6 | Database-touching tests route through the env contract | structural + behavioral | structural: tracked test sources carry no hardcoded connection URLs or credentials; behavioral: with the local test MariaDB's `DB_*` keys set the revised tests pass; with the keys absent they skip with the stated reason in the output |
| SC-7 | The deploy script's new shape: container deployment via vhost-58 | structural | script parse: no properties-file shipping, no war scp to the old Tomcat host; targets `vhost-58.cherokeedictionary.net`; deploys the application container with env/secret-by-path delivery (key names and paths only). The script's end-to-end run is verified by the deployment composition (devops#13's SC-1/SC-4 instruments) once #26's export lands — not by this spec's instrument |
| SC-8 | A database that cannot be connected to fails in bounded time — a request against a wrong-credential or unreachable database surfaces as an error within the pool's connection timeout, never an unbounded hang | behavioral | smoke run with a wrong password via `DB_PASSWORD_FILE`: the CSV request returns an error response within the pool's connection timeout (bounded — no indefinite wait) |

## Impact

**Consumers:** the application's database connection path (the repository's only DB consumer);
devops#13's deployment — the first production consumer of the `DB_*` interface (its compose
`environment:` + `secrets:` deliver these keys over the backend network); #25 (composes with
this change; its no-file boot depends on this migration); #26's smoke run (exercises the
migrated DB path); the local development workflow; the deploy path (the legacy flow is
superseded). **Blast radius:** the db module's configuration read and its DB-touching tests
(one class each — `AudioQualityVoteDao.State`, `UidByEmailTest`; the db module holds the
repository's only test sources), the deploy script, the SOP's Deployment section,
`.gitignore`, `.env.example`. No REST API changes, no schema changes, no client-side changes.
**Risks:** a re-assembled URL silently dropping `allowMultiQueries=true` would break the vote
path at runtime — SC-3 proves the parameter through the assembled URL; charset/timezone
behavior changing if the dropped parameters were load-bearing — the driver facts verify they
are not; the intermediate-state window (the file still required for non-DB keys until #25) —
sequencing stated in R-4; a deploy script rewritten ahead of #26's export could not yet deploy
— the sequencing is stated in R-7.

## Out of scope

- The non-DB configuration keys and the properties-mechanism retirement itself (#25's
  deliverable — `AUDIO_FOLDER`, `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET_FILE`/
  `GITHUB_CLIENT_SECRET`, the `AppPathConfig` file-search removal, the properties template's
  deletion, the SOP's env-var local-run procedure).
- Production packaging via the gretty application-export workflow (#26) — the deploy script's
  container mechanics consume its exported product.
- The compose service, image assembly, host provisioning, routing, and posture (devops#13's
  deliverables).
- Password rotation and secret-file management (the operator security workstream; this change
  makes rotation a secrets-file update rather than a re-ship).
- OAuth re-registration or callback changes; `Consts.CLIENT_ID` (a compiled UI constant,
  unaffected by the DB migration).
- Dependency or framework upgrades, DI-framework introduction, or refactoring beyond the
  configuration surface (minimal-change discipline; #25's shared posture).
- The cutover, DNS/zone changes, and the old host's decommissioning (devops#12/#13 sequence).

## Approach

In `AudioQualityVoteDao.State`, replace the properties read with environment reads at the same
initialization point: read the four location/user keys, resolve the password through the
exclusive `_FILE`-or-direct form (R-2), assemble the JDBC URL in code (R-3), and delete the
`jdbc.*` properties extraction. No framework introduction; the pool configuration and driver
class stand. Revise the DB-touching tests (`UidByEmailTest` and any sibling) to the env
contract with assumption-based skipping, including the multi-statement execution test through
the assembled URL (SC-3). Rewrite `deploy-dict.sh` to the container deployment against
`vhost-58.cherokeedictionary.net` (sequenced with #26 — its image input is the exported
product) and update the SOP's Deployment section. Create `.env.example` with the `DB_*`
placeholder entries (extended by #25 to the full key set) and add the `.env` git-ignore entry.

## Traceability

| Source | Requirement | Criteria |
|---|---|---|
| Developer directive 2026-11-03: "select item b" — the file mechanism retires; OAuth/`audio.folder` move to `*_FILE` secrets too | R-4 (with the retirement's ownership assigned to #25 R-4, per that spec's provenance) | SC-1, SC-2 |
| Developer directive 2026-11-03: "whatever the standard approach is for caddy + env vars for the docker guest" | R-1, R-2 (the architecture L6/L8 compose `environment:` + secrets-by-path delivery; the official image's `_FILE` exclusivity and trailing-newline convention) | SC-2, SC-4 |
| Developer directive 2026-11-03: "fix" — the `.env`/`.env.example` conflation and the local-dev delivery mechanics | R-5 | SC-2, SC-5 |
| Developer directive 2026-11-03: "revise tests as needed for the migration from properties to env in docker" | R-6 | SC-3, SC-6 |
| Developer directive 2026-11-03: "the deploy script will need to be changed to deploy the container via vhost-58.cherokeedictionary.net" | R-7 | SC-7 |
| Original provenance: cherokeelanguage-devops#12; serving-stack architecture L6/L8 (backend network, `mariadb:3306`, credentials in the consuming app's environment, connection parameters operator-provided) | R-1's delivery shape; R-8 | SC-2, SC-5 |
| Driver-parameter research card `mariadb-connector-j-params` (verified against the pinned 2.7.0 tagged source, 2026-11-02) | R-3's parameter facts | SC-1, SC-3 |
| Verified code facts (multi-statement `@SqlUpdate`s; three independent file consumers; lazy DAO initialization) | R-3's `allowMultiQueries` ground; R-1's resolution point; R-4's intermediate state | SC-1, SC-3, SC-2 |
| gretty v3.1.9 source verification (no environment knob; the forked runner inherits the daemon's process environment) | R-5's invocation forms | SC-2 |
| #25 provenance ("lands with or before this change") and devops#13 R-3/R-4 (the stack's consumption of `DB_*` and env-only delivery) | R-4's sequencing; R-7's stack delivery | SC-2, SC-7 |
| Standing floor norm (no secrets in output, code, or commits) | R-8 | SC-5 |
