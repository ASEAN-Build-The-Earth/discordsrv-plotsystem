# Discord — [Plot-System](https://github.com/ASEAN-Build-The-Earth/Plot-System) Integration w/ [DiscordSRV](https://github.com/DiscordSRV/DiscordSRV)
![Discord](https://img.shields.io/discord/690908396404080650?style=flat&logo=discord&label=Discord&labelColor=E0E3FF&color=5865F2)

[![Maven Central](https://img.shields.io/maven-central/v/asia.buildtheearth.asean.discord/discordsrv-plotsystem?label=Plugin)](https://central.sonatype.com/artifact/asia.buildtheearth.asean.discord/discord-plotsystem)
[![Maven Central](https://img.shields.io/maven-central/v/asia.buildtheearth.asean.discord/discordsrv-plotsystem?label=API)](https://central.sonatype.com/artifact/asia.buildtheearth.asean.discord/discord-plotsystem)
[![javadoc](https://javadoc.io/badge2/asia.buildtheearth.asean.discord/discordsrv-plotsystem/javadoc.svg)](https://javadoc.io/doc/asia.buildtheearth.asean.discord/discord-plotsystem-api)
[![GitHub license](https://img.shields.io/github/license/ASEAN-Build-The-Earth/discordsrv-plotsystem)](https://github.com/ASEAN-Build-The-Earth/discordsrv-plotsystem/blob/main/LICENSE)

# DiscordSRV Plot-System Extension

An extension plugin for [DiscordSRV](https://github.com/DiscordSRV/DiscordSRV) designed to integrate with the Build The Earth project's Plot-System.

---

## Requirements

- [DiscordSRV](https://modrinth.com/plugin/discordsrv) `v1.29.0`
- Java `21` or higher
- A connector plugin that uses the [API module](#api-usage)

> [!IMPORTANT]
> This plugin does not include built-in plot management. You must implement your own logic by creating a connector plugin using the provided API.
>
> An example of this implementation can be found in our [fork of Plot-System](https://github.com/ASEAN-Build-The-Earth/Plot-System)


## 📦 Downloads

The latest plugin JAR is available on the [Releases](https://github.com/ASEAN-Build-The-Earth/discordsrv-plotsystem/releases/) section of this repository.

---

## API Usage

Refer to the **API module** to integrate and implement support for this plugin.

See the [Javadocs for our API](https://javadoc.io/doc/asia.buildtheearth.asean.discord/discord-plotsystem-api) for details on using `DiscordPlotSystemAPI` and subscribing to plot events.

You must register a data provider through the API to supply plot information.  
See `DiscordPlotSystemAPI.registerProvider(...)` for integration details.

### 📚 Maven Artifacts

| Module         | Artifact ID                                               |
|----------------|-----------------------------------------------------------|
| Parent Project | `asia.buildtheearth.asean.discord:discordsrv-plotsystem`  |
| API Module     | `asia.buildtheearth.asean.discord:discord-plotsystem-api` |
| Plugin Module  | `asia.buildtheearth.asean.discord:discord-plotsystem`     |

Artifacts are published to [Maven Central](https://central.sonatype.com/search?q=asia.buildtheearth.asean.discord), so no custom repository is required.

### Add to `pom.xml`

```xml
<dependency>
    <groupId>asia.buildtheearth.asean.discord</groupId>
    <artifactId>discord-plotsystem-api</artifactId>
    <version>1.2.0</version>
    <scope>compile</scope>
</dependency>
```

> You may include the plugin module entirely (`discord-plotsystem`) to directly extend the plugin.

---