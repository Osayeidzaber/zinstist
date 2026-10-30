# Jilaton Client — provenance and release audit

**Audit date:** 2026-10-30
**Checkout:** `C:\Users\zaber\Desktop\zinstist`, branch `main`, HEAD `f11bfff`, remote `https://github.com/Osayeidzaber/zinstist.git` (public).
**Scope:** assets under `JilatonClient/src/main/resources`, the attribution claims in `JilatonClient/ATTRIBUTION.md`, and what the built artifacts actually contain.
**Nature:** read-only. No Gradle task, no client launch, no JAR execution, no edit to `ATTRIBUTION.md`, `CHECKPOINT.md`, `analysis/` or `reference/`. This file is the only write.

## 0. Method and limits

Evidence was gathered with file reads, `find`/`file`/`sha256sum`, `git ls-files`/`git log --all`/`git check-ignore`, and Python `zipfile` **listing and reading** of JARs (never executing them). The reference Anubis sources under `JilatonClient/reference/` and `analysis/` were read for comparison only.

Limits that affect verdicts:

- The installed LiquidBounce JAR whose SHA-256 is cited throughout ATTRIBUTION (`6bbf432c…f898`) is **not present in this checkout**. Only its `javap`/CFR transcripts in `JilatonClient/analysis/` (git-ignored) remain. LiquidBounce claims can therefore be checked for consistency and for absence of copied code, but the cited archive cannot be re-hashed locally.
- The Anubis JAR **is** present and was re-hashed; it matches the claim exactly (below).
- Statements in ATTRIBUTION of the form "the user stated they have permission" are not licenses and cannot be verified from the repository. They are reported, not relied on.
- No network access was used, so `https://github.com/4ldenz/Anubis-client` (ATTRIBUTION.md:4, claimed 404) was not re-checked.

## 1. Asset provenance

### 1.1 What the searches found

| Search | Result |
| --- | --- |
| `git ls-files JilatonClient/src/main/resources` | only `assets/jilaton/lang/en_us.json`, `fabric.mod.json`, `jilaton.mixins.json` are tracked. **All 15 image/script assets are untracked.** |
| `git log --all -- <textures/brand …>` and `git log --follow -- sky/day.png` | **no commits on any branch.** The assets have never existed in Git history. |
| `git status --short -- …/resources` | `?? assets/jilaton/brand/`, `?? assets/jilaton/scripts/`, `?? assets/jilaton/textures/`. |
| Repo-wide `*license*` / `*credit*` / `*notice*` search | only `JilatonClient/LICENSE` (GPL-3.0 text). No per-asset licence, credit or notice file exists. |
| `ATTRIBUTION.md` | sky section is line 155; brand/logo claim is line 145; the PowerShell bridge is line 157. The loading wordmark is **not** mentioned in ATTRIBUTION. |
| `development/CHECKPOINT.md` | loading wordmark generation: line 45; window icons: lines 27–36; artifact identity: line 13. (CHECKPOINT is being rewritten by other sessions; line numbers are as read at audit time.) |
| Source pack `C:\Users\zaber\Downloads\Dramatic Skys Demo 1.5.3.36.6.zip` | exists, 13,912,171 B, 102 entries, 20 PNGs. Contains `credit.txt` and `pack.mcmeta`; **no LICENSE/COPYING/NOTICE/README of any kind** (entry-name scan). |

### 1.2 Asset table

All paths are relative to `JilatonClient/src/main/resources/`. "Git" says whether the file is tracked; none of these 15 are.

