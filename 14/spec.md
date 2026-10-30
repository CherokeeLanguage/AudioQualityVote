# Spec: Scrub code/state/iss from the OAuth callback URL after exchange

Intent (developer-stated, verbatim): "why does the app leave the ?code= in the URL after
redirect?" — a diagnosed residual of the #2 client flow — followed by the directive
"file a fix spec and implement for PR".

## Problem

`AppPresenter.handleOAuthReturn` consumes the OAuth `state` (sessionStorage, CSRF gate)
but never rewrites the browser URL: the page keeps carrying
`?code=…&state=…&iss=…` for the rest of the session, leaving the one-time authorization
code in the address bar, browser history, and any bookmarked/copy-pasted/shared URL.
Replays are already neutralized (state consumed, single-use GitHub codes now surface as a
401), so this is URL hygiene — credential-ish data must not linger in the address bar.

Grounded: `audio-quality-vote-ui/src/main/java/com/cherokeelessons/audio/quality/presenter/AppPresenter.java`
(`showLogin` reads code/state from `Window.Location.getParameterMap()`;
`handleOAuthReturn` removes only the sessionStorage item; no history/URL write exists
anywhere in the flow).

## Approach

Inside the state-matched branch of `handleOAuthReturn`, after consuming the state item,
rewrite the browser URL with a single history replace-state to the clean callback base —
host-page base URL with no query parameters — before the login RPC runs. One rewrite for
both outcomes (success and failure), since both stem from the same consumed return;
the state-mismatch path (CSRF abort) does NOT rewrite, leaving evidence intact for
troubleshooting and for reload-based aborts. Failure of exchange still lands on the login
view; only the URL is clean.

## Requirements

- R1. After a state-matched OAuth return, the browser URL SHALL be rewritten (history
  replace-state, no reload) to the host-page base with no `code`/`state`/`iss` query
  parameters, before the login RPC — the same single rewrite applies when the exchange
  succeeds and when it fails.
- R2. Login behavior is unchanged: state consumption, RPC, success (`showMain`) and
  failure (`displayLoginView`) outcomes, 400/401/403/500/502 error surfacing from `#12` —
  the diff's only behavioral addition is the URL write; the state-mismatch branch is
  untouched (no rewrite there).
- R3. No new dependencies: the rewrite uses the already-present Elemental2
  `DomGlobal.window.history` surface (no add-to-classpath).

## Success criteria

| ID | Criterion | Class | Verification instrument |
|----|-----------|-------|-------------------------|
| SC-1 | After a state-matched return the app never re-URLs the one-time code | Behavioral | Local-stack flow probe (mariadb container + wrapper `./gradlew :audio-quality-vote-war:appStart` — the #2/#12 stack): drive the client's `handleOAuthReturn` through a session with a matched state by loading the callback URL `http://localhost:8080/AudioQualityVote/?code=probe-code&state=<storage-seeded>`, then read back the address bar via the browser console probe (`window.location.href`) — the URL shows the clean base with no `code`/`state`/`iss`; the login RPC fires (server outputs the `AQV login` 401 line from the invalid probe code, proving the exchange ran post-rewrite) |
| SC-2 | Rewrite is replace-state, not a reload, and precedes both outcomes | Structural | Diff read: the rewrite uses `history.replaceState` (no `Location.reload`/`assign`/`replace` call), executes once, before the `api.login` call — the single placement guarantees the success path (`showMain`) is served the clean URL too |
| SC-3 | Failed-exchange path leaves no URL or storage residue | Behavioral | Same probe run: after the failed exchange, `window.location.href` still carries no `code`/`state`/`iss`, and the consumed state key in sessionStorage reads `null` via the console probe |
| SC-4 | The state-mismatch branch does not rewrite | Structural | Diff read: the `displayLoginView()` CSRF-abort path contains no history/URL write |

## Out of scope

- Server-side changes; the CSRF state machinery; the 4xx/5xx mapping from #12; redirect_uri
  values; production callback configuration (operator surface).

## Traceability
R1/R2 ← the developer's question + "file a fix spec and implement for PR" (the defect is
the lingering code in the URL; the fix is hygiene, not auth-model change); R3 ← gap
coverage (no dependency creep). SC-1→R1; SC-2→R1; SC-3→R1/R2; SC-4→R2.

Byline: 🤖 Co-authored with AI: OpenCode (huggingface/zai-org/GLM-5.3-Flash)