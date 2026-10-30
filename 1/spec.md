# Spec: Upgrade build to the current latest stable Gradle

Intent (developer-stated): "upgrade to latest gradle."

## Problem

The build is pinned to the Gradle 7 era and rides a dead artifact repository:

- `gradle/wrapper/gradle-wrapper.properties` pins the distribution to
  `gradle-7.0-bin.zip` (read from the file). The current latest stable release is
  **Gradle 9.8.2** (published 2026-10-19; verified against the head of
  https://gradle.org/releases/ at spec-filing time — 8.14.6 is the last
  maintenance release of the 8.x line).
- `settings.gradle` pluginManagement pins the applied plugins to Gradle-7-era
  releases — `com.diffplug.eclipse.apt` 3.29.1, `org.gretty` 3.0.4,
  `org.wisepersist.gwt` 1.1.11 — and additionally declares two dead entries
  applied by no build: `com.github.johnrengelman.shadow` 6.1.0 and
  `io.freefair.lombok` 6.0.0-m2.
- Every build resolves through `jcenter()` (root `build.gradle` allprojects
  block; `audio-quality-vote-war/build.gradle`). JFrog ended JCenter service on
  2024-08-15 (read-only since 2021) —
  https://jfrog.com/blog/jcenter-sunset/ — so the pinned endpoint is a
  discontinued repository in every resolution path.

Root cause: the toolchain pins (wrapper distribution, plugin releases, jcenter)
were set in the Gradle 7 era and never revisited. The stated problem is that
staleness itself: bring the build to the current latest stable Gradle.
Discovered during this filing (developer-stated): the project is a JDK 8
project — the app deploys to a JDK 8 runtime — and the checked-in
`compileJava.options.release = 11` flags are inconsistent with that target:
class-file major 55 cannot load on JDK 8. The target correction is in scope.

## Verified current state

- Six `build.gradle` files (root, plus db/server/shared/ui/war); each sets
  `compileJava.options.release = 11`, `java.toolchain.languageVersion = 11`,
  `java.toolchain.vendor = JvmVendorSpec.ADOPTOPENJDK`. Three modules apply
  `war`; `audio-quality-vote-server` and `audio-quality-vote-war` name their
  output `AudioQualityVote.war` (`war { archiveName }`), while `audio-quality-vote-ui`
  sets no archive name (its war keeps the default project-based name and is
  consumed as a gretty overlay by the war module — `overlay ':audio-quality-vote-ui'`
  in its gretty block).
- The project is a JDK 8 project (developer-stated deployment target). Today's
  `compileJava.options.release = 11` in all six build files produces
  class-file major 55 — bytecode that cannot load on a JDK 8 runtime; the
  flags predate this change and are inconsistent with the project's target.
- Dev environment JDKs: OpenJDK 21 and 25 in `/usr/lib/jvm` (no JDK 11 in that
  location; `compileJava.options.release = 11` pins the bytecode target
  independently of the compiling JDK). No toolchain-resolver plugin is
  configured in the build.
- No CI workflows exist (`.github/workflows` absent): no CI compatibility
  surface for this change.