| # | Path | Bytes | What it depicts | Where it came from | Licence / credit present | Git history | Risk |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `assets/jilaton/brand/icon-16.png` | 612 | 16×16 window icon: violet angular "J" on near-black rounded square | Generated from `assets/jilaton/brand/mark.svg` by `JilatonClient/tools/generate-brand-icon.py`; ATTRIBUTION.md:145 calls the mark original Jilaton artwork; CHECKPOINT.md:32 documents classpath use | Original artwork under the project GPL-3.0-only (`JilatonClient/LICENSE`); no separate credit | untracked, never committed | Low |
| 2 | `assets/jilaton/brand/icon-32.png` | 823 | 32×32, same mark | as #1 | as #1 | untracked | Low |
| 3 | `assets/jilaton/brand/icon-48.png` | 1,301 | 48×48, same mark | as #1 | as #1 | untracked | Low |
| 4 | `assets/jilaton/brand/icon-128.png` | 2,345 | 128×128, same mark; also the Fabric mod icon (`fabric.mod.json:6`) | as #1 | as #1 | untracked | Low |
| 5 | `assets/jilaton/brand/icon-256.png` | 4,547 | 256×256, same mark | as #1 | as #1 | untracked | Low |
| 6 | `assets/jilaton/brand/mark.svg` | 411 | Six-rectangle angular "J", fill `#8878ff`, `viewBox 0 0 24 24`; `<title>` says "original artwork" | Authored for Jilaton; ATTRIBUTION.md:145 | Original; project licence | untracked | Low |
| 7 | `assets/jilaton/textures/brand/loading-logo.png` | 42,430 | 2048×512 "JILATON / CLIENT" wordmark, light-grey text on transparent, angular J at left; large transparent padding (text occupies roughly x 273–1790, y 107–406 per CHECKPOINT.md:8) | Rasterised by `JilatonClient/tools/generate-loading-logo.py` from `mark.svg` plus Windows `C:/Windows/Fonts/bahnschrift.ttf`; CHECKPOINT.md:45 | Wordmark/mark original; **font is Microsoft Bahnschrift**, used only to rasterise an image — not documented in ATTRIBUTION | untracked | Low (documentation gap) |
| 8 | `assets/jilaton/textures/sky/day.png` | 2,186,889 | 2048×1365 cube-net: photoreal blue sky with heavy cumulus and light rays | Downscaled from `assets/skybox/day.png` in "Dramatic Skys Demo 1.5.3.36.6" (thebaum64); ATTRIBUTION.md:155; loaded by `CustomSkyModule.java:53,157` | **No licence file in the pack.** Credits exist only in the pack: `credit.txt` and `pack.mcmeta`. Neither is in this repo or any jar | untracked, never committed | **High** |
| 9 | `assets/jilaton/textures/sky/night.png` | 762,264 | 2048×1365 cube-net: near-black night clouds, faint moonlit edges | same pack (`assets/skybox/night.png`); ATTRIBUTION.md:155 | as #8 | untracked | **High** |
| 10 | `assets/jilaton/textures/sky/sun.png` | 1,885,950 | 2048×1365 cube-net: golden/amber sunset clouds with sun rays | same pack (`assets/skybox/sun.png`); ATTRIBUTION.md:155 | as #8 | untracked | **High** |
| 11 | `assets/jilaton/textures/sky/stars.png` | 166,496 | 2048×1365 black star field, sparse white points | same pack (`assets/skybox/stars.png`); ATTRIBUTION.md:155 | as #8 | untracked | **High** |
| 12 | `assets/jilaton/textures/sky/mask.png` | 127,997 | 1024×683 high-contrast cloud mask: black field, white cumulus band near the horizon | same pack (`assets/skybox/mask.png`); ATTRIBUTION.md:155 | as #8 | untracked | **High** |
| 13 | `assets/jilaton/textures/sky/mask_moon.png` | 3,129 | 1024×683 near-uniform white layer with one small dark circular disc (moon cut-out) | same pack (`assets/skybox/mask_moon.png`); ATTRIBUTION.md:155 | as #8 | untracked | **High** |
| 14 | `assets/jilaton/textures/sky/sunflare.png` | 17,496 | 1024×683 black frame with a small orange/white four-point starburst | same pack (`assets/skybox/sunflare.png`); ATTRIBUTION.md:155 | as #8 | untracked | **High** |
| 15 | `assets/jilaton/scripts/media-session.ps1` | 5,681 | Windows PowerShell bridge for GSMT C media sessions (title/artist/album/position, cover art, play-pause/next/prev); no file, token or network access | ATTRIBUTION.md:157 calls it original Jilaton code; copied out of the mod classpath and run by `music/MediaSessionSource.java:67-82` | Original; project licence | untracked | Low (executable script, see L4) |

Total sky payload: **5,150,221 bytes** (4.91 MiB) across the seven PNGs.

The pack's two credit lines were read verbatim and are quoted here because they are the only authorship statement that exists:

> Credit to Hipshot for base legacy textures
> (Created in 2005 by Hipshot, reformatted/edited for MC by me in 2012-2014)

`pack.mcmeta` describes the pack as `"Dramatic Skys Demo/Alpha 1.5.3.36.6 by thebaum64"`. That is the entire credit apparatus. No licence, no terms, no redistribution permission is present in the 102-entry archive.

### 1.3 Non-code resource descriptors (for completeness)

