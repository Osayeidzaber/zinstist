# Static security assessment

Checkpoint: 2026-10-30. This is a partial, evidence-based static assessment. No third-party client JAR was executed, installed, or loaded into Minecraft by this analysis. Both Anubis and LiquidBounce remain **Insufficient evidence** as whole-file security classifications; this report does not declare either artifact safe.

## Artifacts and identity

| Artifact | Exact path | Size | SHA-256 | Static status |
|---|---|---:|---|---|
| Anubis 0.9.9.3 (desktop copy) | `C:\Users\zaber\Desktop\zinstist\Anubis Beta 0.9.9.3 (1).jar` | 19,646,433 bytes | `c5259f9ffee4bbe2511972f7941ab6fc2dcda691dc0d5ab18a558f3f8f4285de` | Insufficient evidence |
| Anubis 0.9.9.3 (downloads copy) | `C:\Users\zaber\Downloads\Anubis Beta 0.9.9.3.jar` | 19,646,433 bytes | `c5259f9ffee4bbe2511972f7941ab6fc2dcda691dc0d5ab18a558f3f8f4285de` | Same bytes as desktop copy; insufficient evidence |
| Anubis 0.9.9.1 | `C:\Users\zaber\Downloads\Anubis Beta 0.9.9.1.jar` | 21,206,373 bytes | `20a03c38a39d013bf5f6836818c0b665478eb3381181fd372cd661c438fd752a` | Insufficient evidence |
| LiquidBounce 0.37.0 | `C:\Users\zaber\AppData\Roaming\CCBlueX\LiquidLauncher\data\gameDir\nextgen\mods\LiquidBounce.jar` | 51,115,814 bytes | `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898` | Insufficient evidence |
| Unidentified `g9pfrn.jar` | `C:\Users\zaber\Downloads\g9pfrn.jar` | 3,114,249 bytes | `bdcb9bd162125d9831878e8494046dfc9411a7625e4ca1772ba91b0f87aaae09` | Suspicious; do not execute or install |
| Krypton 1.21.1 | `C:\Users\zaber\Desktop\zinstist\krypton-client-1.21.1.jar` | Missing | — | Not available for analysis |
| Prestige 1.21.1 | `C:\Users\zaber\Desktop\zinstist\prestige-client-1.21.1.jar` | Missing | — | Not available for analysis |
| Rise 1.21.1 | `C:\Users\zaber\Desktop\zinstist\Rise-client-1.21.1.jar` | Missing | — | Not available for analysis |

The Anubis desktop and Downloads files have identical SHA-256 hashes. REA identified both inspected client JARs as ZIP archives. Its complete central-directory inventory reported no integrity contradictions; safe extraction verified path containment and file hashes. Archive inventory and extraction establish neither behavior nor safety.

Current-turn REA reinspection of Anubis 0.9.9.1 confirmed a ZIP archive with 1,288 occurrences (1,287 nodes, zero inventory integrity contradictions). Its `fabric.mod.json` declares Anubis 0.9.9.1 for Minecraft 1.21.11, client environment, and GPL-3.0-only. This archive inventory and manifest review do not establish whole-file safety; its status remains insufficient evidence.

Current-turn REA reinspection evidence: Anubis summary `ev_716e932fa587098b0de5892fc35c91d032b79f97304e3b4a3a63fdab378d12e7` reports 1,318 archive occurrences (1,317 nodes, no integrity contradictions); LiquidBounce summary `ev_b522aff3da7c5c3b3ffac0ddbd0bc7a0edbcc107ce242f4b802ee63d0dfe2f0d` reports 4,642 archive occurrences (4,641 nodes, no integrity contradictions). The LiquidBounce module occurrence for `ModuleSprint` was identified through inventory evidence `ev_4630ba0a4041f789d048f71e6637c9060751a0b38b64e29f668ee68dd2213fe7` and statically decompiled with CFR 0.152. These are static observations, not behavioral or whole-file safety attestations; both client JARs remain classified as insufficient evidence.

## Anubis observations

The embedded `fabric.mod.json` identifies mod `anubisplus`, version `0.9.9.3+mc1.21.11`, client environment, pre-launch entry point `net.anubis.client.security.EarlyBuildCheck`, and client entry point `net.anubis.client.AnubisClientMod`. The manifest also names `net.anubis.launcher.InstallNotice` as `Main-Class`. Minecraft metadata targets 1.21.11 and Java 21. The JAR includes bundled Fabric API and UI/native-related libraries; all embedded payloads remain unexecuted.

