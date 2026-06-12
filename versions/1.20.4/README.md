# Enthusia AutoClicker for Minecraft 1.20.4

This standalone Gradle build produces Fabric, Forge, and NeoForge jars for Minecraft 1.20.4.

## Requirements

- Java 17
- Fabric: Fabric Loader and Fabric API
- Fabric configuration screen integration: Mod Menu is recommended
- Forge: no additional runtime dependency
- NeoForge: no additional runtime dependency

## Build

Run `gradlew.bat build` on Windows or `./gradlew build` on Linux and macOS.

The loader-specific jars are written to `fabric/build/libs`, `forge/build/libs`, and `neoforge/build/libs`.
