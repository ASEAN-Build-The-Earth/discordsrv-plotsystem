package asia.buildtheearth.asean.discord.plotsystem.core.system;

import asia.buildtheearth.asean.discord.plotsystem.ConfigPaths;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.system.events.PlotSystemEvents.InactivityNoticeEvent;
import com.alpsbte.plotsystem.api.event.PlotAbandonedEvent;
import com.alpsbte.plotsystem.api.event.PlotActivityChangedEvent;
import com.alpsbte.plotsystem.api.event.PlotArchivedEvent;
import com.alpsbte.plotsystem.api.event.PlotStatusChangedEvent;
import com.alpsbte.plotsystem.api.model.PlotSnapshot;
import com.alpsbte.plotsystem.api.model.PlotStatus;
import com.alpsbte.plotsystem.api.provider.PlotSnapshotProvider;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

/**
 * Owns Discord reminder schedules and reconciles them with Plot-System snapshots.
 */
public final class InactivityReminderManager implements Listener, AutoCloseable {
    private static final long TICK_PERIOD_TICKS = 20L * 60L;
    private static final long RECONCILE_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(6);

    private final DiscordPS plugin;
    private final Path storageFile;
    private final Object persistenceLock = new Object();
    private final Map<Integer, Reminder> reminders = new HashMap<>();
    private final Set<Integer> pendingDeliveries = new HashSet<>();
    private final ConcurrentMap<Integer, Long> retryAfter = new ConcurrentHashMap<>();
    private PlotSnapshotProvider provider;
    private BukkitTask task;
    private long revision;
    private volatile long lastReconciliation;
    private volatile String lastInvalidSettings;
    private volatile boolean providerUnavailableReported;
    private volatile boolean closed;

    public InactivityReminderManager(@NotNull DiscordPS plugin) {
        this.plugin = plugin;
        this.storageFile = plugin.getDataFolder().toPath().resolve("inactivity-reminders.yml");
    }

