# SOP — AudioQualityVote project

🤖 Co-authored with AI: OpenCode (huggingface/zai-org/GLM-5.3-Flash)

## Build/run mandate (from the developer, mandatory)

- **The project root `./gradlew` wrapper is the ONLY gradle to use.** Never invoke a
  system gradle (`gradle`, `/usr/bin/gradle`) — the wrapper pins the project's build
  toolchain; a system gradle has produced real overlay-resolution failures here. This
  applies to every surface, including the deploy script.
- **Default local run mode: `./gradlew :audio-quality-vote-war:appStart`** — the server
  runs detached and stays up; halt it with `./gradlew :audio-quality-vote-war:appStop`.
- `appRun` (foreground) is a named variant only, never the default; when it is used, it
  holds the terminal and needs stdin kept open to stop cleanly — that friction is why
  `appStart` is the default.

## Local verification stack (as used for #2/#12 live tests)

1. MariaDB 10.11 on `127.0.0.1:3306`:
   `docker run -d --name aqv-mysql -e MYSQL_ROOT_PASSWORD=root_pw -e MYSQL_DATABASE=audiovote -e MYSQL_USER=audiovote -e MYSQL_PASSWORD=audiovote_pw -p 3306:3306 mariadb:10.11`
2. Configuration at repo root `AudioQualityVote.properties` (git-ignored):
   `jdbc.url=jdbc:mysql://127.0.0.1:3306/audiovote?allowMultiQueries=true&useUnicode=true&characterEncoding=utf8`,
   `jdbc.username`, `jdbc.password`, `audio.folder=/tmp/opencode/aqv-audio`, and the
   `github.client_id`/`github.client_secret` pair from the operator's records.
3. `./gradlew :audio-quality-vote-war:appStart`, context at
   `http://localhost:8080/AudioQualityVote` (Tomcat 9 via gretty).
4. `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` when the default JVM is newer than
   Gradle's supported range (`Unsupported class file major version 69` = Java 25 under
   old Gradle).

## Browser verification (playwright CLI)

Browser probes for live verification run through the vendored CLI:

- **PATH-prefix invocation:** `PATH=<repo-root>/.tools/node/bin:$PATH playwright-cli …`
  — a bare invocation resolves the ambient system Node and fails at Playwright's
  version gate.
- **Browser choice:** open with `--browser chromium` — the `chrome` channel binary is
  absent here; chromium builds are cached at `~/.cache/ms-playwright/`.
- **Persistent profile:** open with `--persistent` — the daemon's profile home
  `~/.cache/ms-playwright/daemon/ud-bl-persist` carries stored logins across browser
  restarts. A custom `--profile <dir>` forks an orphan profile and loses them; headed
  (`--headed`) is required whenever a login hop is part of the verification. Named
  sessions (`-s=<name>`) run alongside any window already in use.
- **Login hop:** a completed GitHub login is a human identity action — the developer
  completes it in the visible browser window. App sessions are DB-backed session rows,
  so a server restart between probe phases does not log out the browser.
- **appStart invocation:** background it (for example
  `( ./gradlew :audio-quality-vote-war:appStart > appStart.log 2>&1 & )`) and poll the
  context URL instead of holding the calling shell.

## Versioning regime (from the developer, mandatory)

The project is evergreen: version identity advances with every merged PR — no release
train, tags, or GitHub Releases. Every PR carries exactly one SemVer bump of the
application version in its own diff; the version on trunk after a merge equals the
pre-merge trunk version incremented once:

- **MAJOR+1** (minor and patch reset) — incompatible changes.
- **MINOR+1** (patch reset) — backward-compatible new functionality.
- **PATCH+1** — backward-compatible defect fixes.

Levels follow SemVer's own definitions (https://semver.org/), applied by the PR author.
The version subject is the deployed application as a whole; backward-compatibility is
judged against its user-visible behavior, REST API, and stored data. The version's
source of truth is the single `version` declaration in the root `build.gradle`
(the `allprojects` block — explicit wiring; Gradle propagates no root version without
it), with every project in the build resolving it. The first PR under this regime set
the initial version (0.1.0, AudioQualityVote#15) instead of bumping.

## Deployment

`audio-quality-vote-war/deploy-dict.sh` — the repo's single deploy script — may be
invoked from anywhere (it pins its own directory); it builds with the project-root
wrapper (`./gradlew :audio-quality-vote-war:clean build war` run from the repo root) and
ssh-copies the operator-maintained repo-root `AudioQualityVote-prod.properties` to
`/opt/tomcat/webapps/AudioQualityVote.properties.tmp`, remote-renames it to
`AudioQualityVote.properties`, then scp's the war as `AudioQualityVote.tmp` and remote
renames it over `AudioQualityVote.war` (the atomic publish). Target:
`mconrad@vhost.cherokeedictionary.net`, whose `/opt/tomcat` is a symlink to
`apache-tomcat-9.0.7` (the symlinked webapps path is the deployment target). A properties
template is tracked at `audio-quality-vote-war/AudioQualityVote.properties.template`.

## Failure policy (from the developer, mandatory)

Hiding or papering over errors is a serious policy violation. Failure branches surface as
real HTTP errors (see `RestApiImpl.login` — 400/401/403/500/502 by branch) and emit `AQV
`-prefixed `System.err` diagnostics; login never masks a failure as a null/silent 204.