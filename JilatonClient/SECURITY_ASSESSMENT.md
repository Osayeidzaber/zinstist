# Static security assessment

Checkpoint: 2026-10-30. This is a partial, evidence-based static assessment. No third-party client JAR was executed, installed, or loaded into Minecraft by this analysis. Both Anubis and LiquidBounce remain **Insufficient evidence** as whole-file security classifications; this report does not declare either artifact safe.

## Artifacts and identity

| Artifact | Exact path | Size | SHA-256 | Static status |
|---|---|---:|---|---|
| Anubis 0.9.9.3 (desktop copy) | `C:\Users\zaber\Desktop\zinstist\Anubis Beta 0.9.9.3 (1).jar` | 19,646,433 bytes | `c5259f9ffee4bbe2511972f7941ab6fc2dcda691dc0d5ab18a558f3f8f4285de` | Insufficient evidence |
| Anubis 0.9.9.3 (downloads copy) | `C:\Users\zaber\Downloads\Anubis Beta 0.9.9.3.jar` | 19,646,433 bytes | `c5259f9ffee4bbe2511972f7941ab6fc2dcda691dc0d5ab18a558f3f8f4285de` | Same bytes as desktop copy; insufficient evidence |
| LiquidBounce 0.37.0 | `C:\Users\zaber\AppData\Roaming\CCBlueX\LiquidLauncher\data\gameDir\nextgen\mods\LiquidBounce.jar` | 51,115,814 bytes | `6bbf432cc5b829963b671c8b948e33e6cdb7a66d14f3a2f8507cad4f3f94f898` | Insufficient evidence |
| Krypton 1.21.1 | `C:\Users\zaber\Desktop\zinstist\krypton-client-1.21.1.jar` | Missing | — | Not available for analysis |
| Prestige 1.21.1 | `C:\Users\zaber\Desktop\zinstist\prestige-client-1.21.1.jar` | Missing | — | Not available for analysis |
| Rise 1.21.1 | `C:\Users\zaber\Desktop\zinstist\Rise-client-1.21.1.jar` | Missing | — | Not available for analysis |

The Anubis desktop and Downloads files have identical SHA-256 hashes. REA identified both inspected client JARs as ZIP archives. Its complete central-directory inventory reported no integrity contradictions; safe extraction verified path containment and file hashes. Archive inventory and extraction establish neither behavior nor safety.

Current-turn REA reinspection evidence: Anubis summary `ev_716e932fa587098b0de5892fc35c91d032b79f97304e3b4a3a63fdab378d12e7` reports 1,318 archive occurrences (1,317 nodes, no integrity contradictions); LiquidBounce summary `ev_a830cbafb372d4b884299d9e610e1e96e56c2c35a28a3e5903bb7c10665d3c95` reports 4,642 archive occurrences (4,641 nodes, no integrity contradictions). These are central-directory inventories, not behavioral or safety attestations.

## Anubis observations

The embedded `fabric.mod.json` identifies mod `anubisplus`, version `0.9.9.3+mc1.21.11`, client environment, pre-launch entry point `net.anubis.client.security.EarlyBuildCheck`, and client entry point `net.anubis.client.AnubisClientMod`. The manifest also names `net.anubis.launcher.InstallNotice` as `Main-Class`. Minecraft metadata targets 1.21.11 and Java 21. The JAR includes bundled Fabric API and UI/native-related libraries; all embedded payloads remain unexecuted.

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

## Jilaton source scan

The current Jilaton Java source was searched for HTTP clients/URLs, sockets, process execution, dynamic class loading, native loading, environment variables, browser storage, cookies, tokens, and webhooks. Matches were limited to attribution comments; no such application calls were found in the scanned Java source. Jilaton's configuration writes its own module settings, HUD positions, themes, and friend names under Fabric's config directory. Resources and dependency behavior still require a broader audit.

## Coverage limits and next checks

- These results are static observations. No network capture, dynamic trace, sandbox execution, complete decompilation-completeness check, dependency provenance audit, or exhaustive call/data-flow analysis has been performed.
- REA's ZIP inventory covers central-directory entries and does not prove behavior of bytes outside those entries or runtime behavior.
- Do not copy Anubis's OS process/media, Discord IPC, Spotify art/lyrics, authentication, update, or native-loading paths into Jilaton without a separate need and review.
- Keep the Jilaton implementation free of user-account tokens, browser profiles/cookies, arbitrary process execution, remote code loading, and unexplained outbound network calls. Re-run static source scans when such capabilities or dependencies are added.