The current desktop copy was rechecked without loading it: REA reports 1,318 archive occurrences (1,317 nodes, 1,237 files, 81 directories, zero inventory integrity contradictions) for SHA-256 `c5259f9ffee4bbe2511972f7941ab6fc2dcda691dc0d5ab18a558f3f8f4285de` (summary evidence `ev_716e932fa587098b0de5892fc35c91d032b79f97304e3b4a3a63fdab378d12e7`). The manifest declares Anubis 0.9.9.3 for Minecraft 1.21.11 and Java 21, with an ImGui Java binding and platform native archives in `META-INF/jars`. JDK `jarsigner -verify -verbose -certs` reports the SHA-256/ECDSA signature valid, but the signer is self-signed (`CN=Anubis Client`) and the certificate chain cannot be validated against the local trust store; the signature therefore does not establish publisher identity. The signer certificate SHA-256 fingerprint is `C0:F6:2A:59:D9:78:5F:4D:2F:02:6C:DD:71:F4:AC:27:26:76:DB:D1:49:E3:24:73:C2:62:47:11:F3:47:0E:79`. These results do not change the whole-artifact status of insufficient evidence.

Static CFR review found these host interaction paths:

- `OfficialBuild` verifies signed JAR entries against an embedded expected certificate fingerprint. On a detected modification, `TamperStop` logs, displays a warning, then exits. Its Swing warning fallback starts the current Java executable with the same JAR and `TamperWindow` class as a child process. This observed process launch is tied to the tamper-warning path; it is not evidence that the whole artifact is safe.
- `ScriptedPlayer` uses `ProcessBuilder` for platform media integration. The reviewed Linux implementation passes fixed `dbus-send` arguments to query or control Spotify over the session bus; the reviewed macOS implementation passes fixed AppleScript strings to `osascript`. The caller-supplied arrays in those implementations are not derived from downloaded text. The rest of the media/platform code has not received a complete call-graph review.
- Spotify album artwork code performs HTTP(S) requests using artwork URLs obtained from local media metadata, follows redirects, applies a three-second timeout, and caps responses at 4 MiB. This can contact a URL selected by that metadata, so it is not being reused in Jilaton.
- Lyrics use `lrclib.net`; the skin utility contacts Mojang profile endpoints. Discord Rich Presence integration reads `DISCORD_IPC_*` environment values to locate Discord IPC. Those are observable network/IPC surfaces, not by themselves evidence of credential theft.
- The searched CFR source did not reveal reviewed reads of browser `Login Data`/`Cookies`, browser profiles, Windows credential stores, or wallet files. That bounded search does not prove those behaviors are absent from all bytecode.

The supplied Anubis JAR contains `LICENSE_anubis` with GPL-3.0 text. CFR output in `reference/anubis-cfr-0.9.9.3` is a recovery aid, not original source and not a complete behavioral proof. The configured upstream repository was unavailable during the prior review; compatibility or authorship cannot be inferred from the decompilation alone.

## LiquidBounce observations

The embedded metadata identifies `liquidbounce` version `0.37.0`, targets Minecraft 1.21.11, requires Fabric Language Kotlin, and lists many embedded libraries. REA inventoried 4,253 files and 389 directories, with zero archive-inventory integrity contradictions. An available source checkout is at commit `e033f5aaf05be802dd2ca51782f255119d7d5654`; that commit is not cryptographically matched to this JAR digest.

Static `javap` inspection of classes extracted from the actual JAR confirms an `OAuthClient`, `AuthenticationApi`, `ClientInteropServer`, and `AuthMiddleware`; the bytecode constants include LiquidBounce's auth endpoint and an advertised `http://127.0.0.1:<port>` URL. The server's actual bind interface and the complete authorization flow were not verified. The local source checkout is incomplete for these `api` and `integration` packages, so it cannot close that gap. This is visible account/network functionality and is not being ported into Jilaton. A source search of the available checkout did not identify browser-cookie/profile scraping or arbitrary process-launch code, but that result is not proof about the compiled artifact. The source checkout and ZIP inventory are not a full audit of the JAR.

