# Release manifest

Audit record for the isolated build output. Written by E (HUD session) under RELEASE-MANIFEST-1 on
2026-10-31 00:06 and updated under RELEASE-GATE-1 after one forced full gate at 00:46. Only this file
was edited; no source file was changed.

## 1. Artefact identity — pin the hash, never the mtime

A jar's mtime says when a file was written; it says nothing about what is inside. Two builds can
produce different bytes with the same timestamp, and a copied file keeps its content while gaining a
new mtime. **A release must pin the SHA-256.** If any tool or note quotes only a size or a clock time,
treat it as unverified.

Measured history (`.isolated-build/root/libs/`). **Only the last row is the current baseline**; the
others are historical and every one of those files has been overwritten and no longer exists.

| Written | jar bytes | jar SHA-256 | sources bytes | sources SHA-256 | Gate read from XML |
| --- | --- | --- | --- | --- | --- |
| 22:55:26 (historical) | 8,342,055 | never captured — replaced before audit | — | — | 131 / 560 / 0 / 0 / 0 (historical) |
| 22:59:49 (historical) | 8,342,126 | `449bf3290ddfbe120836db3487a5c0d64e25fa40efb5b327e5e622eb0698e567` | 7,460,101 | `02def77be89190ae397bc073eb743aa4f1c7433b483789bfd9aa2288f9c8bc22` | 131 / 560 / 0 / 0 / 0 (historical) |
| 23:13:52 (historical, red) | 8,342,850 | `f88023f5a7b73eb6039e5ceee6fa0a96c8d24bea62c63c424fd3c2e48531ea33` | 7,460,526 | `298e4af3678df3b403e37dcabd1ba59cabb0c0774b73c85beedd882fb06275c6` | 139 / 604 / 2 / 0 / 0 (stale — see §4) |
| **00:46:46 (baseline)** | **8,348,477** | **`ab256bac4947df09ac7c1ebc90a4983feb41ff7ff471b20bee10e77212c81e7c`** | **7,464,560** | **`f93607e494ce648adf51b874e5df5de175a82c73febb5824705b8c67de1ca4c6`** | **144 / 622 / 0 / 0 / 0 (green)** |

Anything that quotes 8,342,055 B / 22:55 as "the release" is quoting a file that cannot be
re-verified. The 00:46 run was forced (`--rerun-tasks`) with no wrapper live and no jar write for
over an hour before it started, and every source file predates its XML timestamps (newest source
write 23:58), so it is a verdict on the current tree.

If a jar changes under you mid-audit: wait until
`wmic process where "name='java.exe'" get commandline | grep -c gradle-wrapper` returns 0, then
`stat` and hash again until two consecutive hashes match. Never carry a hash across a write.

## 2. The gate, one command

From `JilatonClient/`:

```bash
JAVA_HOME=$PWD/../analysis/toolchains/temurin-21/jdk-21.0.12.1+1 \
  ./gradlew --init-script ../.git/snapshots/isolated-build.gradle test build --console=plain --rerun-tasks
```

`--rerun-tasks` is not optional for a release verdict: without it Gradle may reuse a UP-TO-DATE
`test` task from a different tree state. Use the same command without it only for a quick
incremental check, and never quote that result as a gate.

Preconditions, all of which have bitten this project before:

- Run it only when no other wrapper is live (check command above). `.isolated-build/` is shared by
  every session; two runs at once corrupt each other's classpath and have produced `EOFException`
  and phantom failures.
- Never `./gradlew clean`. If a task must re-run, use `--rerun-tasks`.
- Copy the `TEST-*.xml` files out immediately after the run. The directory has been deleted by a peer
  session between a successful finish and the moment it was read.

## 3. Expected artefact paths

- `.isolated-build/root/test-results/test/TEST-*.xml`
- `.isolated-build/root/libs/jilaton-client-0.1.0.jar`
- `.isolated-build/root/libs/jilaton-client-0.1.0-sources.jar`

There is **no `build/` segment**. `build/` belongs to the user's live dev client and must not be used
for gate artifacts.

## 4. Expected counts

Sum the `tests`, `failures`, `errors`, `skipped` attributes of every `TEST-*.xml`:

```bash
python - <<'PY'
import glob, xml.etree.ElementTree as ET
s=t=f=e=sk=0
for p in glob.glob('.isolated-build/root/test-results/test/TEST-*.xml'):
    r=ET.parse(p).getroot(); s+=1
    t+=int(r.get('tests',0)); f+=int(r.get('failures',0))
    e+=int(r.get('errors',0)); sk+=int(r.get('skipped',0))
print(s,t,f,e,sk)
PY
```

- **Current baseline (00:46 forced run):** **144 suites / 622 tests / 0 failures / 0 errors /
  0 skipped**, `BUILD SUCCESSFUL`, pipe status 0.
- Historical only: 131 / 560 / 0 / 0 / 0 at 22:59. Do not quote 560.
- **A release requires 0 failures and 0 errors**, all XML newer than the last source write.
- **Any count not measured after the last source write is invalid.** A number is evidence only for
  the tree as it stood when the run started; the tree has moved 560 → 604 → 622 tests in one hour.

## 5. Fabric metadata (verified on the 00:46 jar, hash above)

