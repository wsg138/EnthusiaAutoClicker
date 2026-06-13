# Enthusia AutoClicker for Minecraft 1.20.x

This standalone Gradle build produces Fabric and Forge jars for the Minecraft 1.20.x compatibility band (1.20.1+).

## Requirements

- Java 17
- Fabric: Fabric Loader and Fabric API
- Fabric configuration screen integration: Mod Menu is recommended
- Forge: no additional runtime dependency

## Build

Run `gradlew.bat build` on Windows or `./gradlew build` on Linux and macOS.

The loader-specific jars are written to `fabric/build/libs` and `forge/build/libs`.
