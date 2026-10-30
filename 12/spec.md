# Spec: Surface login failures as errors, not silent 204s (RestApiImpl.login)

Intent (developer-stated): pasted Tomcat log showing "log flow failure - nothing in logs"
after the #2 login deploy, then "fix the logging"; then the developer's correction —
"hiding and paperworking over errors is a serious policy violation". The login flow's
`return null` masking (204 to the client, login screen re-shown, nothing to grep) was the
papering-over; the deliverable must surface failures as failures AND log them loudly.

## Problem

`audio-quality-vote-server/RestApiImpl.login` returns `null` on every failure branch —
JAX-RS maps that to HTTP 204, the UI's success path receives an empty result, and all the
operator sees is silence (the branches that did log wrote `System.err`, which may be
captured in a different Tomcat file than the dated log the operator read). Failure classes
currently masked by `null`:

- empty/null `code` guard,
- unreadable/missing deployment properties,
- `github.client_secret` missing,
- GitHub code-exchange failure (`OAuthException`, or transport `IOException`/`ExecutionException`),
- exchange succeeded but `accessToken`/`getAccessToken()` null,
- GitHub identity fetch `IOException`,
- no usable verified email on the identity,
- `uid==0` after user-row insert — not even checked today; the flow continues against a
  nonexistent row (session issuance for uid 0).

Grounded against current source (RestApiImpl.java:126-218). The client side already carries
a failure path: `AppPresenter.handleOAuthReturn` calls
`api.login(code).thenAccept(...).exceptionally(...)` — an HTTP error reaches
`exceptionally`, logs to the browser console, and re-shows the login view. Nothing in the
client blocks error surfacing.

## Approach

`login` keeps returning `UserInfo` for success only. Every failure branch throws
`WebApplicationException` with a distinct status and a bounded, secret-free message, plus
the uniform `AQV login` `System.err` diagnostic line (so the reason exists in server output
regardless of the HTTP layer). Status assignment by failure class:

| Branch | Status |
|---|---|
| empty/null code (client bug) | 400 |
| properties unreadable / `github.client_secret` missing (deployment config) | 500 |
| exchange `OAuthException` (bad/stale code or credentials) | 401 |
| exchange `IOException`/`ExecutionException`/`InterruptedException` (GitHub unreachable) | 502 |
| `accessToken` null after successful exchange | 502 |
| identity fetch `IOException` | 502 |
| no usable verified email | 403 |
| `uid==0` after insert (DB row resolution failed) | 500 |

Exception messages carry the branch name and class/summary detail through a bounded prefix
helper (200 chars) — no codes, secrets, tokens, or email values. The `uid==0` branch now
fails (error) instead of continuing silently — that continuation is eliminated as part of
surfacing the error, per thedeveloper's policy correction; this is the one intentional
behavior change and it converts a silently-broken session into a 5xx.

## Requirements

- R1. Every failure branch of `RestApiImpl.login` SHALL throw `WebApplicationException`
  with the status per the branch table (none returns `null` any more), and emit one
  `System.err` line prefixed `AQV login` naming the branch before throwing.
- R2. No thrown message or log line contains OAuth codes, client secrets, tokens, or user
  email values; exception-derived detail passes through a bounded prefix helper (200 chars)
  — including the properties-path `IOException` detail.
- R3. Success behavior is untouched: a successful login returns the same `UserInfo`
  (uid/email/session), same endpoint path; the client's `thenAccept` success path is
  unchanged — errors surface through its existing `exceptionally` path with no client
  change required.
- R4. The `uid==0` silent continuation is removed: that branch throws 500 — a failed row
  resolution can no longer issue sessions against a nonexistent uid.

## Success criteria

| ID | Criterion | Class | Verification instrument |
|----|-----------|-------|-------------------------|
| SC-1 | Invalid-code login attempts surface as a server error with a diagnostic, not a 204 | Behavioral | Local stack (mariadb container + gretty appRun as in #2 live tests): POST `/api/user/login` with header code `probe-invalid-code` → HTTP 401 (not 204), server output shows `AQV login` exchange-failure line, browser-side contract exercised by the live #2 flow shape; no code/secret in the line |
| SC-2 | The null-masking paths are gone: every branch maps per the table | Behavioral | Same run: POST with empty code → 400; (structural for the rest) |
| SC-3 | No secrets/codes/emails leak into errors or logs by construction | Structural | Diff read: no `client_secret`, `code` value, token, or email value in any thrown message or `System.err` string; all raw `getMessage()` in the flow routed through the bounded helper; SC-1's runtime line double-checks |
| SC-4 | `uid==0` can no longer produce a session | Structural | Diff read: the post-insert re-query result is checked; 0 throws 500; no session issuance (`newSessionId`) precedes the check |

## Out of scope

- Client/UI changes (the `exceptionally` path already surfaces server errors);
- live-host log sink routing (operator-side); retry logic; log framework adoption;
- broader error-surface campaigns in unrelated endpoints (filed separately if found).

## Traceability
R1/R2 ← "fix the logging" + the policy correction (surfacing, not masking, is the fix);
R3 ← gap coverage (success contract unchanged); R4 ← the policy correction applied to the
worst masked branch. SC-1→R1 (exchange branch live); SC-2→R1 (branch mapping);
SC-3→R2; SC-4→R4.

Byline: 🤖 Co-authored with AI: OpenCode (huggingface/zai-org/GLM-5.3-Flash)