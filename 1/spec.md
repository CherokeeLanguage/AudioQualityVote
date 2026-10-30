# Spec: Upgrade build to the current latest stable Gradle

Intent (developer-stated): "upgrade to latest gradle."

## Problem

The build is pinned to the Gradle 7 era and rides a dead artifact repository:

- `gradle/wrapper/gradle-wrapper.properties` pins the distribution to
  `gradle-7.0-bin.zip` (read from the file). The current latest stable release is
  **Gradle 9.8.2** (published 2026-10-19; verified against the head of
  https://gradle.org/releases/ at spec-filing time — 8.14.6 is the last
  maintenance release of the 8.x line).
- `settings.gradle` pluginManagement pins the build to Gradle-7-era plugin
  releases: `com.diffplug.eclipse.apt` 3.29.1, `com.github.johnrengelman.shadow`
  6.1.0, `io.freefair.lombok` 6.0.0-m2, `org.gretty` 3.0.4, `org.wisepersist.gwt`
  1.1.11.
- Every build resolves through `jcenter()` (root `build.gradle` allprojects
  block; `audio-quality-vote-war/build.gradle`). JFrog ended JCenter service on
  2024-08-15 (read-only since 2021) —
  https://jfrog.com/blog/jcenter-sunset/ — so the pinned endpoint is a
  discontinued repository in every resolution path.

Root cause: the toolchain pins (wrapper distribution, plugin releases, jcenter)
were set in the Gradle 7 era and never revisited. The stated problem is that
staleness itself: bring the build to the current latest stable Gradle.

## Verified current state

- Six `build.gradle` files (root, plus db/server/shared/ui/war); each sets
  `compileJava.options.release = 11`, `java.toolchain.languageVersion = 11`,
  `java.toolchain.vendor = JvmVendorSpec.ADOPTOPENJDK`. The war modules produce
  `AudioQualityVote.war` (`war { archiveName }`).
- Dev environment JDKs: OpenJDK 21 and 25 in `/usr/lib/jvm` (no JDK 11 in that
  location; `compileJava.options.release = 11` pins the bytecode target
  independently of the compiling JDK). No toolchain-resolver plugin is
  configured in the build.
- No CI workflows exist (`.github/workflows` absent): no CI compatibility
  surface for this change.
- `com.github.johnrengelman.shadow` is declared in pluginManagement but applied
  by no module (checked all five module `plugins {}` blocks).

## Approach

- Bump the wrapper distribution to the current latest stable (9.8.2 at filing)
  and regenerate the wrapper files with the `wrapper` task so wrapper jar and
  scripts match the distribution.
- Raise each pin in `settings.gradle` pluginManagement to a release that loads
  and functions on the target Gradle major line — versions are whatever the
  compatible releases are at implementation time, proven by the build
  instrument (SC-2). The unused shadow pin is deleted (dead entry) rather than
  bumped.
- Remove `jcenter()` (root `allprojects` + war module); resolution rides
  `mavenCentral()` and the jitpack endpoint (the UI's
  `com.github.CherokeeLanguage.gwt-oauth2` resolves there).
- Apply only the build-script adjustments the new Gradle line requires for
  this build to run (toolchain/vendor configuration included if needed) —
  application code and application dependency versions are untouched.
- Decisions fixed here: stable releases only (no nightlies/RCs; the 9.8.x line
  is current); "latest" means the head of the stable releases list at
  implementation time. Staying on 7.x or moving to a maintenance-only 8.x line
  contradicts the stated intent.

## Requirements

- R1. The Gradle wrapper SHALL pin the current latest stable Gradle release,
  and the build SHALL run through the wrapper.
- R2. Each build plugin on which the build depends SHALL be on a release that
  loads and functions on the wrapper's Gradle version.
- R3. Build repository declarations SHALL reference live repositories only;
  the dead JCenter endpoint SHALL be removed.
- R4. The upgrade SHALL preserve build outputs: the Java 11 bytecode target
  (`compileJava.options.release = 11`) and the produced war artifact
  (`AudioQualityVote.war`) are unchanged.

## Success criteria

| ID | Criterion | Class | Verification instrument |
|----|-----------|-------|-------------------------|
| SC-1 | The wrapper pins the current latest stable Gradle release (9.8.2 at filing; a newer stable published before implementation supersedes it) | Structural | Read `gradle/wrapper/gradle-wrapper.properties` `distributionUrl`; the pinned version equals the head entry of the stable releases list at https://gradle.org/releases/ at check time |
| SC-2 | `./gradlew build` completes successfully under the wrapper-pinned Gradle and produces `AudioQualityVote.war` | Behavioral | Run `./gradlew build` (via the wrapper); capture exit code 0 and `BUILD SUCCESSFUL`; confirm the war artifact exists in the war module's build output |
| SC-3 | No dead repository remains in build scripts | Structural | Grep `*.gradle` for `jcenter` → 0 matches; dependency resolution from live repositories evidenced in the SC-2 build output |
| SC-4 | Compiled bytecode keeps the Java 11 target (unchanged behavior the upgrade puts at risk) | Structural | `javap -verbose` on a class compiled by the new build (e.g. server module) reports class-file major version 55 |

## Out of scope

- Application code changes; upgrading application dependencies (GWT 2.9.0,
  jersey, jdbi, gwt-material, …) beyond what SC-2 requires.
- CI creation (none exists), deployment or process changes, project version
  bumps (no version operation — this is a build-toolchain change, not a
  release).
- Raising the app's Java language level or bytecode target.

## Impact

- Touches: wrapper files, `settings.gradle`, all six `build.gradle` files.
- Developer environments: the wrapper self-provisions the new distribution;
  the Gradle daemon runs on a locally installed JDK (21/25 here). Compilation
  toolchain resolution is an open point this change must resolve: the current
  pin (`languageVersion 11` + `ADOPTOPENJDK` vendor) has no matching JDK on
  this machine and no resolver plugin configured, so the build needs a
  discoverable or provisioned compilation toolchain while the release-11
  bytecode output stays fixed; whether that point already resolves under the
  new Gradle line, or requires the pin to change, is proven by SC-2/SC-4.
- Risk: the era-jump of the plugin pins (gretty, wisepersist GWT plugin,
  eclipse.apt, freefair lombok) is the uncertain part; mitigated by SC-2
  evidence. No external consumers (no CI, no published build artifacts).
- Traceability: the staleness facts above require R1-R3; R4 preserves the
  app-facing behavior (bytecode target, war artifact) the change puts at risk.
  Every SC maps to its requirement (SC-1→R1, SC-2/SC-4→R1, R4, SC-2→R2, SC-3→R3).

Byline: 🤖 Co-authored with AI: OpenCode (huggingface/zai-org/GLM-5.3-Flash)