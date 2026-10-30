# Spec: Switch OAuth login from Google to GitHub

Intent (developer-stated): "switch oauth from google to github."

## Problem

Visitors log in through Google as the identity provider; the requirement is to
switch that identity provider to GitHub.

### Current state (verified in code)

**Client — GWT UI (`audio-quality-vote-ui`)**
- `src/main/java/com/cherokeelessons/audio/quality/presenter/AppPresenter.java`
  lines 101-125 (`showLogin()`): implicit-flow login via
  `io.github.freddyboucher.gwt.oauth2.client.Auth` —
  `new AuthRequest("https", "accounts.google.com", "o/oauth2/auth", Consts.CLIENT_ID)`
  with `scope=email profile`, `response_type=token id_token`; on success posts
  `result.get("id_token")` to `api.login(...)`.
- `src/main/resources/com/cherokeelessons/audio/quality/AudioQualityVote.gwt.xml`
  line 14: `<inherits name="io.github.freddyboucher.gwt.oauth2.OAuth2"/>`.
- `ui/Login.ui.xml` lines 27, 38: "Login with Google"; "A Google login is
  required to permit correct tabulation of votes."
- `build.gradle` line 77: `compileOnly
  "com.github.CherokeeLanguage.gwt-oauth2:gwt-oauth2:1.2-RC1"` (JitPack fork
  of the upstream lib; the commented line above references the upstream 1.1).

**Shared (`audio-quality-vote-shared`)**
- `shared/Consts.java`: `CLIENT_ID` — the Google OAuth client id, compiled into
  the GWT bundle (OAuth client ids are public by design; it is not a secret).

**Server (`audio-quality-vote-server`)**
- `servlet/RestApiImpl.java` lines 118-152 (`login(String idToken)`): verifies
  the Google ID token (`GoogleIdTokenVerifier`, audience `Consts.CLIENT_ID`,
  issuer `accounts.google.com`), takes subject and email, upserts the user as
  provider `"Google"` (`dao().addUser(..., "Google", oauthId, email)`,
  `dao().updateEmail(...)`), resolves the session uid by email
  (`AudioQualityVoteDao.uidByEmail`, `AudioQualityVoteDao.java:369`), issues a
  session id and trims to 5 retained sessions (lines 140-150).
- `build.gradle` lines 37-38: `com.google.api-client:google-api-client[.jackson2]:1.31.5`
  present for this verification path only (grep confirms no other module or
  class references it).

**DB (`audio-quality-vote-db`)** — already provider-generic
- users table `(uid, oauth_provider, oauth_id, email)` with provider/id indexes
  (`AudioQualityVoteDao.java:90-96`); `addUser`/`updateEmail` are
  provider-parameterized (lines 371-378). No schema change is needed.
- Server-side runtime configuration exists: a deployment properties file
  resolved by `AppPathConfig.findConfigFile` (context-named, e.g.
  `/home/{context}/{context}.properties`), read by `AudioQualityVoteDao` and
  `AudioQualityVoteFiles`.

### Target provider mechanics (verified against GitHub documentation)

- GitHub's OAuth implementation supports only the authorization-code web
  application flow for browser apps — "The implicit grant type is not
  supported" — plus the device flow for headless apps (not applicable here).
  https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps
- Web flow: browser GET `https://github.com/login/oauth/authorize` with
  `client_id` (required) and `redirect_uri`, `state`, and PKCE
  (`code_challenge`, S256) strongly recommended → user authorizes → GitHub
  redirects back with a temporary `code` (expires after 10 minutes) plus the
  `state` (state mismatch ⇒ abort).
- Token exchange: server-side POST `https://github.com/login/oauth/access_token`
  (Accept: application/json) with required `client_id`, **`client_secret`**,
  `code` → bearer access token.
- Identity: GET `https://api.github.com/user` with `Authorization: Bearer <token>`;
  the app revalidates the user's identity at every sign-in. When an account's
  email is private, the address comes from the user's email list; scope
  `user:email` "Grants read access to a user's email addresses"
  (https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/scopes-for-oauth-apps).
