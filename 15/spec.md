# Spec: Semver-per-PR versioning — version and build date shown on Login and About pages

Intent (developer-stated, verbatim): "we want to start using semver for all PR's. We
want the login page and the About page to both show the version and build date." Regime
clarification: "this is an evergreen project. each PR needs to have a version bump."

## Problem

The project has no version surface anywhere. Observed at spec time (2026-10-30):

- The root `build.gradle` declares no `version`; every Gradle subproject resolves to
  Gradle's `unspecified` default (grep across all six build files: only dependency
  coordinates carry versions).
- The repository carries no git tags (`git tag -l` is empty) and SOP.md documents no
  versioning or release process.
- Neither the Login view (`audio-quality-vote-ui/.../ui/Login.ui.xml`) nor the About
  view (`audio-quality-vote-ui/.../ui/AboutDisplay.ui.xml`) displays version or build
  information; no build-stamping mechanism exists in any build file.

Consequences: deployed builds are unidentifiable — nobody can tell which code the war
running on prod corresponds to (deploys publish `AudioQualityVote.war` atomically over
the previous war, per SOP.md); and the requested semver-per-PR regime has nothing to
increment.

"Evergreen" here means the project evolves continuously through merged PRs with no
release train — version identity advances with every merge, not with cut releases.

## Approach

1. **Version source of truth — root `build.gradle` `version`.** Gradle's native
   multi-module property: all five subprojects inherit it; one declaration, one bump
   location per PR.
2. **Stamp at packaging time.** A build stamps its own version and UTC build date into
   the artifact it produces — `<meta name="aqv-version">` / `<meta name="aqv-build-date">`
   on the host page (`index.html`) of the produced `AudioQualityVote.war`. Stamping at
   packaging (not source-checkin) is what makes the display describe the *deployed*
   artifact: merged-PR versions run ahead of deployments (deploys happen only via
   `deploy-dict.sh`), so a page reporting repo-HEAD values would misstate the running
   build. The host page is the metadata transport because it ships with the artifact,
   needs no RPC round-trip, and the GWT client already reads the DOM (Elemental2
   `DomGlobal` is on the classpath and used in `AppPresenter`). Alternatives weighed: a
   REST endpoint adds a network dependency for display-only data; build-time generated
   Java constants freeze values into the compiled GWT bundle — same effect, more
   machinery.
3. **Client render.** The GWT client reads the two metadata values once and displays
   them on the Login view and the About view (both UiBinder widgets under
   `audio-quality-vote-ui`).
4. **Codify the regime in SOP.md** — this repo's document for developer-mandated
   process rules.

The initial version value is set by the first PR implementing this spec; this spec
fixes no number (no developer statement pins one). From that PR onward, trunk always
carries a valid SemVer version.

## Requirements

- R-1. The application version's single source of truth SHALL be the `version` property
  in the root `build.gradle`, inherited by all five subprojects. Once this change lands,
  the property's value on trunk SHALL be a valid SemVer string (MAJOR.MINOR.PATCH per
  https://semver.org/) — never Gradle's `unspecified` default.
