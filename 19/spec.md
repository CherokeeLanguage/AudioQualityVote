# Spec — migrate to env-variable database connections (caddy-routed docker instances)

**Provenance:** filed from cherokeelanguage-devops#12 (2026-11-02) — the phase-3 database
modernization surfaced the committed-credential finding and established the replacement serving
stack's configuration convention. Correction of record (2026-11-02): the committed-credential
finding is recorded against the public `cherokeedictionary` repo (`grails-app/conf/application.yml`
dataSource block; cherokeelanguage-devops#12 spec R-14, referenced path-only) — **this**
repository carries no committed credentials (template placeholders only; the real values live in
the git-ignored local file the deploy script ships). The application's actual gap is
architectural: it reads its database connection from a shipped properties file, outside the
stack's convention. The replacement serving stack runs this application as a Docker container on
the backend network (see the [serving-stack architecture](https://github.com/CherokeeLanguage/cherokeelanguage-devops/blob/main/docs/infrastructure/serving-stack-architecture-cherokeedictionary-net.md)), reachable database: Docker DNS `mariadb:3306`.

## Problem

1. **An architecture mismatch with the stack's recorded convention:** the serving-stack
   architecture records that connection credentials live in the consuming app's environment and
   connection parameters stay operator-provided — the dockerized replacement provisions
   applications through environment variables and mounted secret files (compose `environment:` +
   `/run/secrets/`), not baked-in config files. This application reads URL, username, and
   password from its shipped `AudioQualityVote.properties` instead.
2. **Delivery friction:** values ship via the deploy script's properties file; rotation means
   re-shipping a config file rather than updating operator-held secrets.

## Required end state

- **All database connection parameters come from the environment**: host, port, database name,
  username, and password resolved at container start (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`,
  `DB_PASSWORD_FILE`/`DB_PASSWORD` per the stack's `_FILE` idiom).
- **No credential material in the repository** — shipped config carries placeholders only
  (the password rotation itself is the operator security workstream; this change makes rotation
  a secrets-file update, not a re-ship).
- **The container deployment supplies the values** — compose service-level `secrets:` mounts
  (read-only, `/run/secrets/<name>` per the compose specification) with the `environment:` block
  referencing the operator-held secret files **by path** (the same pattern the MariaDB container
  uses: `*_FILE` references, never literals), and the app joins the `backend` network so
  `DB_HOST=mariadb` resolves by Docker DNS.
- **The assembled JDBC URL preserves the vote path's functional parameter**: `allowMultiQueries=true`
  is required by the vote path (per the repository's own template) and must survive URL
  re-assembly.
- **Local development** keeps working through an env file that is git-ignored and documented
  (`.env.example` carrying placeholder keys only).

## Migration shape (per caddy-routed instance)

1. Read the framework's supported env-override for the datasource (Grails: the
   `DB_*`-aware `application.yml` `environments.production.dataSource` block using
   `${DB_HOST:…}` interpolation, or — operative for this application, which has no framework
   datasource layer — JDBI/servlet: `System.getenv` at startup, assembling the JDBC URL from
   `DB_HOST`/`DB_PORT`/`DB_NAME` plus the preserved functional parameters).
2. **Driver-parameter facts (verified against the pinned driver's tagged source,
   mariadb-java-client 2.7.0):** the driver negotiates a utf8mb4 collation at connection by
   construction, so the template URL's `useUnicode=true` and `characterEncoding=utf8` are not
   MariaDB Connector/J parameters at all — unknown options are silently ignored in 2.x; they are
   MySQL-ism leftovers, not load-bearing; drop them. `connectionTimeZone` is MySQL Connector/J
   8.0.23+ vocabulary (MariaDB Connector/J only since 3.4.0) and is silently ignored by 2.7.0 —
   do not use it. Against the replacement server's UTC configuration, **no timezone parameter is
   needed**: the 2.x `serverTimezone` is inert at driver defaults, and absent, the driver adopts
   the server's session time zone; if an explicit pin is ever required, the 2.x-native mechanism
   is `sessionVariables=time_zone='UTC'`. The native URL scheme is `jdbc:mariadb://`
   (`jdbc:mysql://` remains an accepted compatibility alias in 2.7.0).
3. Point the connection at `mariadb:3306` (backend network) with the per-application user —
   each application's user is granted only its own database (the production database work
   recorded in [cherokeelanguage-devops#12](https://github.com/CherokeeLanguage/cherokeelanguage-devops/issues/12)).
4. Compose provides `DB_*` values from operator-provided secret files under
   `/srv/mariadb/secrets/` (by path reference, never literals).
5. Verify the repository is clean with the org's secret scan (template placeholders only; the
   dictionary repo's committed-credential remediation is that repo's own workstream).

## Acceptance

- `git grep -iE 'password\s*='` over the shipped config tree returns no credential values.
- The container starts with only env-provided connection parameters.
- The assembled JDBC URL carries `allowMultiQueries=true` (the vote path's requirement).
- The application connects to `mariadb:3306` as its own user and passes its smoke checks
  (phase-5 verification).

---
Correction record (2026-11-02): conformance check against the serving-stack architecture's
recorded conventions and the pinned driver's tagged source corrected — the `connectionTimeZone=UTC`
and keep-`characterEncoding`/`useUnicode` instructions (MySQL-Connector/J vocabulary, silently
ignored by mariadb-java-client 2.7.0), the committed-credential attribution (the recorded finding
is the dictionary repo's), the `allowMultiQueries=true` preservation requirement, and the dangling
`production-database-spec.md` link. Sources: the
[driver-parameter research card](https://github.com/CherokeeLanguage/cherokeelanguage-devops/tree/issues-data/research-cards/mariadb-connector-j-params.md)
in the devops store.
