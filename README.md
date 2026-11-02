<div align="center">

<img src="docs/header.svg" alt="Jilaton" width="100%"/>

<a href="#-install">
  <img src="https://readme-typing-svg.demolab.com?font=JetBrains+Mono&weight=500&size=18&duration=2600&pause=900&color=F5F5F5&center=true&vCenter=true&width=720&lines=118+modules.+One+design+system.;GLSL+shader+backgrounds+in+the+menus.;3D+characters+that+finish+your+fights.;Built+for+Minecraft+1.21.11+on+Fabric." alt="What Jilaton is"/>
</a>

<br/>

<img src="https://img.shields.io/badge/MINECRAFT-1.21.11-0A0A0A?style=for-the-badge&labelColor=000000"/>
<img src="https://img.shields.io/badge/FABRIC-0.20.1-0A0A0A?style=for-the-badge&labelColor=000000"/>
<img src="https://img.shields.io/badge/JAVA-21-0A0A0A?style=for-the-badge&logo=openjdk&logoColor=white&labelColor=000000"/>
<img src="https://img.shields.io/badge/GLSL-SHADERS-0A0A0A?style=for-the-badge&labelColor=000000"/>
<img src="https://img.shields.io/badge/LICENSE-GPL--3.0-0A0A0A?style=for-the-badge&labelColor=000000"/>

<br/><br/>

<img src="docs/showcase.gif" alt="Jilaton in motion" width="100%"/>

</div>

<img src="docs/divider.svg" width="100%"/>

### `01 — THE IDEA`

Jilaton started as a personal project: what would a Minecraft client look like if it were designed like a
modern editorial website instead of a debug menu? The answer is one design system end to end. Heavy display
type sits against italic serif accents, with monospace section labels and hairline rules. Live GPU shaders run
behind the menus, and every hover, press and panel shares the same spring motion.

<img src="docs/divider.svg" width="100%"/>

### `02 — INTERFACE`

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/home-threads.jpg"/><br/><sub><b>HOME</b> · live shader background, kinetic wordmark, sliding navigation</sub></td>
    <td width="50%"><img src="docs/screenshots/loading.jpg"/><br/><sub><b>LOADING</b> · driven by real resource-loading progress</sub></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/clickgui.jpg"/><br/><sub><b>CLICKGUI</b> · numbered categories, live previews, light sweeps on hover</sub></td>
    <td><img src="docs/screenshots/clickgui-settings.jpg"/><br/><sub><b>SETTINGS</b> · only the switch toggles; the card opens settings</sub></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/theme.jpg"/><br/><sub><b>THEME</b> · seven presets, accent, motion, blur, GUI size</sub></td>
    <td><img src="docs/screenshots/pause.jpg"/><br/><sub><b>PAUSE</b> · real GPU blur with a live session panel</sub></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/hud.jpg"/><br/><sub><b>HUD</b> · widgets with a drag, snap and align editor</sub></td>
    <td><img src="docs/screenshots/music.jpg"/><br/><sub><b>MUSIC</b> · Spotify or local files with synced lyrics</sub></td>
  </tr>
</table>

<img src="docs/divider.svg" width="100%"/>

### `03 — IN THE WORLD`

<table>
  <tr>
    <td width="50%"><img src="docs/screenshots/finisher.jpg"/><br/><sub><b>FINISHER</b> · a 3D character finishes what you defeat, with a crowd and kill effects</sub></td>
    <td width="50%"><img src="docs/screenshots/xray.jpg"/><br/><sub><b>XRAY</b> · terrain fades so ores show through</sub></td>
  </tr>
  <tr>
    <td><img src="docs/screenshots/storage-esp.jpg"/><br/><sub><b>STORAGE ESP + BREADCRUMBS</b></sub></td>
    <td><img src="docs/screenshots/waypoints.jpg"/><br/><sub><b>WAYPOINTS</b> · beams and distance labels</sub></td>
  </tr>
</table>

<img src="docs/divider.svg" width="100%"/>

### `04 — WHAT'S INSIDE`

| | |
|---|---|
| **Shader backgrounds** | Dark Veil, Aurora, Threads, Beams, Galaxy and Liquid Chrome, written in GLSL for Minecraft's GUI pipeline. They follow the cursor. |
| **Presets** | Jilaton, Editorial, Cyber, Monochrome, Glass, Cinematic, Performance. Each one changes the shader, accent, motion, blur and quality together. |
| **Configs** | Profiles, clipboard import/export, per-module reset, and search that also matches setting names. |
| **Finisher** | Ground slam, Rising uppercut, Spin kick and Tung tung bonk, with twelve kill effects and a crowd of 3D characters. |
| **Cosmetics** | 3D characters, wings, halos, knives, swing animations and a view-model editor. |

<details>
<summary><b>Browse the modules</b></summary>
<br/>

**Combat** · KillAura, AimAssist, Triggerbot, Criticals, Velocity, CrystalAura, AnchorAura, AutoTotem, AutoArmor, AutoWeapon, AntiBot, Teams, Friends …<br/>
**Movement** · Sprint, Speed, Flight, Step, Spider, Sneak, SafeWalk, AutoWalk, LongJump, TargetStrafe, NoSlow …<br/>
**Player** · AutoTool, AutoFish, AutoEat, AutoRespawn, InventoryCleaner, ChestStealer, ChestCleaner, SpeedMine, Replenish …<br/>
**Visuals** · Finisher, TargetESP, StorageESP, BlockESP, XRay, Trajectories, Waypoints, Breadcrumbs, NoRender, Time Changer, Custom Sky, Wings, Halo, Swing Animations …<br/>
**Client** · HUD, Notifier, Macros, AutoReconnect, Music Center …

</details>

<img src="docs/divider.svg" width="100%"/>

### `05 — INSTALL`

1. Install [Fabric Loader](https://fabricmc.net/use/) **0.20.1 or newer** for Minecraft **1.21.11**.
2. Drop [Fabric API](https://modrinth.com/mod/fabric-api) and the Jilaton jar into `mods`.
3. Launch on Java 21 and press **Right Shift**.

```bash
# or build it yourself
cd JilatonClient && ./gradlew build   # → build/libs/
```

> Some modules change gameplay and may break a server's rules. Use them where they are allowed.

<img src="docs/divider.svg" width="100%"/>

### `06 — CREDITS`

Licensed under [GPL-3.0](LICENSE). Parts are adapted from [LiquidBounce](https://github.com/CCBlueX/LiquidBounce) and the
Anubis client (GPL-3.0, Horcus and 4ldenz); every source is listed in [ATTRIBUTION.md](JilatonClient/ATTRIBUTION.md).
Type: Inter, JetBrains Mono, Bricolage Grotesque and Instrument Serif (SIL OFL).

<div align="center">
<br/>
<sub><code>JL — VOL. 01</code> &nbsp;·&nbsp; made by <a href="https://github.com/Osayeidzaber">Osayeed</a></sub>
</div>