A bounded combat-module review used REA archive evidence `ev_a830cbafb372d4b884299d9e610e1e96e56c2c35a28a3e5903bb7c10665d3c95`, CFR 0.152 output and static `javap -p -c` for `ModuleAutoLeave`, its `tickConditional` predicate class, and `ModuleKick$KickModeEnum` in the installed JAR above. The compiled predicate confirms the AutoLeave health threshold, survival/singleplayer gates, and main/offhand death-protection checks. `ModuleKick` includes a normal QUIT mode alongside intentional exploit actions: movement packets with NaN/infinite coordinates, self-attack, malformed chat, held-slot `-1`, and illegal self-interaction. These are risky network-abuse features, but this limited code review is not evidence of credential theft or malware and does not change the whole-JAR classification. Jilaton adapts only the ordinary Minecraft disconnect path; it does not expose or send the exploit packets. The local checkout at commit `e033f5aaf05be802dd2ca51782f255119d7d5654` is not cryptographically matched to the installed JAR.

## Unidentified `g9pfrn.jar` — analysis stopped

REA verified this ZIP archive's central-directory inventory (30 occurrences, 27 files, zero inventory integrity contradictions). Its metadata identifies Fabric entrypoint `net.java.h`, manifest `Premain-Class` `net.java.f`, and `Main-Class` `net.java.n`; it also contains Forge metadata. Class names are heavily obfuscated (`net.java.*`) and the archive includes a 2,333,048-byte extensionless payload. Static byte-string inspection found `ProcessBuilder` in `net/java/g.class`, `Socket` in `net/java/l.class`, and `defineClass` in `net/java/n.class`, alongside JNA references. This combination has no established legitimate purpose in the available evidence, so investigation stopped at these indicators; the payload and call paths were not further examined. This is a **Suspicious** classification, not confirmed malware. Do not run or install the artifact or use it as a reference. It was not executed.

## Jilaton source scan

The current Jilaton Java source was searched again after Step was added for HTTP clients/URLs, sockets, process execution, dynamic class loading, native loading, environment variables, browser storage, cookies, tokens, and webhooks. No matches were found in `src/main/java`. Jilaton's configuration writes its own module settings, HUD positions, themes, and friend names under Fabric's config directory. No Donut-specific branding, commands, or modules were found in `src`, README, or build files. This pass also searched `src/main/java` and Gradle configuration for process execution, dynamic/native loading, direct network APIs, browser storage, credentials, cookies and tokens; no matches were found. Gradle declares Minecraft, Fabric Loader/API and JUnit only. This is source-search coverage, not a whole-project or dependency behavior proof. Resources and dependency behavior still require a broader audit.

## Coverage limits and next checks

- These results are static observations. No network capture, dynamic trace, sandbox execution, complete decompilation-completeness check, dependency provenance audit, or exhaustive call/data-flow analysis has been performed.
- The `g9pfrn.jar` investigation stopped after suspicious static indicators; no claim is made about its complete behavior or whether it is malicious.
- REA's ZIP inventory covers central-directory entries and does not prove behavior of bytes outside those entries or runtime behavior.
- Do not copy Anubis's OS process/media, Discord IPC, Spotify art/lyrics, authentication, update, or native-loading paths into Jilaton without a separate need and review.
- Keep the Jilaton implementation free of user-account tokens, browser profiles/cookies, arbitrary process execution, remote code loading, and unexplained outbound network calls. Re-run static source scans when such capabilities or dependencies are added.

## HUD/Zoom implementation and runtime checkpoint (2026-10-30)

REA re-opened the retained Anubis archive and returned the same structural identity and complete central-directory summary (`ev_716e932fa587098b0de5892fc35c91d032b79f97304e3b4a3a63fdab378d12e7`). Artifact metadata/hashes were rechecked read-only in `development/artifact-identities.json`. The unknown `g9pfrn.jar` remains excluded. No third-party client was executed. The existing Jilaton source and its reviewed standard Fabric build were compiled and launched with the offline development profile; the new local Creative test world contains no imported account credentials. See `development/CHECKPOINT.md` for reproduced/fixed Mixin startup errors, actual UI smoke checks and remaining limitations. This source-client runtime test does not change the static security classification of any reference artifact.
