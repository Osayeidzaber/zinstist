<div align="center">

# Jilaton Client

**A configurable Minecraft Java client built with Fabric.**

Combat and movement modules, a searchable ClickGUI, and a customizable HUD — built in Java with an original dark interface.

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62b47a?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-e89b43?style=flat-square)
![Fabric](https://img.shields.io/badge/Fabric-0.20.1-dbd0b4?style=flat-square)
![Status](https://img.shields.io/badge/Status-In%20development-818cf8?style=flat-square)

[Get started](#get-started) · [Features](#features) · [Controls](#controls) · [Development status](#development-status) · [Documentation](#documentation)

</div>

---

Jilaton brings module settings, profiles, keybinds, theme controls, and HUD editing into one client interface. The project targets **Minecraft 1.21.11** and is under active development. Feature coverage and testing vary by module; the engineering checkpoint records what has actually been verified.

## Features

### Make the interface yours

- **Searchable ClickGUI** with categories, expandable settings, configurable keybinds, and saved profiles.
- **Custom title screen** with a dark theme, animated backdrop, and original vector J mark.
- **HUD editor** with draggable widgets and saved scale, alignment, accent, panel opacity, and visibility.
- **Gameplay widgets** for FPS/ping, coordinates, keystrokes, CPS, armor, potion effects, module activity, and target information.
- **Zoom** with hold/toggle activation, smoothing, scroll adjustment, and proportional mouse sensitivity.

### Configure how you play

| Area | Selected modules in the source |
| --- | --- |
| Combat and targeting | AimAssist, KillAura, BowAim, AutoShoot, Triggerbot, Criticals, CrystalAura, AutoAnchor, AnchorAura |
| Inventory and survival | AutoTotem, AutoArmor, AutoReplenish, AutoEat, AutoPot, AutoRod, AutoWeapon, AutoLeave |
| Movement | Sprint, Strafe, Speed, Step, SafeWalk, InventoryMove, HighJump, LongJump, Flight, NoSlow, FastFall |
| Visuals and client | Fullbright, Zoom, PlayerESP, ItemESP, Tracers, SwingAnimations, HUD |

These are source features, not a promise of complete mode parity or verified multiplayer behavior. See the [implementation inventory](JilatonClient/IMPLEMENTATION_INVENTORY.md) for behavior, dependencies, and remaining work.

### Keep your setup

Settings, module states, profiles, and friend names persist locally. With the Friends module enabled, middle-clicking a targeted player toggles their friend status. Friends receive a blue overhead hat marker and are excluded from combat targeting.

## Get started

### Requirements

- A **Java 21 JDK**, with `JAVA_HOME` set to its installation.
- Network access for Gradle and the first dependency download.
- Minecraft **1.21.11**, Fabric Loader, and Fabric API for running the built mod.

The build pins Fabric Loader **0.20.1**, Fabric API **0.141.6+1.21.11**, Loom **1.14.10**, and Gradle **9.8.2**. The Gradle wrapper is included; downloaded toolchains and caches are not.

### Clone and build

```sh
git clone https://github.com/Osayeidzaber/zinstist.git
cd zinstist/JilatonClient
```

**Windows / PowerShell**

```powershell
.\gradlew.bat test build --no-daemon
```

**Linux / macOS**

```sh
sh ./gradlew test build --no-daemon
```

Build output is written to `JilatonClient/build/libs/`. For the current version, use **`jilaton-client-0.1.0.jar`**; the `-sources.jar` contains source code.

### Run the mod

1. Set up a Minecraft **1.21.11** instance with Fabric Loader and the matching Fabric API.
2. Place the built Jilaton JAR and Fabric API JAR in that instance's `mods/` directory.
3. Launch the Fabric instance. Press **Right Shift** to open Jilaton's ClickGUI.

For a development instance, run from `JilatonClient/`:

```powershell
.\gradlew.bat runClient --no-daemon
```

On Linux/macOS, use `sh ./gradlew runClient --no-daemon`. The development instance uses the local `run/` directory. An offline development profile cannot authenticate with Realms or authenticated multiplayer services.

## Controls

| Action | Default control or command |
| --- | --- |
| Open ClickGUI | **Right Shift** or `/jilaton gui` |
| Open HUD editor | `/jilaton hud` |
| Toggle a module | `/jilaton toggle <module_id>`; module IDs have suggestions |
| Zoom | Hold **C** by default; configure hold/toggle behavior in settings |
| Toggle a friend | **Middle-click** a targeted player while Friends is enabled |

In the HUD editor, drag widgets to position them. Keyboard controls:

| Key | Action |
| --- | --- |
| **Tab** | Select a widget |
| **[ / ]** | Adjust scale |
| **− / =** | Adjust panel opacity |
| **C** | Change accent |
| **A** | Change alignment |
| **H** | Toggle visibility |
| **R** | Reset the selected widget |
| **M** | Toggle reduced motion |

## Development status

**This is a development build.** Module parity, rendering coverage, and gameplay acceptance are still in progress.

The recorded **October 30, 2026 checkpoint** reports **259 automated tests passing**, with zero failures, errors, or skips. It also records a successful launch into a local Creative world and focused checks of the title screen, ClickGUI, and HUD editor. Those results do not establish complete gameplay or multiplayer compatibility, and they are recorded results rather than a live CI status.

Open work includes detailed combat and movement testing, inventory/action conflict review, Zoom behavior checks, fuller rendering coverage, and additional UI and persistence checks. Read the [checkpoint](JilatonClient/development/CHECKPOINT.md) for the exact scope and limitations.

## Repository layout

```text
zinstist/
├── README.md
├── .gitignore
├── SECURITY_ASSESSMENT.md
└── JilatonClient/
    ├── src/main/java/       # Client, modules, GUI, configuration, and Mixins
    ├── src/main/resources/  # Fabric metadata, assets, and Mixin configuration
    ├── src/test/java/       # Automated tests
    ├── gradle/wrapper/      # Pinned Gradle wrapper
    ├── development/         # Checkpoint and machine-readable inventories
    ├── tools/               # Inventory and packaging helpers
    ├── build.gradle
    ├── gradle.properties
    ├── LICENSE
    └── ATTRIBUTION.md
```

Git excludes downloaded JDK/Gradle distributions, dependency caches, build artifacts, logs, game saves, local configurations, and reference-analysis directories. These can remain on the developer's computer without being uploaded. Reference-client JARs and recovered sources are not build dependencies.

## Documentation

| Document | What it covers |
| --- | --- |
| [Build guide](JilatonClient/BUILDING.md) | Requirements, dependency resolution, and build output |
| [Engineering checkpoint](JilatonClient/development/CHECKPOINT.md) | Verified checks, limitations, and next steps |
| [Implementation inventory](JilatonClient/IMPLEMENTATION_INVENTORY.md) | Module behavior and implementation depth |
| [Integration manifest](JilatonClient/development/integration-manifest.json) | Machine-readable source and integration records |
| [Contributor guide](JilatonClient/agent.md) | Architecture, implementation, and verification rules |
| [Security assessment](JilatonClient/SECURITY_ASSESSMENT.md) | Artifact provenance and review boundaries |
| [Attribution](JilatonClient/ATTRIBUTION.md) | Sources, adaptations, and licensing notices |

## Contributing

Keep changes focused and preserve the existing Java/Fabric architecture. Use the shared module lifecycle, configuration, input, rotation, and rendering systems. Add meaningful tests for behavior changes, run the relevant checks, and distinguish automated results from behavior observed in Minecraft.

Before contributing, read the [project guide](JilatonClient/agent.md) and [current checkpoint](JilatonClient/development/CHECKPOINT.md). Include the affected module, expected behavior, actual behavior, and reproduction steps when reporting an issue.

## License and credits

Jilaton's Fabric metadata declares **GPL-3.0-only**; see [LICENSE](JilatonClient/LICENSE). The project credits Anubis Client by **Horcus and 4ldenz** and documents adaptations from **LiquidBounce**, including GPL-3.0-or-later obligations, in [ATTRIBUTION.md](JilatonClient/ATTRIBUTION.md). Preserve the applicable notices when modifying or redistributing the project.