| Path | Bytes | Purpose | Provenance | Risk |
| --- | --- | --- | --- | --- |
| `fabric.mod.json` | 513 | mod id/entrypoint/deps; `license: GPL-3.0-only` (line 8); icon `assets/jilaton/brand/icon-128.png` (line 6) | original, tracked, modified in working tree | Low |
| `jilaton.mixins.json` | 1,819 | 59 client mixin entries, package `dev.jilaton.client.mixin` (line 5) | original, tracked | Low |
| `assets/jilaton/lang/en_us.json` | 139 | three keybind strings | original, tracked | Low |

### 1.4 Provenance that cannot be determined

- **Authorship split of the sky art.** The only statement is the pack's: Hipshot's 2005 base legacy textures, "reformatted/edited for MC" by the pack author. Which of the seven PNGs contain Hipshot's original pixels and how much was painted by thebaum64 cannot be determined from the archive; ATTRIBUTION.md:155 correctly avoids attributing percentages.
- **Redistribution rights.** No licence, EULA or permission text exists in the pack. Rights may be stated on the pack's distribution page, but that is outside this checkout, so the answer is *unknown*, not *permitted*.
- **Font licence for `loading-logo.png`.** The PNG is a rasterisation of Microsoft's Bahnschrift. No font file is redistributed, which is normally acceptable, but no font licence or attribution is recorded anywhere.
- **Whether a given published jar contains the textures.** It depends on which build directory is shipped; see 3.1 — the two current outputs disagree.

## 2. Code provenance cross-check

### 2.1 Existence sweep

Every Java symbol named in ATTRIBUTION was checked on disk. **All 62 module/policy/geometry files tested exist** under `JilatonClient/src/main/java/dev/jilaton/client/…`, including `TargetEspModule`, `TargetEspGeometry`, `NametagsModule`, `FreecamModule`, `HitParticlesModule`, `CustomFovModule`, `CustomSkyModule`, `render/fx/FxFrame`, `rotation/RotationManager`, and all LiquidBounce-named modules from ElytraTarget through Tracers. No named file is missing.

### 2.2 Marker sweep

Searches across all 289 main sources for `net.anubis`, `com.example`, `net.ccbluex`, `class_`/`method_`/`field_` identifiers, `Decompiled with`, `CFR`, `GlyphCanvas`, `imgui`, `Intrinsics`, `WhenMappings`, `access$` and `lambda$` returned **zero hits**. The only `INSTANCE` is an ordinary singleton in `render/fx/FxParticles.java:21`. Jilaton is plain Java using Mojang mappings; no decompiler output, Kotlin runtime artefacts, Anubis package names or Anubis renderer classes were pasted in.

### 2.3 Anubis claims

The supplied JAR `Anubis Beta 0.9.9.3 (1).jar` at the repo root re-hashes to **`c5259f9ffee4bbe2511972f7941ab6fc2dcda691dc0d5ab18a558f3f8f4285de`**, exactly the value in ATTRIBUTION.md:5. Its `fabric.mod.json` declares `license: GPL-3.0-only`, `authors: ["4ldenz"]`, id `anubisplus`. The "Horcus / 4ldenz" credit in ATTRIBUTION is therefore slightly broader than the archive metadata, but not contradicted by it.

