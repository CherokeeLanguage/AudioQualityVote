(issue: AudioQualityVote#2) (YAML)
question: >
  Java libraries for a server-side GitHub OAuth flow (authorization-code
  exchange + /user identity fetch), usable in this stack: Java 8 bytecode
  target, javax servlet 3.x (Jersey 2.32 / Tomcat 9), no security framework.
date: 2026-10-29
verified: true
confidence: 0.85
recommendation:
  exchange: com.github.scribejava:scribejava-core/-apis:8.3.3 (GitHubApi module
    wires github.com/login/oauth/access_token + authorize URL + token
    extractor; MIT; Java 7 bytecode; dormant release line since 2023-01 —
    accepted, endpoints stable, halves independently replaceable)
  identity: org.kohsuke:github-api:1.330 (hub4j; MIT; Java-8 bytecode in 1.x;
    connectUsingOAuth(token) -> getMyself()/listEmails(); actively maintained —
    2.0-rc line in flight; pin 1.x for the Java 8 floor)
  note: kohsuke does NOT do the exchange by design (consumes tokens only).
ruled_out:
  - com.google.oauth-client:google-oauth-client 1.39.0 — generic exchange
    (AuthorizationCodeFlow); released 2025-03; Google self-declared
    "maintenance mode"; heavier transitives (google-http-client + guava)
  - pac4j:pac4j-oauth 6.1.2 — active but JDK 17 bytecode (majors 61); heavy
  - org.springframework.security:spring-security-oauth2-client — Java 17 +
    Spring context; excluded
  - org.apache.oltu:* — retired to Apache Attic 2018; dead
  - com.nimbusds:oauth2-oidc-sdk 11.38.2 — active generic (monthly cadence,
    Java 7 bytecode); unverified against GitHub token-endpoint specifics;
    OIDC-flavored
  - bytecode downgrade (unimined/JvmDowngrader up to Java 22 -> 8;
    net.raphimc.javadowngrader 1.1.1) — tools exist and work; NOT used: no
    library this project consumes is jdk8-blocked, and jar-rewrite machinery
    is bespoke permanent infrastructure
used_by: AudioQualityVote#2 (spec names the pair; criteria SC-6)
sources:
  - https://api.github.com/repos/scribejava/scribejava/releases/latest
  - https://repo1.maven.org/maven2/com/github/scribejava/scribejava-core/
  - https://repo1.maven.org/maven2/org/kohsuke/github-api/
  - https://repo1.maven.org/maven2/com/google/oauth-client/google-oauth-client/
  - https://attic.apache.org/projects/oltu.html
  - https://repo1.maven.org/maven2/org/pac4j/pac4j-parent/6.1.2/pac4j-parent-6.1.2.pom
  - https://repo1.maven.org/maven2/com/nimbusds/oauth2-oidc-sdk/