- R-2. Every PR SHALL carry exactly one version bump of that property in its own diff,
  such that the version on trunk after a merge equals the pre-merge trunk version
  incremented once: PATCH+1 for backward-compatible defect fixes, MINOR+1 (patch reset)
  for backward-compatible new functionality, MAJOR+1 (minor and patch reset) for
  incompatible changes. The version subject is the deployed application as a whole;
  "backward-compatible" is judged against its user-visible behavior, its REST API, and
  stored data. Level selection follows SemVer's own definitions
  (https://semver.org/), applied by the PR author; the first post-regime PR sets the
  initial version (any valid SemVer value).
- R-3. A produced build artifact (`AudioQualityVote.war`) SHALL carry that build's
  version and UTC build date as host-page metadata (`aqv-version`, `aqv-build-date`)
  stamped at packaging time; the stamped values SHALL equal that build's `version`
  property value and that build's UTC date.
- R-4. The Login view SHALL display the deployed artifact's `aqv-version` and
  `aqv-build-date` values.
- R-5. The About view SHALL display the deployed artifact's `aqv-version` and
  `aqv-build-date` values.
- R-6. SOP.md SHALL document the versioning regime: one bump per PR, the three level
  definitions, and the source-of-truth location.

## Success criteria

| ID | Criterion | Class | Verification instrument |
|----|-----------|-------|-------------------------|
| SC-1 | Root `build.gradle` carries a `version` property whose value is a valid SemVer string | Structural | Read the root `build.gradle` on trunk: the `version` property exists, its value matches the SemVer grammar (MAJOR.MINOR.PATCH), and it is not `unspecified` |
| SC-2 | Every post-regime merged PR bumps the version exactly one increment, level SemVer-consistent | Structural (conduct requirement — continuation evidence) | Sample any merged post-regime PR and diff it against the trunk commit it landed on: the diff modifies the `version` value exactly once, from base B to B+1 increment (patch+1; minor+1 with patch reset; major+1 with minor+patch reset), and the level is consistent with the PR's content per SemVer definitions. Verified across post-regime PRs; repeatable per-PR because SOP.md carries the rule |
| SC-3 | A freshly produced war self-describes with its build's version and UTC build date | Behavioral | Wrapper build `./gradlew :audio-quality-vote-war:clean build` (project-root wrapper per SOP): extract the war's `index.html` — `aqv-version` equals the build's `version` value and `aqv-build-date` equals the build's UTC date; then bump `version` and rebuild — the second war carries the new value (values are build-derived, never hardcoded) |
| SC-4 | The Login page displays the deployed build's version and build date | Behavioral | SOP local stack (MariaDB container + `./gradlew :audio-quality-vote-war:appStart`): browser automation loads `http://localhost:8080/AudioQualityVote/` and observes the rendered login view containing the built artifact's `aqv-version` and `aqv-build-date` values |
| SC-5 | The About page displays the deployed build's version and build date | Behavioral | Same stack; the About view is login-gated by design (`api.loggedIn()`), so the instrument uses a browser session with a completed GitHub login, then observes the About view (first view shown post-login) displaying the artifact's `aqv-version` and `aqv-build-date` values |
| SC-6 | SOP.md documents the regime | Structural | Read SOP.md: it states one-bump-per-PR, the three level definitions, and the root `build.gradle` `version` source |

## Out of scope

- Release machinery — git tags, GitHub Releases, changelogs. The evergreen regime
  derives version identity from per-PR bumps; no tag or release process is stated or
  required.
- Per-module independent versioning — the single root property covers all subprojects.
- CI creation (none exists — the #1 toolchain spec scope-excluded CI); SC-2's
  verification is per-PR diff inspection supported by the documented rule, not a CI
  gate.
- New git hooks — the repo's hook surface is fixed at the trunk-protection pair
  (.opencode#2490 SC-6: exactly one hook survives).
- Backfill — PRs merged before this regime and history rewriting are untouched; the
  regime starts with the first post-regime PR.
- A REST endpoint exposing version/build info.
- Display formatting specifics (exact strings/styling) beyond requiring the two values
  present — plan-level.

## Traceability

R-1/R-2 ← "start using semver for all PR's" + "this is an evergreen project. each PR
needs to have a version bump": the bump must live somewhere incrementable (R-1's
property is that place), and R-2 is the stated rule itself. R-3 ← necessary mechanism
for R-4/R-5: a deployed page can only show what its artifact carries, and merged
versions run ahead of deploys — artifact self-description via packaging-time stamping.
R-4 ← "the login page ... show the version and build date". R-5 ← "the About page ...".
R-6 ← codification of the stated convention in the repo's process document (SOP.md
carries the developer's mandatory process rules); flagged for developer confirmation at
the approval gate. SC-1→R-1; SC-2→R-2; SC-3→R-3; SC-4→R-4; SC-5→R-5; SC-6→R-6.

Byline: 🤖 Co-authored with AI: OpenCode (huggingface/zai-org/GLM-5.3-Flash)