| Claim (ATTRIBUTION) | Named source | Exists | Implementation verdict | Evidence |
| --- | --- | --- | --- | --- |
| TargetESP adapts appear/vanish timing, memory, crossfade, hit flash; Jilaton geometry is original | `render/TargetEspModule.java` | yes | **Original Java adaptation.** Constants 0.30/0.38 s match; no Anubis styles, glyph renderer or palette | `module/impl/TargetEspModule.java:1-3` (attribution header), `:28-29` (0.30/0.38); `render/TargetEspGeometry.java:3` ("Original Jilaton target-marker shapes"), `:5-8` (Halo/Orbit/Helix/Brackets). Anubis file `reference/anubis-cfr-0.9.9.3/net/anubis/client/module/impl/render/TargetEspModule.java:44-62` still has Ankh/Horus/Vortex/Capstone and Glyph classes. |
| Nametags adapts the "complete recovered" Anubis module | `render/NametagsModule.java` | yes | **Behavioural reimplementation, not a code copy.** 128 lines vs 454; uses Jilaton `RecentItems`, settings and renderer; method sets differ almost completely | `module/impl/NametagsModule.java:1-2`, `:38` (`new RecentItems<>(128, 9)`); Anubis reference has `drawPanel/drawIcon/renderTag/nearest/…` while Jilaton has `targets/shouldHideVanilla/compact/…`. Feature list in the claim (latency, two hands, armour, 128-player/9-type history, dropped items, 64 cap) is present in the Jilaton file. |
| Freecam adapts the "complete recovered" module plus seven mixins | `misc/FreecamModule.java` | yes | **Behavioural adaptation.** 200 lines vs 413; own `FreecamMotion`, body-input and six Jilaton mixins; constants 40-tick learn and 2–32 chunk window retained | `module/impl/FreecamModule.java:1-2`, `:31` (chunk 2–32), `:59` (`learn = 40`); Anubis constants `LEARN_TICKS = 40`, `CHUNK_CAP = 32` at `…/misc/FreecamModule.java:58-59`. |
| HitParticles adapts the "complete recovered" module | `render/HitParticlesModule.java` | yes | **Behavioural adaptation**; 92 vs 195 lines; nine types/origins/jitter retained, Anubis ThemeTint/Peep replaced | `module/impl/HitParticlesModule.java:1-2`; no `ThemeTint`/`Peep`/`GlyphCanvas` anywhere in `src/main/java`. |
| CustomFov adapts module plus GameRenderer/ScreenEffect hooks | `render/CustomFovModule.java`, `mixin/MixinCustomFovGameRenderer.java`, `MixinScreenEffectRenderer.java` | yes | **Adaptation with the same injection point, different code.** Jilaton uses `@Redirect` on `new PoseStack()`; Anubis used `@ModifyExpressionValue` with the same target and scale formula | `module/impl/CustomFovModule.java:1-2`, `:38-42` (tan ratio); `mixin/FovScreenEffectMixin.java:11-21`; Anubis `reference/…/com/example/client/mixin/MixinScreenEffectRenderer.java:43-50`. Jilaton correctly drops Anubis' NoRender/BoatNoclip injections, as claimed. |
| Combat/utility modules (AimAssist, Triggerbot, KillAura, CrystalAura, AnchorAura, AutoTotem, AutoEat, WTap, …) adapt Anubis behaviour | 20+ files | yes | **Consistent at file level; not individually diffed.** No Anubis identifiers, no copied comments, own policies/settings | Example: `module/impl/ArmorScoring.java:3` explicitly credits LiquidBounce; no Anubis-origin file contains `net.anubis` strings (sweep 2.2). |

**Structural-copy verdict for Anubis: no literal copy found.** Method-name sets differ substantially in every "complete adaptation" file checked, no package names, headers, comments or decompiler artefacts survive, and the recovered source keeps intermediary `class_` names that Jilaton never uses. The word "complete" in ATTRIBUTION.md:125/133/141 describes the *behaviour set*, and that overstates parity in places (e.g. 128 vs 454 lines for Nametags), but not a licensing defect.

### 2.4 LiquidBounce claims

ATTRIBUTION.md:9–113 names ~60 upstream classes across ~55 Jilaton files. Checks:

- **Existence:** every named Jilaton file exists (2.1). The Jilaton-side implementation classes are ordinary Java with Jilaton settings, events and lifecycle.
- **Headers:** the files that most directly derive from upstream carry the CCBlueX copyright and GPL-3.0-or-later notice, e.g. `mixin/FirstPersonHandsAndItemsMixin.java:1-5`, `module/impl/AntiBotModule.java:2-4`, `module/impl/SwingAnimationsModule.java:2-4`, `module/impl/NoSlowModule.java:2-4`, `module/impl/FlightModule.java:2-4`.
- **No copied bytecode or Kotlin:** the built jar contains no `net/ccbluex`, `kotlin/`, `Module*` or `AutoFirework` classes (3.1); no `Intrinsics`/`WhenMappings`/`access$` in source (2.2).
- **Structural similarity:** direct textual comparison is impossible because the JAR cited at ATTRIBUTION.md:9 and the source snapshot commit `e033f5a` are absent from the checkout. What can be said is that no file shows decompiled-Kotlin shape, and every file is integrated into Jilaton's own module framework. Numeric constants that match upstream (e.g. HighJump 0.8/0.2–10.0 at ATTRIBUTION.md:67, LongJump 4.25 at :69, Reach 1.0/0.5 at :77, AutoArmor weights at `module/impl/ArmorScoring.java:15`) are values, not structure.
- **Verdict:** claims are internally consistent and the GPL-3.0-or-later notices are present where required; **reference JAR identity cannot be independently re-verified** (L3).

### 2.5 FabricSkyboxes claim

ATTRIBUTION.md:155 says the pack's FabricSkyboxes/Celestial layer definitions were "reimplemented as original Java in `FxFrame.skyCube`/`CustomSkyModule.packSky`" and that no FabricSkyboxes/Celestial code or jars are bundled.

