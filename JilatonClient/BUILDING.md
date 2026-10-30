# Build Jilaton from source

Use a trusted Java 21 JDK and Gradle 9.8.2. Set `JAVA_HOME` to the JDK installation, then run from this directory:

```text
gradle test build --no-daemon
```

Gradle resolves the pinned Minecraft/Fabric/Loom dependencies from the repositories declared in `settings.gradle` and `build.gradle`. The first build needs network access to those repositories. Once dependencies and Minecraft assets are cached, `gradle --offline test build --no-daemon` works. No reference-client JAR is a build dependency; recovered sources outside `src/` are excluded from compilation.

The remapped client and sources JARs are written to `build/libs/`. `gradle runClient --no-daemon` launches the development profile in `run/`; use a separate local test world. An offline development profile cannot authenticate with Realms or authenticated multiplayer services. Logs from that profile do not establish multiplayer compatibility.

The source archive includes the complete Jilaton `src/` tree, build definitions, license and attribution. It deliberately omits local Gradle caches, JDK/Gradle binaries, downloaded reference clients, personal configurations, saved worlds and analysis binaries. Supply your own trusted JDK and Gradle installation.

This is a development build. See `development/CHECKPOINT.md` for known gaps and the checks that actually ran.