    public void start() {
        loadState();
        this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin,
            this::tick,
            20L,
            TICK_PERIOD_TICKS
        );
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> reconcile());
    }

    public void refresh() {
        synchronized(this) {
            lastReconciliation = 0;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> reconcile());
    }

    @EventHandler
    public void onPlotActivityChanged(@NotNull PlotActivityChangedEvent event) {
        updateSnapshot(event.getPlot());
    }

    @EventHandler
    public void onPlotStatusChanged(@NotNull PlotStatusChangedEvent event) {
        updateSnapshot(event.getPlot());
    }

    @EventHandler
    public void onPlotAbandoned(@NotNull PlotAbandonedEvent event) {
        updateSnapshot(event.getPlot());
    }

    @EventHandler
    public void onPlotArchived(@NotNull PlotArchivedEvent event) {
        updateSnapshot(event.getPlot());
    }

    private void tick() {
        if(closed) return;

        PlotSnapshotProvider snapshots = getProvider();
        if(snapshots == null) return;

        long now = System.currentTimeMillis();
        if(lastReconciliation == 0 || now - lastReconciliation >= RECONCILE_INTERVAL_MILLIS) {
            reconcile(snapshots);
        }

        Settings settings = getSettings();
        if(settings == null || plugin.getWebhook() == null) return;

        List<Reminder> due;
        synchronized(this) {
            due = reminders.values().stream()
                .filter(reminder -> !reminder.sent() && reminder.notifyAt().toEpochMilli() <= now)
                .toList();
        }

        for(Reminder reminder : due) {
            Long retryTime = retryAfter.get(reminder.plotID());
            if(retryTime != null && retryTime > now) continue;
            checkAndDeliver(snapshots, reminder, settings);
        }
    }

    private void checkAndDeliver(
            @NotNull PlotSnapshotProvider snapshots,
            @NotNull Reminder scheduled,
            @NotNull Settings settings
    ) {
        final Optional<PlotSnapshot> currentSnapshot;
        try {
            currentSnapshot = snapshots.getPlotSnapshot(scheduled.plotID());
        }
        catch (RuntimeException ex) {
            DiscordPS.error("Failed to refresh Plot-System snapshot for inactivity reminder (plot "
                + scheduled.plotID() + ")", ex);
            retryAfter.put(scheduled.plotID(), System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));
            return;
        }

        if(currentSnapshot.isEmpty()) {
            removeReminder(scheduled.plotID());
            return;
        }

        updateSnapshot(currentSnapshot.get(), settings);

        Reminder current;
        synchronized(this) {
            current = reminders.get(scheduled.plotID());
            if(current == null || current.sent() || !current.matches(scheduled)
                    || current.notifyAt().toEpochMilli() > System.currentTimeMillis()
                    || !pendingDeliveries.add(scheduled.plotID())) {
                return;
            }
        }

        InactivityNoticeEvent notice = new InactivityNoticeEvent(
            current.plotID(),
            current.lastActivityAt(),
            current.abandonmentAt(),
            settings.days()
        );

        try {
            plugin.getWebhook().sendInactivityReminder(notice).whenComplete((_, error) ->
                completeDelivery(scheduled, error)
            );
        }
        catch (RuntimeException ex) {
            completeDelivery(scheduled, ex);
        }
    }

    /**
     * Finish a delivery attempt. {@code null} error means a permanent outcome
     * (Discord accepted the message, or send was intentionally skipped); only
     * exceptional failures are retried.
     */
    private void completeDelivery(@NotNull Reminder scheduled, @Nullable Throwable error) {
        synchronized(this) {
            pendingDeliveries.remove(scheduled.plotID());
            // Permanent outcomes (delivered or intentionally skipped) must not retry.
            // sendInactivityReminder returns false for disabled/cancelled/untracked plots.
            if(error == null) {
                Reminder current = reminders.get(scheduled.plotID());
                if(current != null && current.matches(scheduled)) {
                    reminders.put(scheduled.plotID(), current.markSent());
                    revision++;
                    persistAsync();
                }
                retryAfter.remove(scheduled.plotID());
                return;
            }
            retryAfter.put(scheduled.plotID(), System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));
        }

        Throwable cause = error instanceof CompletionException && error.getCause() != null
            ? error.getCause() : error;
        DiscordPS.error("Failed to deliver inactivity reminder for plot " + scheduled.plotID(), cause);
    }

    private void reconcile() {
        PlotSnapshotProvider snapshots = getProvider();
        if(snapshots != null) reconcile(snapshots);
    }

    private void reconcile(@NotNull PlotSnapshotProvider snapshots) {
        Settings settings = getSettings();
        if(settings == null) return;

        final long startingRevision;
        final Map<Integer, Reminder> previous;
        synchronized(this) {
            startingRevision = revision;
            previous = Map.copyOf(reminders);
        }

        final List<PlotSnapshot> currentPlots;
        try {
            currentPlots = snapshots.getPlotSnapshots(PlotStatus.UNFINISHED);
        }
        catch (RuntimeException ex) {
            DiscordPS.error("Failed to enumerate Plot-System snapshots for inactivity reminder reconciliation", ex);
            return;
        }

        Map<Integer, Reminder> reconciled = new HashMap<>();
        for(PlotSnapshot plot : currentPlots) {
            toReminder(plot, settings, previous.get(plot.id())).ifPresent(reminder ->
                reconciled.put(reminder.plotID(), reminder)
            );
        }

        synchronized(this) {
            if(revision != startingRevision) return;
            reminders.clear();
            reminders.putAll(reconciled);
            retryAfter.keySet().retainAll(reconciled.keySet());
            lastReconciliation = System.currentTimeMillis();
            revision++;
            persistAsync();
        }
    }

    private void updateSnapshot(@NotNull PlotSnapshot plot) {
        Settings settings = getSettings();
        if(settings != null) updateSnapshot(plot, settings);
    }

    private synchronized void updateSnapshot(@NotNull PlotSnapshot plot, @NotNull Settings settings) {
        Reminder previous = reminders.get(plot.id());
        Optional<Reminder> updated = toReminder(plot, settings, previous);
        if(updated.isPresent()) reminders.put(plot.id(), updated.get());
        else reminders.remove(plot.id());
        retryAfter.remove(plot.id());
        revision++;
        persistAsync();
    }

    private Optional<Reminder> toReminder(
            @NotNull PlotSnapshot plot,
            @NotNull Settings settings,
            @Nullable Reminder previous
    ) {
        if(plot.status() != PlotStatus.UNFINISHED || plot.owner() == null) return Optional.empty();

        Instant lastActivityAt = getLastActivityAt(plot, settings.zone());
        if(lastActivityAt == null) return Optional.empty();

        Optional<Instant> notifyAt = calculateReminderAt(
            lastActivityAt,
            plot.abandonmentAt(),
            settings.days(),
            settings.time(),
            settings.zone()
        );
        if(notifyAt.isEmpty()) return Optional.empty();

        String ownerUUID = plot.owner().toString();
        boolean sent = previous != null
            && previous.ownerUUID().equals(ownerUUID)
            && previous.lastActivityAt().equals(lastActivityAt)
            && previous.sent();

        return Optional.of(new Reminder(
            plot.id(),
            ownerUUID,
            lastActivityAt,
            plot.abandonmentAt(),
            notifyAt.get(),
            sent
        ));
    }

    private static @Nullable Instant getLastActivityAt(@NotNull PlotSnapshot plot, @NotNull ZoneId zone) {
        if(plot.lastActivityAt() != null) return plot.lastActivityAt();
        LocalDate lastActivity = plot.lastActivity();
        return lastActivity == null
            ? null
            : lastActivity.atStartOfDay(zone).toInstant();
    }

    public static Optional<Instant> calculateReminderAt(
            @NotNull Instant lastActivityAt,
            @Nullable Instant abandonmentAt,
            int inactivityDays,
            @NotNull LocalTime notificationTime,
            @NotNull ZoneId zone
    ) {
        if(inactivityDays < 1) throw new IllegalArgumentException("inactivityDays must be positive");

        ZonedDateTime activity = lastActivityAt.atZone(zone);
        ZonedDateTime earliestReminder = activity.plusDays(inactivityDays);
        ZonedDateTime scheduled = activity.toLocalDate()
            .plusDays(inactivityDays)
            .atTime(notificationTime)
            .atZone(zone);

        if(scheduled.isBefore(earliestReminder)) scheduled = scheduled.plusDays(1);
        Instant notifyAt = scheduled.toInstant();
        if(abandonmentAt != null && notifyAt.isAfter(abandonmentAt)) return Optional.empty();
        return Optional.of(notifyAt);
    }

    private @Nullable Settings getSettings() {
        if(!plugin.getConfig().getBoolean(ConfigPaths.INACTIVITY_NOTIFICATION_ENABLED, true)) {
            lastInvalidSettings = null;
            return null;
        }

        String timeValue = plugin.getConfig().getString(ConfigPaths.INACTIVITY_NOTIFICATION_TIME, "12:00");
        int days = plugin.getConfig().getInt(ConfigPaths.INACTIVITY_NOTIFICATION_DAYS, 7);
        String timezone = plugin.getConfig().getString(ConfigPaths.INACTIVITY_NOTIFICATION_TIMEZONE, "");

        try {
            if(days < 1) throw new IllegalArgumentException("inactivity-notification-days must be at least 1");
            if(timeValue == null) throw new IllegalArgumentException("inactivity-notification-time is missing");
            LocalTime time = LocalTime.parse(timeValue);
            ZoneId zone = (timezone == null || timezone.isBlank())
                ? ZoneId.systemDefault()
                : ZoneId.of(timezone);
            lastInvalidSettings = null;
            return new Settings(days, time, zone);
        }
        catch (DateTimeParseException | IllegalArgumentException ex) {
            String message = "Invalid inactivity reminder settings: " + ex.getMessage();
            if(!message.equals(lastInvalidSettings)) DiscordPS.error(message);
            lastInvalidSettings = message;
            return null;
        }
    }

    private @Nullable PlotSnapshotProvider getProvider() {
        PlotSnapshotProvider current = provider;
        if(current == null) {
            current = PlotSnapshotLookup.findProvider();
            provider = current;
        }
        if(current == null && !providerUnavailableReported) {
            DiscordPS.error("Plot-System did not register its public plot snapshot provider; "
                + "inactivity reminders and manual plot lookups will be unavailable.");
            providerUnavailableReported = true;
        }
        else if(current != null) {
            providerUnavailableReported = false;
        }
        return current;
    }

    private void loadState() {
        if(!Files.exists(storageFile)) return;
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(storageFile.toFile());
        }
        catch (IOException | InvalidConfigurationException ex) {
            DiscordPS.error("Failed to load persisted inactivity reminders", ex);
            return;
        }

        ConfigurationSection section = yaml.getConfigurationSection("reminders");
        if(section == null) return;

        synchronized(this) {
            for(String key : section.getKeys(false)) {
                try {
                    int plotID = Integer.parseInt(key);
                    ConfigurationSection reminder = section.getConfigurationSection(key);
                    if(reminder == null) continue;
                    reminders.put(plotID, new Reminder(
                        plotID,
                        UUID.fromString(reminder.getString("owner")).toString(),
                        Instant.parse(reminder.getString("last-activity-at")),
                        parseInstant(reminder.getString("abandonment-at")),
                        Instant.parse(reminder.getString("notify-at")),
                        reminder.getBoolean("sent", false)
                    ));
                }
                catch (RuntimeException ex) {
                    DiscordPS.error("Ignoring invalid persisted inactivity reminder for plot " + key, ex);
                }
            }
        }
    }

    private static @Nullable Instant parseInstant(@Nullable String value) {
        return value == null ? null : Instant.parse(value);
    }

    private void persistAsync() {
        if(closed) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveState);
    }

    private void saveState() {
        synchronized(persistenceLock) {
            saveStateAtomically();
        }
    }

    private void saveStateAtomically() {
        YamlConfiguration yaml = new YamlConfiguration();
        Map<String, Object> serialized = new HashMap<>();
        synchronized(this) {
            reminders.forEach((plotID, reminder) -> {
                Map<String, Object> values = new HashMap<>();
                values.put("owner", reminder.ownerUUID());
                values.put("last-activity-at", reminder.lastActivityAt().toString());
                values.put("notify-at", reminder.notifyAt().toString());
                values.put("sent", reminder.sent());
                if(reminder.abandonmentAt() != null) {
                    values.put("abandonment-at", reminder.abandonmentAt().toString());
                }
                serialized.put(String.valueOf(plotID), values);
            });
        }
        yaml.createSection("reminders", serialized);

        Path temporaryFile = storageFile.resolveSibling(storageFile.getFileName() + ".tmp");
        try {
            Files.createDirectories(storageFile.getParent());
            Files.writeString(
                temporaryFile,
                yaml.saveToString(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
            try {
                Files.move(temporaryFile, storageFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporaryFile, storageFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException ex) {
            DiscordPS.error("Failed to persist inactivity reminders", ex);
        }
    }

    private synchronized void removeReminder(int plotID) {
        if(reminders.remove(plotID) != null) {
            retryAfter.remove(plotID);
            revision++;
            persistAsync();
        }
    }

    @Override
    public void close() {
        closed = true;
        if(task != null) task.cancel();
        saveState();
    }

    private record Settings(int days, LocalTime time, ZoneId zone) {}

    private record Reminder(
        int plotID,
        String ownerUUID,
        Instant lastActivityAt,
        @Nullable Instant abandonmentAt,
        Instant notifyAt,
        boolean sent
    ) {
        boolean matches(Reminder other) {
            return plotID == other.plotID
                && ownerUUID.equals(other.ownerUUID)
                && lastActivityAt.equals(other.lastActivityAt)
                && notifyAt.equals(other.notifyAt);
        }

        Reminder markSent() {
            return new Reminder(plotID, ownerUUID, lastActivityAt, abandonmentAt, notifyAt, true);
        }
    }
}