- Both methods exist: `render/fx/FxFrame.java:84-102` (cube-net face table, UVs, yaw/roll, additive flag) and `module/impl/CustomSkyModule.java:144-161` (seven layers, per-layer blend and alpha).
- No `net.fabricmc.fabricskyboxes`, celestial or OptiFine package is imported anywhere; `build.gradle:33-41` depends only on Minecraft, Fabric Loader, Fabric API and JUnit. So the "no code bundled" half is true.
- **But the layer timing is pack data, not independent work.** `CustomSkyModule.java:128-129` uses `23666/333/11666/12333` and `13333/13666/22333/22666`, which are byte-for-byte the `startFadeIn/endFadeIn/startFadeOut/endFadeOut` values in the pack's `assets/fabricskyboxes/sky/sky2_day.json` and `sky3_night.json`. The Java is original; the schedule it encodes comes from the unlicensed pack. This belongs to the same rights question as the textures.

### 2.6 OptiFine claim

ATTRIBUTION.md:155 says no OptiFine code or jars are bundled. Repo and jar searches for `optifine` return only the word "OptiFine" inside that sentence. There is no dependency, class, asset or JAR. **Verdict: true.** The only OptiFine-adjacent item is behavioural: the pack's `"blend": {"type": "screen"}` layer is approximated with additive blending in Jilaton (`CustomSkyModule.java:157-160` pass `additive=true`), which the attribution already discloses.

### 2.7 Summary of section 2

| Source | Claim | Verdict | Confidence |
| --- | --- | --- | --- |
| Anubis | Files/methods named exist; behaviour adapted; no styles/renderer copied | **Consistent; original Java confirmed** | High (JAR hash, source comparison) |
| LiquidBounce | Files exist; notices present; no classes/binaries bundled | **Consistent; no copy evidence; identity not locally re-hashable** | Medium |
| FabricSkyboxes | No code/jars bundled; layer model reimplemented | **True for code; timing data is pack-derived** | High |
| OptiFine | No code/jars bundled | **True** | High |

No attribution claim was found to be false. Two are imprecise: "complete" (Anubis behaviour) and "original Java" applied to data values that come from the pack.

## 3. Redistribution check

### 3.1 What the built artifacts actually contain

JARs were opened as ZIP archives and entries listed/read; nothing was executed.

| Artifact | Bytes | Built | Entries | Reference classes / tooling | Sky textures |
| --- | --- | --- | --- | --- | --- |
| `JilatonClient/build/libs/jilaton-client-0.1.0.jar` | 4,108,980 | 20:45 | 415: 391 `dev/**`, 19 asset entries, 2 `META-INF`, 3 descriptors | none (`dev`, `assets`, `META-INF` only; no `.py`, no nested `.jar`) | **only `day.png`, `night.png`** (older byte sizes 2,375,657 / 834,869) |
| `JilatonClient/build/libs/jilaton-client-0.1.0-sources.jar` | 5,958,182 | 20:46 | 333: 305 `dev/**` java/assets | none | **all seven** |
| `JilatonClient/.isolated-build/root/libs/jilaton-client-0.1.0.jar` | 5,978,783 | 20:55 | classes + assets | none | **all seven** |
| `JilatonClient/.isolated-build/root/devlibs/jilaton-client-0.1.0-dev.jar` | 5,973,217 | 20:55 | dev classes + assets | none | all seven |
| `JilatonClient/.isolated-build/root/{libs,devlibs}/*-sources.jar` | 5,494,371 / 5,495,514 | 20:55 | source + assets | none | all seven |

Positive results:

- No `net/anubis/**`, `com/example/**`, `net/ccbluex/**`, `imgui/**`, `kotlin/**`, `.class` from a reference client, `.py` script, or nested reference JAR appears in any artifact. `META-INF` holds only `MANIFEST.MF`.
- `build.gradle:48` sets `sourceSets.main.java.srcDirs = ['src/main/java']`; `:33-41` declares only Minecraft/Fabric/JUnit. There is no `files(...)`, `flatDir`, copied-jar or `from(...)` packaging step, and `fabric.mod.json` has no `jars` entry, so no jar-in-jar path exists.
- `tools/package-development.py:24-28` asserts the packaged jar contains no `net/anubis/`, `net/ccbluex/` or `imgui/` entries — a useful guard, though the script itself is not run by `build`.
- The reference JAR at the repo root is untracked and matched by `*.jar`; `git ls-files "*.jar"` returns only `JilatonClient/gradle/wrapper/gradle-wrapper.jar`. It is not a Gradle dependency.

Negative results (release blockers) are in section 4: the sky art is in the build, and the jars carry **no** `LICENSE`, `ATTRIBUTION.md` or notice file.

### 3.2 `.gitignore` coverage

