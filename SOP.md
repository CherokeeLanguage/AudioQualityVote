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