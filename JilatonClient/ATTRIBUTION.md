Based on Anubis Client by Horcus and 4ldenz.
Source repository claimed by supplied JAR: https://github.com/4ldenz/Anubis-client (unavailable/404 during review).
The checked-in recovery snapshot is CFR 0.152 output from the supplied Anubis 0.9.9.3 JAR, SHA-256 c5259f9ffee4bbe2511972f7941ab6fc2dcda691dc0d5ab18a558f3f8f4285de. It is not verified original source.

## LiquidBounce

The first-person swing transforms in `SwingAnimationsModule` are adapted from `SwingAnimations.kt`; the injection point is adapted from `MixinFirstPersonHandsAndItems.java`. Both originate from LiquidBounce, Copyright (c) 2015-2026 CCBlueX, licensed under GPL-3.0-or-later. Source snapshot: https://github.com/CCBlueX/LiquidBounce/tree/e033f5aaf05be802dd2ca51782f255119d7d5654. The Kotlin module/value/event framework was not copied; the animation math and mode behavior were translated to Java and integrated with Jilaton's own settings, config, UI, and module lifecycle.

`VelocityModule` adapts LiquidBounce's local-player velocity scaling behavior from `ModuleVelocity` / `VelocityModify` at the same source snapshot. Jilaton's packet mixin and settings integration are Java implementations in Jilaton's module lifecycle.

`AntiBotModule` adapts the `NotInTabList` target-filter behavior from LiquidBounce's `ModuleAntiBot` at the same GPL-3.0-or-later source snapshot. The filter is shared by KillAura, AimAssist, and Triggerbot through Jilaton's `PlayerTargetFilters` helper.

`NoSlowModule` and `NoSlowMixin` adapt LiquidBounce's item-use input-multiplier and sprint-while-using hooks from `ModuleNoSlow` / `MixinLocalPlayer`, under GPL-3.0-or-later at the source snapshot above.

`AimAssistModule` adapts target persistence and range/FOV hysteresis from the supplied Anubis 0.9.9.3 `AimAssistModule` decompilation, with the user's stated permission to reuse that code. The supplied reference snapshot is CFR output from JAR SHA-256 `c5259f9ffee4bbe2511972f7941ab6fc2dcda691dc0d5ab18a558f3f8f4285de`.

`CrystalAuraModule` and `CrystalDamageEstimator` adapt the target selection, crystal-base eligibility, conservative self-damage guard, explosion falloff, armor reduction, and exposure sampling approach from the supplied Anubis 0.9.9.3 `CrystalAuraModule`, `Crystals`, and `CrystalDamage` decompilation. The user stated they have permission for this reuse. The implementation is integrated into Jilaton's Java module/settings/config system and is not a wholesale copy. Reference JAR SHA-256: `c5259f9ffee4bbe2511972f7941ab6fc2dcda691dc0d5ab18a558f3f8f4285de`.

`WTapModule` adapts Anubis's `WTapRecodeModule` attack lifecycle by recording a qualifying sprint hit before the client attack and applying the sprint release only after the attack call completes. The surrounding mixin, state cleanup, and Jilaton settings/config integration are Jilaton Java code. Anubis is GPL-3.0; supplied JAR SHA-256: `c5259f9ffee4bbe2511972f7941ab6fc2dcda691dc0d5ab18a558f3f8f4285de`.