| Field | Value | Verdict |
| --- | --- | --- |
| `id` | `jilaton` | OK |
| `version` | `0.1.0` (`${version}` expanded by the build) | OK |
| `name` | `Jilaton Client` | OK |
| `license` | `GPL-3.0-only` | OK |
| `authors` | `["Osayeid Zaber"]` — present in source and in the jar | OK |
| `contact` | absent | OK — **optional** |
| `entrypoints.client` | `dev.jilaton.client.JilatonClient` — class present | OK |
| `entrypoints.main` | not declared | OK for a client-only mod |
| `mixins` | `jilaton.mixins.json` present and parses; 73/73 listed classes present; `refmap` `jilaton.refmap.json` present; `compatibilityLevel` `JAVA_21` | OK |
| `icon` | `assets/jilaton/brand/icon-128.png`, 2,345 B | OK |
| `depends` | `fabricloader >=0.16.0` (built against 0.20.1), `minecraft ~1.21.11`, `java >=21`, `fabric-api *` | sane |

`contact` is **optional** in the Fabric mod schema: the only required keys are `schemaVersion`, `id`
and `version`, and `contact` (when present, a URL string or an object of name → URL) has no required
subfields. Omitting it therefore does not block loading, and leaving it out is preferable to
inventing a URL.

The mixin config inside the jar is not byte-identical to `src/main/resources/jilaton.mixins.json`;
the parsed JSON is identical, so the difference is Loom's normalisation, not divergence.
`fabric.mod.json` in the jar is 548 B against 553 B in source, accounted for by `${version}`
expansion.

## 6. Shipping hygiene, re-checked by hash

On the 00:46 baseline jar (677 entries, all unique):

- No nested jar, no `META-INF/jars`, no duplicate paths, no leading `/`, no `..`, no case collisions.
- No entry matching `remap-anubis`, `/analysis/`, `liquidbounce` or `anubis` (case-insensitive).
- **Dev-only classes do ship:** `dev/jilaton/client/dev/VisualVerifier.class` 38,908 B and
  `VisualVerifier$Step.class` 1,709 B, 40,617 B total. The class is inert unless
  `-Djilaton.visualVerify` is set (`VisualVerifier.java:60-61`) and nothing enters through an
  entrypoint, so it is not a leak. It is still a harness in a user-facing artefact; exclude it from
  the public release jar (`jar { exclude 'dev/jilaton/client/dev/**' }` or equivalent) or accept it
  deliberately.

Bundled art, byte-identical to source in both audits:

| Entry | Bytes | CRC32 |
| --- | --- | --- |
| `assets/jilaton/characters/cj.anc` | 2,209,592 | `d25d53aa` |
| `assets/jilaton/characters/neighbor.anc` | 5,038,471 | `993a6875` |
| `assets/jilaton/textures/sky/day.png` | 2,186,889 | `23fd3fa5` |
| `assets/jilaton/textures/sky/mask.png` | 127,997 | `5098d7f2` |
| `assets/jilaton/textures/sky/mask_moon.png` | 3,129 | `2eb7fc60` |
| `assets/jilaton/textures/sky/night.png` | 762,264 | `106daa2d` |
| `assets/jilaton/textures/sky/stars.png` | 166,496 | `11e8574e` |
| `assets/jilaton/textures/sky/sun.png` | 1,885,950 | `b43683fd` |
| `assets/jilaton/textures/sky/sunflare.png` | 17,496 | `ce6cb9c1` |

## 7. Attribution delta

The character port added **67 files**: 59 Java sources under `render/character/**` (30 `kit/`,
9 `gltf/`, 7 `designs/`, 13 at the package root) and 8 mixins (5 `MixinCharacter*`, 3 `AccessorHand*`).

`ATTRIBUTION.md:7-13` already credits Anubis (Horcus / 4ldenz, GPL-3.0, JAR SHA-256
`c5259f9f…4285de`) and names the `kit/`, `gltf/` and seven `designs/` classes, the `.anc` assets and
sizes, the render-layer integration, and Jilaton's own `CharacterModule`/catalog work.

- All eight mixins are Anubis ports (each has a same-named source under
  `analysis/anubis-vineflower/com/example/client/mixin/`), so the `AccessorHand*` files are covered by
  the existing "render-layer integration" sentence.
- Local citations spot-checked: `music/MediaSessionSource`, `render/fx/SkyPackTextures`,
  `assets/jilaton/scripts/media-session.ps1`, `tools/generate-brand-icon.py`,
  `tools/generate-loading-logo.py`, `development/render-ports.json`, `assets/jilaton/brand/mark.svg`,
  `assets/jilaton/textures/brand/loading-logo.png`, `analysis/liquidbounce-click-patterns.javap.txt`
  all exist. The remaining backticked tokens are upstream LiquidBounce/Anubis paths and `<gameDir>`
  placeholders, not local files.
- **No missing credit found.** `ATTRIBUTION.md` was not edited; this is reported to O.

## 8. What this manifest does not prove

Identity and structure only. Nothing here is runtime evidence: no frame has been inspected, so the
tree remains **BUILD_VERIFIED**, not RUNTIME_VERIFIED. A matching hash proves you have the same bytes
someone audited; it does not prove those bytes behave correctly in a client.
