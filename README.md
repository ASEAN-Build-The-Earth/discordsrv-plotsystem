# Discord — [Plot-System](https://github.com/ASEAN-Build-The-Earth/Plot-System) Integration w/ [DiscordSRV](https://github.com/DiscordSRV/DiscordSRV)
![Discord](https://img.shields.io/discord/690908396404080650?style=flat&logo=discord&label=Discord&labelColor=E0E3FF&color=5865F2)

[![Maven Central](https://img.shields.io/maven-central/v/asia.buildtheearth.asean.discord/discordsrv-plotsystem?label=Plugin)](https://central.sonatype.com/artifact/asia.buildtheearth.asean.discord/discord-plotsystem)
[![GitHub license](https://img.shields.io/github/license/ASEAN-Build-The-Earth/discordsrv-plotsystem)](https://github.com/ASEAN-Build-The-Earth/discordsrv-plotsystem/blob/main/LICENSE)

An extension plugin for [DiscordSRV](https://github.com/DiscordSRV/DiscordSRV) designed to integrate with the Build The Earth project's Plot-System.

> [!NOTE]
> Refer to the [Wiki](https://github.com/ASEAN-Build-The-Earth/discordsrv-plotsystem/wiki) page for detailed explanation of the system!

---

## Requirements

- [DiscordSRV](https://modrinth.com/plugin/discordsrv) `v1.29.0`
- [Plot-System](https://github.com/ASEAN-Build-The-Earth/Plot-System) `5.2.0` or compatible
- Java `25` or higher

> [!IMPORTANT]
> This plugin listens directly to lifecycle events published by Plot-System. It does not include plot-management logic.


## 📦 Downloads

The latest plugin JAR is available on the [Releases](https://github.com/ASEAN-Build-The-Earth/discordsrv-plotsystem/releases/) section of this repository.

---

## Plot-System API integration

The plugin compiles against Plot-System's `com.alpsbte:plot-system-api` and listens for its Bukkit lifecycle events. No separate connector or Discord Plot-System API artifact is required.

Manual fetch, archive-as-new-thread, and showcase commands retrieve current snapshots from Plot-System's service provider, including plots that existed before this plugin started. Snapshot coordinates are included in Discord posts when available. Plot archives from Plot-System (`PlotArchivedEvent`) update tracked Discord threads automatically; the slash archive command remains available for manual Discord-side archives.

Inactivity reminders are scheduled by this integration, not by Plot-System. Set `inactivity-notification-enabled` to toggle scheduling, `inactivity-notification-days` for the minimum full inactive days, `inactivity-notification-time` for the daily delivery clock time, and optionally `inactivity-notification-timezone` (IANA id; blank uses the JVM default). Delivery also requires `plot-notification.on-plot-inactivity: true`. The plugin persists schedules in `inactivity-reminders.yml`, updates them on activity/status/abandon/archive events, reconciles them against Plot-System snapshots at startup and periodically, and refreshes the snapshot immediately before sending. A Plot-System abandonment deadline limits a reminder from being scheduled after the plot would be abandoned; reminders can still be scheduled when Plot-System has no abandonment deadline.

Plot-System lifecycle events stay observe-only. Before Discord delivers a plot notification (including inactivity reminders and showcase), this plugin fires Plot-System's cancellable `PlotNotificationIntentEvent` so third parties can veto delivery without depending on a Discord API. Permanent disablement still belongs in `plot-notification.*` config toggles.

---