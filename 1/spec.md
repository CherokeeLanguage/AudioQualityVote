# Spec: Upgrade build to the current latest stable Gradle

Intent (developer-stated): "upgrade to latest gradle" — extended by the
follow-up (developer-stated): "adjust to also purge all Eclipse configs and
files and folders as part of the Gradle update cleanup."

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
- Eclipse IDE integration spans the build: `eclipse-wtp` and
  `com.diffplug.eclipse.apt` are applied in all six build files, the diffplug
  pin (3.29.1) sits in `settings.gradle`, `eclipseJdt`/`eclipseJdtApt`/
  `eclipseFactorypath` task-wiring blocks exist in the five module files, and
  `eclipse {}` config blocks in all six (root project name, downloadSources,
  the ui `addGwtContainer`). On disk, git-ignored generated metadata exists in
  every module + root: `.project`, `.classpath`, `.factorypath`, `.settings/`
  (regenerable IDE files; nothing Eclipse-related is tracked — verified via
  `git ls-files`). The ui module additionally reads two eclipse/apt-provided
  source dirs into the GWT compiler wiring — `project.eclipse.jdt.apt.genSrcDir`
  (`superDev.src`) and `compileJava.options.annotationProcessorGeneratedSourcesDirectory`
  (`compiler.src`) — the dagger-APT generated client sources feeding
  `GwtCompile`; removing the plugin integration orphans those reads.
  Git-ignored Eclipse-JDT-APT output folders `.apt_generated`
  (+ `.apt_generated_tests`) exist in root + all five modules — configured by
  `.settings/org.eclipse.jdt.apt.core.prefs` (`genSrcDir=.apt_generated`) —
  with `ui/.apt_generated` holding the dagger-generated client sources
  (verified in the tree); the purged `.classpath` files designate `bin/`
  output folders.
- War composition consumes the GWT compile output: the produced wars carry
  the GWT-compiled webapp content (`gwt/*.cache.js`, `gwt/gwt.nocache.js`,
  public assets — e.g. materialize CSS arrives via the compile output, not
  `ui/src/main/webapp`); the war module's gretty block hard-codes the feed
  path `extraResourceBase '../audio-quality-vote-ui/build/gwt/out'`
  (`audio-quality-vote-war/build.gradle:28`), while the successor plugin's
  compiler output defaults to `build/gwt` (docstr v2 configuration docs) with
  no `/out` subdir.
- `org.wisepersist.gwt` is terminal: Central's listing caps at 1.1.19
  (2022-07-21; maven-metadata same day) and the build pins the older 1.1.11;
  its `GwtBasePlugin` builds on the convention-access APIs
  (`Project.getConvention()`/`JavaPluginConvention`) that the Gradle 9 line
  rejects as errors (source read at tag 1.1.19; Gradle's documented 9.0
  upgrade behavior) — no release of that plugin id loads on the pinned major.
  The maintained continuation is the `org.docstr.gwt` fork (Gradle plugin
  portal: latest 2.2.9, created 2026-01-31; repo `jiakuan/gwt-gradle-plugin`).
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
- Raise the surviving applied pin (`org.gretty`) to a release that loads and
  functions on the target Gradle major — proven by the build instrument
  (SC-2). The dead entries (`shadow`, `lombok`) are deleted rather than
  bumped, and `com.diffplug.eclipse.apt` is removed entirely by the R5 purge:
  no build execution exercises those three.
- Succeed the terminated GWT plugin id: adopt the maintained continuation
  `org.docstr.gwt` for `org.wisepersist.gwt`. The successor is a documented
  complete rewrite (portal latest 2.2.9, created 2026-01-31) supporting
  Gradle ≥ 8.1 and GWT 2.12+, with `extraSourceDirs` first-class for
  annotation-processor/generated sources. The `gwt {}` configuration re-maps
  onto the successor's v2 DSL (`gwtVersion`, `modules`, `compiler`/
  `superDev` options, and the superDev source inputs; `extraSourceDirs`
  carries the APT/generated-source re-point). The output side re-maps too:
  the successor's compiler output defaults to `build/gwt` (no `/out`), so the
  war composition's hard-coded feed
  (`extraResourceBase '../audio-quality-vote-ui/build/gwt/out'`) is re-pointed
  — or the successor's output location configured to a stable shared
  location; the choice lands at implementation and is proven by the
  war-content evidence in SC-2. If the functioning
  configuration requires a GWT version beyond 2.9.0, that rides the
  Out-of-scope's SC-2-requires carve-out; otherwise 2.9.0 stays.
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
- Purge the Eclipse IDE integration end-to-end: remove the `eclipse-wtp` and
  `com.diffplug.eclipse.apt` plugin applications (all six build files), the
  diffplug pin in `settings.gradle`, the `eclipseJdt`/`eclipseJdtApt`/
  `eclipseFactorypath` task-wiring blocks and `eclipse {}` config blocks, and
  the ui-side `project.eclipse.jdt.apt.genSrcDir` / `addGwtContainer` reads;
  delete the generated metadata and APT output (`.project`, `.classpath`,
  `.factorypath`, `.settings/`, `.apt_generated`, `.apt_generated_tests`) in
  every module and at root, plus the `bin/` output folders the `.classpath`
  entries designate. Re-point the GWT compiler's annotation-processor source
  input at Gradle's own generated-sources output through the successor
  plugin's `extraSourceDirs` (both `compiler.src` and `superDev.src`) so the
  dagger-APT handoff keeps flowing.
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
- R5. The build SHALL be purged of the Eclipse IDE integration: no
  `eclipse-wtp` or `com.diffplug.eclipse.apt` plugin applications, no
  `eclipseJdt`/`eclipseJdtApt`/`eclipseFactorypath` task-wiring or `eclipse {}`
  config blocks in any build file, and no Eclipse metadata or generated-APT
  surfaces present in the repo tree — `.project`, `.classpath`,
  `.factorypath`, `.settings/`, `.apt_generated`, `.apt_generated_tests`, and
  the `bin/` output folders the purged `.classpath` entries designate; the
  dagger-annotation-processor → GWT-compiler source handoff SHALL keep
  functioning after the purge.

