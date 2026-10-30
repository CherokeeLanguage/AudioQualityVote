# Plan — AudioQualityVote#15: Semver-per-PR versioning; version and build date on Login and About

Spec: `.issues/15/spec.md` (validated PASS, fresh-context). Terminal authorization
("approved for PR") carries plan → implement → PR under this authorization; merge
remains the developer's gate.

Version initialization value: **0.1.0** — the spec fixes no number; 0.1.0 is SemVer's
own initial-development convention (semver.org §"FAQ": 0.y.z for initial development),
and this PR is the regime's initialization PR (R-2's initialization clause). All
version literals referenced by the instruments below mean this value until replaced.

Verification stack (per SOP.md, as used for #2/#12 live tests): MariaDB 10.11 container
`aqv-mysql` on 127.0.0.1:3306, root `AudioQualityVote.properties` (git-ignored) with
jdbc + oauth + audio.folder config, built and run via the project-root wrapper
`./gradlew :audio-quality-vote-war:appStart` (context `http://localhost:8080/AudioQualityVote`),
halted with `:audio-quality-vote-war:appStop`. Browser probes run via playwright CLI.
The produced war path is `audio-quality-vote-war/build/libs/AudioQualityVote.war`
(the path `deploy-dict.sh` scp's from).

## Items

### P-1 — SC-1: version declared once in root build.gradle; every project resolves it

- **Deliverable:** root `build.gradle` gains a single `allprojects { version = '0.1.0' }`
  block (one declaration literal; sets root plus all five subprojects — Gradle propagates
  no root `version` without wiring).
- **RED:** on the branch before the change: `grep -n "version" build.gradle` shows no
  project-version declaration, and `./gradlew -q :properties :audio-quality-vote-db:properties
  :audio-quality-vote-server:properties :audio-quality-vote-shared:properties
  :audio-quality-vote-ui:properties :audio-quality-vote-war:properties` reports
  `version: unspecified` for all six projects — the SC-1 criterion fails today.
- **GREEN:** add the block at the top of root `build.gradle`.
- **Instrument:** rerun the RED's properties invocation — every reported `version:`
  equals `0.1.0`; `grep` of root `build.gradle` shows exactly one declaration.

### P-2 — SC-3: the produced war self-describes with its build's version and UTC build date

- **Deliverable:** the host page `audio-quality-vote-ui/src/main/webapp/index.html`
  carries `<meta name="aqv-version" content="${aqvVersion}">` and
  `<meta name="aqv-build-date" content="${aqvBuildDate}">` (head section); the ui
  module's build stamping expands the tokens **at both assembly paths the artifact
  flows through** — in `audio-quality-vote-ui/build.gradle`'s existing `war { … }`
  configuration (the packaging path the deployed `AudioQualityVote.war` and
  `deploy-dict.sh` use) and on gretty's `prepareInplaceWebAppFolder` task (the inplace
  webapp `appStart` serves — the SOP local-run stack is inplace, gretty builds
  `build/inplaceWebapp` from webapp sources independent of the war). Both use a
  per-line copy `filter` (`line.replace('${aqvVersion}', project.version.toString())
  .replace('${aqvBuildDate}', <UTC date>)`, evaluated at task execution — the values
  are that build's); Gradle's whole-file `expand` (SimpleTemplateEngine) mangles this
  file's `document.write` content and is not usable here (documented engine behavior,
  confirmed on this artifact).
- **RED:** after the P-1 GREEN, `./gradlew :audio-quality-vote-war:clean build`, then
  `unzip -p audio-quality-vote-war/build/libs/AudioQualityVote.war index.html | grep -c "aqv-version"`
  = 0 — the criterion fails today (no metadata in the artifact).
- **GREEN:** the token additions above.
- **Instrument:** clean build; `unzip -p … index.html` shows both metas with
  `content="0.1.0"` and the build's UTC date. Then bump the declared literal to
  `0.1.1` and rebuild — the war carries `0.1.1`; restore `0.1.0` (the initialization
  value this PR ships) and rebuild. A value that never changed proves hardcoding;
  the changed value proves build-derivation (SC-3's build-derived clause).
- **Dependencies:** after P-1 (the stamped value is the resolved project version).

### P-3 — SC-4: the Login view displays version and build date

- **Deliverable:** a GWT helper `com.cherokeelessons.audio.quality.ui.BuildInfo` in
  `audio-quality-vote-ui` (static `version()` / `buildDate()` returning the
  corresponding meta's `content`, read via the project's existing Elemental2 surface —
  `DomGlobal.document.querySelector("meta[name='…']")` cast to `HTMLMetaElement`,
  null-safe → empty string); the Login view gains a display row:
  `Login.ui.xml` adds `<m:MaterialRow style="text-align: center"><h:Text
  ui:field="buildInfo"/></m:MaterialRow>` below the source/privacy links row, and
  `Login.java` gains `@UiField protected Text buildInfo;` (`gwt.material.design.client.ui.html.Text`,
  the same widget class `AppPresenter` setTexts) populated in the constructor after
  `createAndBindUi` with `"Version <v> — built <d>"` when both values are non-empty.
- **RED:** stack up per SOP; browser probe (`playwright-cli`) on `http://localhost:8080/AudioQualityVote/`
  — the rendered login view contains neither value: fails SC-4 today.
- **GREEN:** the deliverable above; rebuild (`./gradlew :audio-quality-vote-war:clean build`)
  and `appStart` again so the client bundle includes the change.
- **Instrument:** browser probe on the login view — the visible text carries
  `Version 0.1.0` and the artifact's build date, equal to `unzip -p …` of the same
  war's stamped metas (display value == artifact value, the SC-4 correspondence).
- **Dependencies:** after P-2 (the metas the display reads must exist in the artifact
  the server serves; also one full build serves both items' evidence).

### P-4 — SC-5: the About view displays version and build date

- **Deliverable:** same pattern in `AboutDisplay.ui.xml` (an `h:Text` display row at
  the bottom of the About panel) and `AboutDisplay.java` (`@UiField Text buildInfo`
  populated in its constructor via `BuildInfo`); identical read-and-render code path
  to P-3 (shared helper), separate view files.
- **RED:** with an authenticated browser session (see Instrument), the rendered About
  view contains neither value: fails SC-5 today (the existing About panel shows only
  the description/instructions text).
- **GREEN:** the deliverable above; rebuild + `appStart`.
- **Instrument:** the About view is login-gated by design (`AppPresenter.maybeShowLogin`
  → `api.loggedIn()`); the instrument therefore runs a browser session that completes
  the real GitHub OAuth login. The login hop is a human identity action requiring the
  developer's GitHub credentials in the browser — the developer completes that hop in
  the playwright-driven session on request; the agent drives every other step (open
  app, land on login, wait for the redirect-and-login to consume the state, then
  observe the About view — first view shown post-login — and check its text equals the
  artifact's stamped values). If the developer declines or credentials are unavailable,
  the remaining-unverified SC is reported as-is, never asserted as verified.
- **Dependencies:** after P-3 (shares `BuildInfo`; one rebuild + `appStart` serves both).

### P-5 — SC-6: SOP.md documents the regime

- **Deliverable:** SOP.md gains a "Versioning regime" section stating: evergreen
  convention (identity advances per merged PR; no release train); one SemVer bump per
  PR, carried in the PR's own diff, exactly one increment (PATCH+1 fixes / MINOR+1
  backward-compatible functionality with patch reset / MAJOR+1 incompatible changes
  with minor+patch reset, per semver.org); subject = the deployed application as a
  whole (backward-compatibility judged against user-visible behavior, REST API, stored
  data); level selected by the PR author; source of truth = the single `version`
  declaration wired in root `build.gradle`; initialization carve-out (the first
  regime PR sets the initial value instead of bumping).
- **RED:** `grep -in "semver\|version regime\|version bump" SOP.md` shows no versioning
  regime (SOP today documents build/deploy rules only): fails SC-6 today.
- **GREEN:** the section above, placed with the other mandatory developer-stated rule
  sections.
- **Instrument:** read SOP.md: the regime section states one-bump-per-PR, the three
  level definitions, and the source-of-truth location; no superseded text remains.
- **Dependencies:** after P-1 (documents the landed declaration mechanism).
- **Mid-cycle extension (developer directive, verbatim "this needs to SOP added"):**
  SOP.md additionally gains a "Browser verification (playwright CLI)" section — the
  vendored CLI invocation (PATH-prefixed, `--browser chromium`), the persistent profile
  (`ud-bl-persist`; custom `--profile` forks an orphan profile), headed windows for
  developer login hops, DB-backed app sessions surviving server restarts, and
  backgrounded `appStart` with context-URL polling. This content traces to that
  directive (verified browser-facts of this session), extends P-5's SOP deliverable,
  and lands with the P-5 cycle on the same branch; it is not part of SC-6.

## Dependencies

P-1 → P-2 → P-3 → P-4 (strict sequence; each GREEN rebuilds on the previous verified
state). P-5 after P-1, any time thereafter (independent of P-2..P-4).

## Traceability

SC-1 ← P-1; SC-3 ← P-2; SC-4 ← P-3; SC-5 ← P-4; SC-6 ← P-5; SC-2 ← P-1 as the
regime's initialization content — this PR sets the initial version instead of bumping
(R-2's initialization clause), SC-2's quantifier explicitly excludes the initialization
PR and routes it to SC-1's post-merge state, and ongoing bump compliance is the
spec's conduct requirement continued across future PRs with the rule documented by
P-5. No item traces to nothing; no SC uncovered.

## Documentation Sources

- `.issues/15/spec.md` — requirements, approach, instruments.
- semver.org — MAJOR/MINOR/PATCH definitions (R-2 levels), 0.y.z initial-development
  convention (initialization value).
- SOP.md — wrapper-only gradle, `appStart` default, local verification stack, deploy
  script; the P-5 section joins its mandatory-rules sections.
- Existing code surfaces: `audio-quality-vote-ui/build.gradle` (war task to extend),
  `audio-quality-vote-ui/src/main/webapp/index.html` (meta target),
  `Login.java`/`Login.ui.xml`, `AboutDisplay.java`/`AboutDisplay.ui.xml` (views),
  `AppPresenter.java` (Text widget usage precedent; login-gating fact),
  `deploy-dict.sh` (war path confirmation).

## Enforcement Gate

PR gate: every SC's instrument executed with real output attached to the PR body;
the developer merges. SC-2's ongoing per-PR compliance is enforced by review against
the SOP rule (no CI exists; hook surface fixed at the trunk-protection pair).

## Cost Frame

Two full GWT builds minimum per instrument cycle (RED baseline + GREEN evidence; each
`clean build` includes the GWT compile, ~minutes on the 2-worker local setup). Browser
verification needs the MariaDB container and `appStart`; the About check additionally
needs one developer-completed login hop.

## Edge Cases

- Missing metas at runtime (e.g. dev-mode serving of un-expanded `index.html`): the
  `BuildInfo` helper reads null → empty string; the views then show no version row
  rather than fabricated values — displays describe the artifact, and an un-stamped
  artifact describes nothing.
- Same-day rebuilds share the build date; the (version, date) pair still identifies
  the deployed artifact uniquely per the displayed fields — the spec requires the date,
  not a finer timestamp, and SC-3's value-correspondence check decides equality.
- Version bump across a merge sequence: each PR's diff is measured against the trunk
  commit it landed on (SC-2 instrument's base), so merges landing after other bumps
  remain decidable.
- The stamping `filter` runs on the ui module's war packaging and inplace assembly
  independently — each is a separate copy; the final war receives the ui war's
  already-stamped file once (the overlay copies, not re-filters), so no double
  substitution can occur.

## Not Included

Everything the spec's Out of scope names: release tags/Releases/changelogs; per-module
versioning; CI; new hooks; REST version endpoint; display formatting beyond presence
of the two values; backfill of pre-regime PRs.