- Consequence: the `client_secret` must live server-side (never in the GWT
  bundle); the gwt-oauth2 implicit-flow helper (Auth/AuthRequest + the GWT
  module inherit) does not fit GitHub's flow and its dependency is dropped;
  `RestApi.login`'s parameter changes from `idToken` to the authorization
  `code`.

### Decisions fixed here

- Provider type: a GitHub **OAuth app** — the direct analog of the existing
  Google OAuth client wiring. GitHub Apps (fine-grained authorizations) are
  unnecessary for a login-only identity use; documented as the alternative and
  not taken.
- CSRF protection: the client generates an unguessable `state`, verifies it on
  return, and aborts on mismatch (GitHub's documented handling). PKCE is not
  added: the code is exchanged immediately, server-side, and its travel time
  in the browser is seconds; the state check plus the server-side exchange is
  the protective set this change ships with.
- The login request carries the returned `code` (not the access token): the
  server does exchange and identity fetch, as GitHub's documented flow
  requires.
- Server tooling (developer-stated preference: "search for github compatible
  libraries, no bespoke trash"): the exchange and identity halves ride
  established libraries — `com.github.scribejava:scribejava-core:8.3.3` +
  `com.github.scribejava:scribejava-apis:8.3.3` (the `GitHubApi` module wires
  GitHub's token endpoint, authorize URL, and token extractor; framework-free,
  MIT, Java-7-era bytecode) for the exchange, and `org.kohsuke:github-api:1.330`
  (the actively maintained dedicated GitHub client — 1.330 released 2025-09,
  MIT, Java-8 bytecode in the 1.x line; `GitHub.connectUsingOAuth(token)` →
  `getMyself()` / `listEmails()` cover `/user` and private verified emails)
  for identity. Recorded caveat: ScribeJava's release line is dormant (no
  release since 2023-01) — accepted because the two GitHub OAuth endpoints are
  stable and each half is independently replaceable. The project's deployment
  target is JDK 8 (developer-stated "this is a jdk8 project"; today's build
  files carry `release = 11` flags inconsistent with that target — the target
  correction is spec `AudioQualityVote#1`'s). Both picks are ≤ Java 8
  bytecode, verified at filing from Maven Central/repo1 poms and GitHub repo
  sources (2026-10-29/30) — the pair holds under the stated deployment floor.
- Ruled out, all verified live at filing: `com.google.oauth-client:google-oauth-client`
  1.39.0 (provider-neutral exchange exists; Google's own "maintenance mode"
  classification; heavier transitives), pac4j 6.x (JDK 17 bytecode — excluded
  by the jdk8 floor and by weight), Spring Security 7 (Java 17 + Spring
  context), Apache Oltu (retired to the Attic 2018 — dead),
  `com.nimbusds:oauth2-oidc-sdk` 11.38.2 (active but OIDC-flavored; its
  behavior against GitHub's token endpoint was not verified), and
  bytecode-downgrade machinery (JvmDowngrader / JavaDowngrader — the tools
  exist and work, but no library this change consumes is jdk8-blocked, so no
  jar-rewrite infrastructure is introduced).

## Approach

- Client: keep `Consts.CLIENT_ID` (now the GitHub client id — public), generate
  `state` client-side, redirect the browser to the GitHub authorize URL
  carrying the `user:email` scope; on return, verify `state` before
  submitting; send the `code` to `api.login(code)`.
- Server: `RestApiImpl.login(code)` exchanges the code for an access token via
  ScribeJava (`GitHubApi` + `OAuth20Service`; `client_id`, `client_secret`
  read from the deployment properties file), resolves the identity from
  `org.kohsuke:github-api` (`GitHub.connectUsingOAuth(accessToken)` →
  `getMyself()` for login/id; `listEmails()` for the primary verified email
  when the account email is private), upserts the user with provider
  `"GitHub"`, and reuses the existing session-issue logic unchanged. The
  shared `RestApi` interface signature follows (idToken → code).
- Build files: drop the gwt-oauth2 compile-only dependency and the GWT module
  inherit; remove the `google-api-client` pair from the server and add the
  scribejava-core/apis 8.3.3 pair plus `org.kohsuke:github-api:1.330` (GWT
  framework dependencies `com.google.gwt.*` are untouched).
- Wording: login button and tabulation notice updated to GitHub wording.
- Identity continuity (existing behavior, preserved and made deterministic):
  session uid resolution by email stays; new GitHub logins insert with
  provider `"GitHub"` — the users table can then hold both provider rows for
  one email (the insert dedupes on provider+oauth_id); provider-`"Google"`
  rows untouched. The email-match resolution
  (`AudioQualityVoteDao.uidByEmail`, an unordered scalar select today) is
  made deterministic — explicit stable ordering preferring the earliest
  matching row — so the migration cohort's continuation does not rest on
  engine scan order.

## Requirements

- R1. Login SHALL authenticate through GitHub's OAuth web application flow —
  the authorization request carrying the `user:email` scope R2's private-email
  resolution needs, the authorization code handed over with client-side state
  verification before submit; no implicit-flow remnants.
- R2. The server SHALL exchange the authorization code for an access token
  using the client secret read from server-side runtime configuration, SHALL
  resolve the user's identity from the GitHub API over that token (identity
  revalidated at each sign-in; the primary verified email resolved when the
  account email is private), and SHALL upsert the user with provider
  identifier `"GitHub"`.
- R3. The Google OAuth login path SHALL be removed end-to-end: the
  accounts.google.com authorization request in the client, the Google
  ID-token verification in the server, the `google-api-client` dependencies,
  the gwt-oauth2 dependency and GWT module inherit, and the Google wording on
  the login screen.
- R4. The GitHub client_secret SHALL exist only in server-side runtime
  configuration (the deployment properties file) — never in the GWT client
  bundle and never in a committed file.
- R5. Login failure SHALL be preserved: an invalid, expired, or
  state-mismatched authorization creates no user and no session, and the
  client offers login again (this matches today's behavior: verification
  failure makes `login` return null and the login view stays).
- R6. Vote attribution SHALL be unchanged for an authenticated session —
  votes attach to the session's uid as today. Identity continuation stays
  email-resolution based: a login whose verified email matches an existing
  row resolves that row's uid — deterministically (the email-match resolution
  orders explicitly and prefers the earliest matching row; it never relies on
  engine scan order). Existing provider-"Google" rows SHALL NOT be
  re-attributed: their identity fields (`oauth_provider`, `oauth_id`) and
  email remain unchanged — session bookkeeping on the resolved row
  (e.g. `updateLastLogin`) is login behavior, not re-attribution — and no
  data migration touches them; a GitHub login may then legitimately leave
  both provider rows sharing an email (the user-insert dedupes on
  provider+oauth_id), which the deterministic resolution absorbs.
- R7. The server's exchange and GitHub identity fetch SHALL ride the
  established maintained libraries named in Approach — ScribeJava 8.3.3
  (GitHubApi) and org.kohsuke:github-api 1.330 — and SHALL NOT hand-roll HTTP
  calls to the GitHub OAuth token or user endpoints.

## Success criteria

| ID | Criterion | Class | Verification instrument |
|----|-----------|-------|-------------------------|
| SC-1 | A user completing login through GitHub on a locally run deployment is authenticated: a session is issued and the users table records the login with `oauth_provider='GitHub'` | Behavioral | Prerequisite: a GitHub OAuth app exists with its dev credentials configured and its callback matching the local deployment. Run the app via the wrapper (gretty/tomcat9, contextPath AudioQualityVote), drive the web flow to completion in a browser, then read the user row: provider value `"GitHub"` and issued session observed |
| SC-2 | A vote cast under a GitHub-issued session is attributed to that user's uid (unchanged counting behavior) | Behavioral | Using the SC-1 session, perform one vote through the voting flow; verify the vote record attaches to that uid (DB read or API result) |
| SC-3 | Failed authorization creates nothing — including through the state gate | Behavioral | (a) Required gate-distinguishing variant: submit a valid authorization code with a mismatched `state` through the client path (an implementation lacking the state gate would exchange that code and create the rows) → login fails and no user or session row is created (users table read before/after); (b) additionally drive a declined flow and a bad code: login fails, no rows; in the driven browser session at flow end, the login view is presented again — no dead-end state |
| SC-4 | No active Google OAuth surface remains in the code | Structural | Grep the repo: `accounts.google.com` in client login code, `GoogleIdToken`, `google-api-client` in build scripts, non-comment `gwt-oauth2` references, the OAuth2 GWT `inherits`, and the Google login wording (`Login with Google`, `A Google login`) → 0 matches; `Login.ui.xml`'s login button and tabulation-notice copy each reference GitHub and carry no Google reference (read of the two elements); GWT framework `com.google.gwt.*` references untouched |
| SC-5 | The client secret is confined to server-side runtime configuration | Structural | `detect-secrets` scan (repo wrapper) reports no committed secret; `client_secret`/`GITHUB_CLIENT_SECRET` appears only in server-module code reading the properties-file config; shared/ui sources contain no secret value |
| SC-6 | The server's exchange and identity ride the named libraries — no bespoke HTTP plumbing for those steps | Structural | Server `build.gradle` declares `com.github.scribejava:scribejava-core/-apis:8.3.3` and `org.kohsuke:github-api:1.330`; grep server sources for `github.com/login/oauth` and `api.github.com` literals → 0 matches (the endpoint URLs live inside the libraries; app code calls the library APIs) |
| SC-7 | A GitHub login whose verified email matches a pre-existing provider-"Google" row resolves that pre-existing row's uid, deterministically — same uid on repeated logins, vote history attaching through it | Behavioral | Local dev database seeded with a provider-"Google" row whose email equals the test account's GitHub verified email; run the SC-1 web flow; assert the resolved uid is the seeded row's uid; repeat the login and assert the same uid; after both logins assert the seeded row's `oauth_provider` still reads "Google" and its `oauth_id`/email fields unchanged (re-attribution probe) |
| SC-8 | A login whose test account's primary email is private records the account's primary verified email — not null and not a noreply alias — via the email-list route | Behavioral | Fixture GitHub account with its primary email set private (email-privacy toggle); confirm the authorize request carries the `user:email` scope (client code read); run the SC-1 web flow; read the recorded email and assert it equals the fixture account's primary verified address |

## Out of scope

- Migrating provider-"Google" user rows (R6 keeps them unmodified).
- Registering the production GitHub OAuth app and configuring its deployment
  properties (operator step; the PR documents it). The dev-app prerequisite
  for SC-1 is created at implementation time if absent.
- Any non-login surface — votes, audio handling, sessions, CSV export.

## Impact

- Shared API: `RestApi.login` signature (idToken → code) — client and server
  change together in this one change.
- Users rows: new logins carry `oauth_provider='GitHub'`; continuation is by
  verified email — a returning voter with the same verified email keeps their
  uid and vote history; a different verified email resolves as a new
  identity. That is today's existing resolution behavior (uidByEmail), not
  something this change adds. For the migration cohort (existing
  provider-"Google" rows), a same-email GitHub login leaves two provider rows
  sharing an email; the deterministic earliest-row resolution (R6/SC-7) keeps
  the pre-existing uid stable across logins.
- Deployment: needs a GitHub OAuth app (callback URL = deployment origin) and
  the client_secret in the deployment properties file; the Google app's
  configuration becomes unused.
- Login-screen copy: both strings change.
- Risk: moderate — the login path is rewritten; SC-2/SC-3 preservation
  criteria bound the blast radius to the login path.
- Traceability: R1-R3 implement the stated switch; R4 follows from GitHub's
  required server-side exchange and the no-committed-secrets floor; R5
  preserves the failure path (gap coverage); R6 preserves the stated purpose
  of login — "correct tabulation of votes" — with the mixed-provider cohort's
  continuation made deterministic; R7 is the developer-stated library
  requirement ("no bespoke trash": named pair on the jdk8 floor). Every SC
  maps to its requirement (SC-1→R1,R2; SC-2→R6; SC-3→R1,R5; SC-4→R3; SC-5→R4;
  SC-6→R7; SC-7→R6; SC-8→R1,R2).

Byline: 🤖 Co-authored with AI: OpenCode (huggingface/zai-org/GLM-5.3-Flash)