| Path | Ignored by | Verdict |
| --- | --- | --- |
| `analysis/` | `.gitignore:2` (`/analysis/`) | covered |
| `JilatonClient/analysis/` | `.gitignore:3` | covered |
| `JilatonClient/reference/` | `.gitignore:4` | covered |
| `Anubis Beta 0.9.9.3 (1).jar` | `.gitignore:14` (`*.jar`) | covered (also untracked) |
| `build/`, `run/`, `logs/`, `out/`, `*.log`, `*.class`, `*.zip`, `*.7z`, `*.tar.gz` | `.gitignore:7-17` | covered |
| `JilatonClient/build/`, `run/`, `run-verify/` | `JilatonClient/.gitignore:2-6` | covered (`run-verify/` was just added; it is the only working-tree change in that file) |
| `JilatonClient/tools/remap-anubis.py` | **nothing** — `git check-ignore` says NOT IGNORED | gap (M2) |
| `JilatonClient/.isolated-build/` | **nothing** — NOT IGNORED (jars inside are covered by `*.jar`, classes/sources are not) | gap (M5) |
| `…/assets/jilaton/textures/sky/*.png` | **nothing** — NOT IGNORED and untracked | gap (folded into H1) |

### 3.3 Remap tooling

`JilatonClient/tools/remap-anubis.py:1-6` describes itself as rewriting intermediary names in "recovered Anubis sources" using Loom mappings, read-only. It is **not** in any JAR and was not executed. It **is** swept into the published source archive by `tools/package-development.py:63-65`, which copies every file in `tools/` (and every file in `development/`) into `build/distributions/jilaton-client-0.1.0-source.zip`. That contradicts ORCHESTRATION.md:94-95 ("`tools/remap-anubis.py` … must never ship") for the source distribution, though not for the mod JAR.

### 3.4 Release-identity mismatch

- `development/build-verification.json:2-8` records 346 tests, **955,370 bytes**, SHA-256 `15113b3a…`.
- `build/libs/jilaton-client-0.1.0.jar` is **4,108,980 bytes**, SHA-256 `fc8dcf9d…` (matches CHECKPOINT.md:13, 20:45:50).
- `.isolated-build/root/libs/jilaton-client-0.1.0.jar` is **5,978,783 bytes**, SHA-256 `2dc40a5b…`.
- Sources jars differ too: `700ad4d9…` (20:46) vs `c10c098b…` (20:55).

No single recorded identity describes the current tree, and the 20:45 and 20:55 jars disagree about which sky textures they contain. ORCHESTRATION.md:63-65 already flags the test-count/byte mismatch; the sky difference is new evidence that a rebuild changes the redistributed payload.

## 4. Ranked findings

| ID | Severity | Finding | Owner |
| --- | --- | --- | --- |
| H1 | **High** | Seven unlicensed sky PNGs (5,150,221 B) are in the source tree and in current build outputs | B (R1) |
| M1 | Medium | Neither mod jar contains a licence or attribution notice | O |
| M2 | Medium | Release source archive packages `tools/remap-anubis.py` and all `development/` files | O |
| M3 | Medium | Release identity is ambiguous/stale; the 20:45 and 20:55 jars differ in third-party payload | O |
| M4 | Medium | Pack-defined fade windows are copied into Java, widening the unlicensed-data footprint | B |
| M5 | Low/Medium | `.isolated-build/` is unignored; the sky directory is unignored and can be committed | O/B |
| L1 | Low | Loading wordmark and its Bahnschrift font are undocumented in ATTRIBUTION | C/O |
| L2 | Low | "User-stated permission" is not a licence; the operative basis is GPL-3.0-only | O |
| L3 | Low | The cited LiquidBounce JAR is absent, so its SHA and contents cannot be re-verified locally | O |
| L4 | Low | The bundled PowerShell bridge is executed with `-ExecutionPolicy Bypass` | A |
| L5 | Low | The dev-only `dev/jilaton/client/dev/VisualVerifier` ships in the production jar | O |

### H1 — Unlicensed third-party sky textures are bundled and shipped

**Evidence**