- `com.github.johnrengelman.shadow` and `io.freefair.lombok` are declared in
  pluginManagement but applied by no `plugins {}` block (checked the root and
  all five module blocks; repo-wide, the only other mentions of either artifact
  are this spec's own references).

## Approach

- Bump the wrapper distribution to the current latest stable (9.8.2 at filing)
  and regenerate the wrapper files with the `wrapper` task so wrapper jar and
  scripts match the distribution.
- Raise the applied pins in `settings.gradle` pluginManagement (eclipse.apt,
  gretty, wisepersist.gwt) to releases that load and function on the target
  Gradle major line — versions are whatever the compatible releases are at
  implementation time, proven by the build instrument (SC-2). The dead
  entries (`shadow`, `lombok`) are deleted rather than bumped: no build
  execution exercises them.
- Remove `jcenter()` (root `allprojects` + war module); resolution rides
  `mavenCentral()` and the jitpack endpoint (the UI's
  `com.github.CherokeeLanguage.gwt-oauth2` resolves there).
- Apply only the build-script adjustments the new Gradle line requires for
  this build to run (toolchain/vendor configuration included if needed) —
  application code and application dependency versions are untouched.
- Correct the compile target to the project's JDK 8 floor: replace the
  pre-existing `compileJava.options.release = 11` with `release = 8` in all
  six build files (javac `--release 8` from the local JDKs 21/25 emits
  class-file major 52; the release flag decouples bytecode from the compiling
  JDK).
- Decisions fixed here: stable releases only (no nightlies/RCs; the 9.8.x line
  is current); "latest" means the head of the stable releases list at
  implementation time. Staying on 7.x or moving to a maintenance-only 8.x line
  contradicts the stated intent.
- The compile target is the project's JDK 8 floor (developer-stated "this is
  a jdk8 project"): the pre-existing `release = 11` flags are corrected to
  `release = 8` by this change. Bytecode-downgrade tooling exists (verified
  live: JvmDowngrader, JavaDowngrader) but is neither needed (no library this
  change consumes is jdk8-blocked) nor used — jar-rewrite machinery is bespoke
  permanent infrastructure.

## Requirements

- R1. The Gradle wrapper SHALL pin the current latest stable Gradle release,
  and the build SHALL run through the wrapper.
- R2. Each build plugin on which the build depends SHALL be on a release that
  loads and functions on the wrapper's Gradle version.
- R3. Build repository declarations SHALL reference live repositories only;
  the dead JCenter endpoint SHALL be removed.
- R4. The upgrade SHALL land the build on the project's JDK 8 floor and
  preserve build outputs: compiled bytecode runs on a JDK 8 runtime
  (`compileJava.options.release = 8` replacing the inconsistent pre-existing
  `release = 11` flags), and the produced war artifacts are unchanged —
  `AudioQualityVote.war` from the server and war modules; the ui module's
  default-named war overlay as it stands today.

## Success criteria

| ID | Criterion | Class | Verification instrument |
|----|-----------|-------|-------------------------|
| SC-1 | The wrapper pins the current latest stable Gradle release (9.8.2 at filing; a newer stable published before implementation supersedes it) | Structural | Read `gradle/wrapper/gradle-wrapper.properties` `distributionUrl`; the pinned version equals the head entry of the stable releases list at https://gradle.org/releases/ at check time |
| SC-2 | `./gradlew build` completes successfully under the wrapper-pinned Gradle and produces the same war artifacts as today: `AudioQualityVote.war` from the server and war modules, the ui module's default-named war overlay | Behavioral | Run `./gradlew build` (via the wrapper); capture exit code 0 and `BUILD SUCCESSFUL`; confirm in the build output the three artifact surfaces: `AudioQualityVote.war` in the war module's and the server module's build output, and the default-named war artifact in the ui module's build output |
| SC-3 | No dead repository remains in build scripts | Structural | Grep `*.gradle` for `jcenter` → 0 matches; dependency resolution from live repositories evidenced in the SC-2 build output |
| SC-4 | Compiled bytecode runs on the project's JDK 8 floor (today's release-11 flags cannot load on JDK 8) | Structural | `javap -verbose` on a class compiled by the new build (e.g. server module) reports class-file major version 52 |

## Out of scope

- Application code changes; upgrading application dependencies (GWT 2.9.0,
  jersey, jdbi, gwt-material, …) beyond what SC-2 requires.
- CI creation (none exists), deployment or process changes, project version
  bumps (no version operation — this is a build-toolchain change, not a
  release).
- Any change to the app's Java language level beyond the JDK 8 bytecode-target
  correction this spec requires (the release-11 → release-8 flag correction
  is the only app-facing compile-target change).

## Impact

- Touches: wrapper files, `settings.gradle`, all six `build.gradle` files.
- Developer environments: the wrapper self-provisions the new distribution;
  the Gradle daemon runs on a locally installed JDK (21/25 here). Compilation
  toolchain resolution is an open point this change must resolve: the current
  pin (`languageVersion 11` + `ADOPTOPENJDK` vendor) has no matching JDK on
  this machine and no resolver plugin configured, so the build needs a
  discoverable or provisioned compilation toolchain while the JDK 8 bytecode
  output (major 52) stays fixed; whether that point already resolves under the
  new Gradle line, or requires the pin to change, is proven by SC-2/SC-4.
- The one app-facing change this spec makes: produced classes load on the
  project's JDK 8 deployment container (today's v55 bytecode would not) — a
  deliberate correction, developer-stated, carried by R4/SC-4.
- Risk: the era-jump of the plugin pins (gretty, wisepersist GWT plugin,
  eclipse.apt, freefair lombok) is the uncertain part; mitigated by SC-2
  evidence. No external consumers (no CI, no published build artifacts).
- Traceability: the staleness facts above require R1-R3; R4 restores the
  app-facing bytecode floor (JDK 8, major 52 — developer-stated project
  target) that the stale release-11 flags violated, plus the war artifacts.
  Every SC maps to its requirement (SC-1→R1, SC-2/SC-4→R1, R4, SC-2→R2, SC-3→R3).

Byline: 🤖 Co-authored with AI: OpenCode (huggingface/zai-org/GLM-5.3-Flash)