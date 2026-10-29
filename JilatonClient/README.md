# Jilaton Client

Jilaton Client is a Java/Fabric client in active development. The current build contains a lifecycle-aware module/settings system, persistent configuration with named profile storage, and a configurable HUD. Its searchable, scrollable ClickGUI now uses a dark navy palette, category counts, an enabled-only filter, compact/roomy module rows, expandable settings, module toggles, and Keybinds, Config Profiles, and Theme Studio pages. Theme Studio supports five Jilaton palettes and an editable HSB accent whose choice persists across launches and profiles; the HUD uses the same accent. The current modules are Sprint, Fullbright, AimAssist, AutoClicker, Triggerbot, AutoTotem, AutoArmor, and HUD. The user supplied Anubis 0.9.9.3 JAR was inspected only as a static reference; neither it nor the other supplied client JARs were executed or included in the build. The JAR has not yet been run in Minecraft.

Anubis source recovery is retained under `reference/` for attribution and investigation. It is not part of Jilaton's source set. The supplied JAR declares GPL-3.0-only; see `LICENSE` and `ATTRIBUTION.md`. Jilaton's new implementation does not copy recovered source or proprietary client assets.

The retained Anubis JAR and analysis artifacts remain outside the build source set. No untrusted JAR was executed.