- Files: `JilatonClient/src/main/resources/assets/jilaton/textures/sky/{day,night,sun,stars,mask,mask_moon,sunflare}.png`, 2,186,889 + 762,264 + 1,885,950 + 166,496 + 127,997 + 3,129 + 17,496 = **5,150,221 bytes**.
- Source pack `C:\Users\zaber\Downloads\Dramatic Skys Demo 1.5.3.36.6.zip` (13,912,171 B, 102 entries). Full entry scan finds `credit.txt`, `pack.mcmeta`, `splashes.txt` and no licence. `credit.txt` credits Hipshot's 2005 base textures; `pack.mcmeta` credits thebaum64. **No permission to redistribute.**
- ATTRIBUTION.md:155 documents exactly this and says the art "is not relicensed by Jilaton"; ORCHESTRATION.md:87-93 rates it R1 high/open and calls for de-bundling.
- The bundling is not theoretical: `.isolated-build/root/libs/jilaton-client-0.1.0.jar` (20:55) and both sources jars contain all seven files; `build/libs/jilaton-client-0.1.0.jar` (20:45) already contains `day.png` and `night.png`. No jar contains `credit.txt`, `pack.mcmeta`, a licence, or any mention of Hipshot/thebaum64.
- Git: all seven are untracked and **not ignored** (`git check-ignore` says NOT IGNORED; `git status` shows `?? …/textures/`). `git log --all` shows they were never committed on any branch, so a plain `git add .` would publish 5.15 MB of unlicensed art to a public GitHub remote for the first time.
- `CustomSkyModule.java:52` enables the pack by default (`dramatic_skys = true`) and `:96-98` loads only from the `jilaton` classpath namespace. There is no runtime path loader for a user-supplied pack, so the documented mitigation (ORCHESTRATION.md:91-93) is not implemented.

**Smallest correct remediation**

1. Delete the seven PNGs from `JilatonClient/src/main/resources/assets/jilaton/textures/sky/` and add that directory to `.gitignore` so they cannot be committed while rights are unresolved.
2. Set the `dramatic_skys` default to `false` (or make `packSky` a no-op when the textures are absent) so the procedural themes remain the shipped sky.
3. Load the pack from a user-supplied directory at runtime (e.g. `config/jilaton/sky/` via `NativeImage`/`ResourceManager`) rather than the mod classpath, and document the folder in the Sky module tooltip.
4. Rebuild, then assert in `tools/package-development.py` that neither the mod jar nor the sources jar contains `assets/jilaton/textures/sky/`.
5. Delete or regenerate the stale artifacts listed in M3 so no pre-fix jar remains distributable.

Alternative only if a written redistribution permission is obtained from thebaum64 (and Hipshot for the base textures): keep the files, ship `credit.txt` plus the permission text in `META-INF/`, and update ATTRIBUTION. Until one of these is done, every published jar or source zip containing them is the high-risk item.

### M1 — Jars ship no licence or attribution notice

**Evidence.** Entry listings for `build/libs/jilaton-client-0.1.0.jar` and `.isolated-build/root/libs/jilaton-client-0.1.0.jar` show only `META-INF/MANIFEST.MF` under `META-INF`; no `LICENSE`, `ATTRIBUTION`, `NOTICE` or `.md`. The Anubis reference JAR, by contrast, carries `LICENSE_anubis`. GPL-3.0 conveyance of object code requires the licence text and notices; the project itself says "Preserve the applicable notices" (`README.md:168`) and BUILDING.md:13 claims the archive includes them (true for the source zip at `package-development.py:57-58`, false for the jar).

**Remediation.** In `build.gradle` `processResources`, copy `LICENSE` and `ATTRIBUTION.md` into `META-INF/` (e.g. `META-INF/LICENSE` and `META-INF/ATTRIBUTION.md`), rebuild, and add an assertion in `package-development.py` that both exist.

### M2 — Release source archive ships remap tooling

**Evidence.** `tools/package-development.py:63-65` copies every file in `tools/` and `development/` into `jilaton-client-0.1.0-source.zip`. That includes `tools/remap-anubis.py` (`:1-6`), which exists to rewrite recovered Anubis sources, and `development/artifact-identities.json` (576 KB) with reference-artifact identities. ORCHESTRATION.md:94-95 requires that the remap tool and reference JARs never ship.

**Remediation.** Move `remap-anubis.py` under `analysis/tools/` (already ignored) or add it to the packaging exclusion list; add a packaging assertion that no `remap-anubis.py`, `analysis/`, `reference/` or `artifact-identities.json` entry appears in either zip. The mod jar is already clean.

### M3 — Release identity is ambiguous and the two jars disagree

**Evidence.** `development/build-verification.json:2-8` (346 tests, 955,370 B, `15113b3a…`) matches neither `build/libs` (4,108,980 B, `fc8dcf9d…`, 20:45) nor `.isolated-build/root/libs` (5,978,783 B, `2dc40a5b…`, 20:55). The sources jars also differ (`700ad4d9…` vs `c10c098b…`). The 20:45 jar has two sky textures; the 20:55 jar has seven. ORCHESTRATION.md:63-65 records the same inconsistency.