## Success criteria

| ID | Criterion | Class | Verification instrument |
|----|-----------|-------|-------------------------|
| SC-1 | The wrapper pins the current latest stable Gradle release (9.8.2 at filing; a newer stable published before implementation supersedes it) | Structural | Read `gradle/wrapper/gradle-wrapper.properties` `distributionUrl`; the pinned version equals the head entry of the stable releases list at https://gradle.org/releases/ at check time |
| SC-2 | `./gradlew build` completes successfully under the wrapper-pinned Gradle and produces the same war artifacts as today — same names and same content: `AudioQualityVote.war` from the server and war modules, the ui module's default-named war overlay, each carrying the GWT-compiled webapp content | Behavioral | Run `./gradlew build` (via the wrapper); capture exit code 0 and `BUILD SUCCESSFUL`; confirm in the build output the three artifact surfaces (`AudioQualityVote.war` in the war and server modules' output, the default-named war in the ui module's output); confirm by war listing each contains the GWT-compiled entries (e.g. `gwt/`-prefixed `*.cache.js` bundles and public assets) — war composition, not names alone |
| SC-3 | No dead repository remains in build scripts | Structural | Grep `*.gradle` for `jcenter` → 0 matches; dependency resolution from live repositories evidenced in the SC-2 build output |
| SC-4 | Compiled bytecode runs on the project's JDK 8 floor (today's release-11 flags cannot load on JDK 8) | Structural | `javap -verbose` on a class compiled by the new build (e.g. server module) reports class-file major version 52 |
| SC-5 | No Eclipse IDE integration remains in build files or the tree | Structural | Grep the six `build.gradle` + `settings.gradle` for `eclipse-wtp`, `com.diffplug.eclipse.apt`, `eclipseJdt`, `eclipseFactorypath`, and `eclipse {` config blocks → 0 matches; search the tree (excluding `.issues`) for `.project`, `.classpath`, `.factorypath`, `.settings`, `.apt_generated`, `.apt_generated_tests` (any depth) → no entries present; and no `bin/` directory exists as a direct child of root or any of the five module directories (the locations the purged `.classpath` entries designate) — `bin/` uses outside the modules (e.g. agent tooling trees) are out of the probe |
| SC-6 | The dagger-annotation-processor → GWT-compiler source handoff keeps functioning after the purge, and the compile output lands where the war composition consumes it | Behavioral | Run the GWT plugin's compile task via the wrapper (task name read from the adopted plugin's task catalog); exit 0 — the compile consumes the dagger-generated client sources through the re-pointed source input (successor-DSL `extraSourceDirs`), and the compile's output directory is the location the war composition consumes (no stale producer-less `build/gwt/out` feed path remains) |

## Out of scope

- Application code changes; upgrading application dependencies (GWT 2.9.0,
  jersey, jdbi, gwt-material, …) beyond what SC-2 requires.
- CI creation (none exists), deployment or process changes, project version
  bumps (no version operation — this is a build-toolchain change, not a
  release).
- Any change to the app's Java language level beyond the JDK 8 bytecode-target
  correction this spec requires (the release-11 → release-8 flag correction
  is the only app-facing compile-target change).
- Non-Eclipse IDE integrations (the `.idea` folder is IntelliJ's — out of this
  purge), and `.gitignore` hygiene lines (the now-dead
  `.classpath`/`.project`/`.settings/`/`.factorypath` ignore entries remain —
  harmless hygiene, not configuration).

## Impact

- Touches: wrapper files, `settings.gradle`, all six `build.gradle` files, and
  the git-ignored Eclipse metadata + APT-output deletions (`.project`,
  `.classpath`, `.factorypath`, `.settings/`, `.apt_generated`,
  `.apt_generated_tests`, `bin/`) in root + the five modules — untracked,
  regenerable IDE files.
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
- Eclipse purge (developer-stated follow-up): the Eclipse IDE workflow
  (workspace project import, JDT/FactoryPath generation, wtp integration) is
  removed by intent; consumers outside the repo: none verified (no CI, no
  published artifacts). The uncertain parts: the ui module's GWT-compiler
  source re-wiring after the diffplug/apt reads vanish, and the
  war-composition feed path (`extraResourceBase`) following the successor's
  output-location change (R5/SC-6 + SC-2 evidence).
- Risk: the plugin-era moves are the uncertain part — the gretty release bump,
  the `org.wisepersist.gwt` → `org.docstr.gwt` id succession with its v2-DSL
  re-map (and the GWT-version knock-on only if the functioning configuration
  requires it), all mitigated by SC-2/SC-6 evidence. No external consumers
  (no CI, no published build artifacts).
- Traceability: the staleness facts above require R1-R3; R4 restores the
  app-facing bytecode floor (JDK 8, major 52 — developer-stated project
  target) that the stale release-11 flags violated, plus the war artifacts;
  R5 is the developer-stated follow-up (Eclipse purge), covering the plugin
  surfaces and the GWT handoff its removal endangers. Every SC maps to its
  requirement (SC-1→R1; SC-2→R1,R2,R4; SC-3→R3; SC-4→R4; SC-5→R5; SC-6→R5,R2).

Byline: 🤖 Co-authored with AI: OpenCode (huggingface/zai-org/GLM-5.3-Flash)