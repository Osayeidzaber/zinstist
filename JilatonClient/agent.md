# Jilaton project guide

This file records the working rules for future contributors and coding agents. The current implementation and verification state lives in [`info.md`](info.md) and [`IMPLEMENTATION_INVENTORY.md`](IMPLEMENTATION_INVENTORY.md).

## Project identity

- Product: Jilaton Client, a client-side Minecraft Java PvP mod.
- Foundation: the existing Anubis-based Java/Fabric project. Keep Java as the main language; do not migrate the project wholesale to Kotlin.
- Current target: Minecraft 1.21.11, Java 21, Fabric Loader 0.20.1, Fabric API 0.141.6.
- Preserve the existing architecture and user changes. Make small, reviewable changes and avoid replacing functioning systems without a concrete reason.

## Safety rules

- Treat downloaded client JARs as untrusted data. Inspect archives and bytecode statically; never launch them or load them into Minecraft on the host.
- Do not run unknown installers, scripts, native libraries, or build logic from reference artifacts. Do not ask for credentials or access browser, Microsoft, or Discord session data.
- Keep Jilaton free of unexplained credential/token access, browser-profile scraping, arbitrary process execution, and remote code loading.
- REA, CFR, `javap`, archive inspection, and other static tools may be used to investigate artifacts. Record exact artifact identity, SHA-256, evidence, scope, and limitations in the security/inventory documents.
- Do not describe a JAR as safe based only on a clean scan or incomplete static review. Use the risk statuses and caveats in `SECURITY_ASSESSMENT.md`.

## Implementation rules

- Inspect the existing source, module/event/settings/config/render hooks, and local project instructions before editing.
- Prefer existing module lifecycle, settings, rendering, event dispatch, and persistence systems. Avoid duplicate infrastructure and placeholder modules.
- A module must have real behavior, settings where appropriate, enable/disable handling, GUI/config integration, and focused verification. Clearly distinguish source/build/test evidence from behavior verified inside Minecraft.
- Preserve vanilla/client stability: validate world/player state, avoid stale state after disable, coordinate shared input/rotation/hotbar actions, and avoid unnecessary per-frame allocations.
- Keep the middle-click friend behavior reversible: click a targeted player once to add, click again to remove; friend names persist through the existing config. The blue overhead hat is a friend marker.
- Respect licenses and attribution. LiquidBounce-derived adaptations are covered by GPL-3.0-or-later obligations; see `ATTRIBUTION.md`. Do not copy proprietary branding or assets.

## Verification and change management

- Preserve the current uncommitted working tree. Do not reset, clean, or overwrite unrelated work.
- Build/test only when requested or necessary to validate a code change. Do not launch Minecraft while the user says they are playing.
- Current supported build command (PowerShell):

  ```powershell
  $env:JAVA_HOME='C:\Users\zaber\Desktop\zinstist\analysis\toolchains\temurin-21\jdk-21.0.12.1+1'
  $env:Path="$env:JAVA_HOME\bin;$env:Path"
  .\gradlew.bat build --rerun-tasks --no-daemon
  ```

- After a verified build, update the artifact hash, size, test count, and outstanding runtime limitations in `IMPLEMENTATION_INVENTORY.md`.
- Do not claim that a feature, game load, or release is verified unless the corresponding check actually ran and succeeded.