**Remediation.** Pick one output directory as release-of-record, delete or clearly mark the other as stale, regenerate `build-verification.json` from the actual `sha256sum` and `stat` of that jar after the H1 change, and make the packaging script fail if the recorded size/hash do not match.

### M4 — Pack-defined timing data is copied into Java

**Evidence.** `CustomSkyModule.java:128-129` hard-codes `23666, 333, 11666, 12333` (day) and `13333, 13666, 22333, 22666` (night). These are exactly the `startFadeIn/endFadeIn/startFadeOut/endFadeOut` values in the pack's `assets/fabricskyboxes/sky/sky2_day.json` and `sky3_night.json`. The code is original; the schedule is not.

**Remediation.** Treat the timing values as part of the same permission request as H1, or read fade windows from the user-supplied pack JSON at runtime. Document in ATTRIBUTION that layer order and fade windows are reproduced from the pack's data, not independently invented.

### M5 — Ignore gaps around build and sky directories

**Evidence.** `git check-ignore` reports NOT IGNORED for `JilatonClient/.isolated-build` (20:55 jars, classes, generated sources) and for `…/textures/sky/day.png`. `.isolated-build` contains a full 5,978,783-byte release jar and sources.

**Remediation.** Add `.isolated-build/` to `JilatonClient/.gitignore`, and the sky directory per H1. Keep `*.jar` coverage; it already protects the jars themselves.

### L1 — Loading wordmark provenance is undocumented

**Evidence.** `loading-logo.png` is generated by `tools/generate-loading-logo.py` (lines 1–10: Pillow + `C:/Windows/Fonts/bahnschrift.ttf`) from `mark.svg`; CHECKPOINT.md:45 documents it. ATTRIBUTION.md:145 covers only the mark and the icon PNGs, not the wordmark or the font.

**Remediation.** Add one sentence to ATTRIBUTION: the wordmark is original Jilaton artwork rasterised locally with the Windows Bahnschrift system font; no font file is redistributed.

### L2 — "User-stated permission" wording

**Evidence.** ATTRIBUTION.md:27, 31, 35, 37, 49 repeatedly rely on the user's stated permission for Anubis/LiquidBounce reuse. The Anubis archive is declared GPL-3.0-only, and Jilaton is GPL-3.0-only, so the licence already authorises adaptation; a verbal permission adds nothing and cannot be audited.

**Remediation.** Keep the GPL basis as the stated authority and describe the permission as context only; ensure the corresponding source for adapted behaviour is what Jilaton publishes (it is, via the sources jar/source zip).

### L3 — LiquidBounce reference cannot be re-verified

**Evidence.** No `*liquidbounce*.jar` exists in the checkout; only `JilatonClient/development/artifact-identities.json` and `JilatonClient/analysis/*.javap.txt` (ignored) record the cited hash `6bbf432c…f898`.

**Remediation.** For a release audit, record the absolute path and re-hash of the reference JAR in `development/artifact-identities.json`, or archive the minimal evidence (class-entry hash list) that does not require the JAR.

### L4 — Bundled PowerShell runs with ExecutionPolicy Bypass

**Evidence.** `music/MediaSessionSource.java:67-82` extracts `assets/jilaton/scripts/media-session.ps1` from the classpath and launches `powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass`. The script (5,681 B) is original, reads only Windows media-session metadata, and makes no network calls; ATTRIBUTION.md:157 covers it.

**Remediation.** None required for provenance; note it in the security assessment and consider a checksum comparison before execution. It is not reference-client material.

### L5 — Dev harness ships in the production jar

**Evidence.** The 20:45 jar has 391 `dev/**` entries including the `dev/jilaton/client/dev/` package and `VisualVerifier`; ORCHESTRATION.md:35-37 treats the verifier as a development harness.

**Remediation.** Exclude `dev/jilaton/client/dev/**` from the release jar via the `jar` task, or confirm it is intentionally shipped and document it.

## 5. Minimum gate before any publication

1. H1 resolved (files removed and unignored directory closed, or written permission + notices shipped).
2. M1 notices added and verified inside both jars.
3. M2 packaging exclusions applied and verified.
4. M3 a single artifact identity regenerated and recorded.
5. M4/M5 documentation and ignore fixes applied.
6. Re-run the jar-entry assertions (`package-development.py:24-28` plus a new `textures/sky` assertion) against the final artifact.

Until items 1–4 are done, the repository can be developed privately but its jars and source archives are not safe to publish under the current ATTRIBUTION wording.
