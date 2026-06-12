# Enthusia AutoClicker for Minecraft 1.20.6

This standalone Gradle build produces Fabric and Forge jars for Minecraft 1.20.6.

## Requirements

- Java 21
- Fabric: Fabric Loader and Fabric API
- Fabric configuration screen integration: Mod Menu is recommended
- Forge: no additional runtime dependency

## Build

Run `gradlew.bat build` on Windows or `./gradlew build` on Linux and macOS.

The loader-specific jars are written to `fabric/build/libs` and `forge/build/libs`.
