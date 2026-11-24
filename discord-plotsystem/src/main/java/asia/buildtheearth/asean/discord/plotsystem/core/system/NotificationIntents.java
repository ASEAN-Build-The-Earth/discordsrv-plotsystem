package asia.buildtheearth.asean.discord.plotsystem.core.system;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.system.events.PlotSystemEvents.NotificationType;
import com.alpsbte.plotsystem.api.event.PlotNotificationIntentEvent;
import com.alpsbte.plotsystem.api.model.PlotNotificationReason;
import com.alpsbte.plotsystem.api.model.PlotSnapshot;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Fires Plot-System's cancellable {@link PlotNotificationIntentEvent} before Discord delivery.
 */
public final class NotificationIntents {
    private NotificationIntents() {}

    public static boolean allow(@NotNull NotificationType type, int plotID) {
        return allow(toReason(type), plotID, null);
    }

    public static boolean allow(
            @NotNull PlotNotificationReason reason,
            int plotID,
            @Nullable PlotSnapshot plot
    ) {
        PlotNotificationIntentEvent intent = new PlotNotificationIntentEvent(plotID, reason, plot);
        if(Bukkit.isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(intent);
            return !intent.isCancelled();
        }

        DiscordPS plugin = DiscordPS.getPlugin();
        if(plugin == null || !plugin.isEnabled()) {
            return true;
        }

        CompletableFuture<Boolean> allowed = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Bukkit.getPluginManager().callEvent(intent);
                allowed.complete(!intent.isCancelled());
            }
            catch (Throwable error) {
                allowed.completeExceptionally(error);
            }
        });

        try {
            return allowed.get();
        }
        catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
        catch (ExecutionException execution) {
            Throwable cause = execution.getCause() == null ? execution : execution.getCause();
            if(cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Failed to dispatch plot notification intent", cause);
        }
    }

    public static @NotNull PlotNotificationReason toReason(@NotNull NotificationType type) {
        return switch (type) {
            case ON_CREATED -> PlotNotificationReason.CREATED;
            case ON_SUBMITTED -> PlotNotificationReason.SUBMITTED;
            case ON_REVIEWED -> PlotNotificationReason.REVIEWED;
            case ON_APPROVED -> PlotNotificationReason.APPROVED;
            case ON_REJECTED -> PlotNotificationReason.REJECTED;
            case ON_UNDO_REVIEW -> PlotNotificationReason.UNDO_REVIEW;
            case ON_UNDO_SUBMIT -> PlotNotificationReason.UNDO_SUBMIT;
            case ON_SHOWCASED -> PlotNotificationReason.SHOWCASED;
            case ON_ABANDONED -> PlotNotificationReason.ABANDONED;
            case ON_INACTIVITY -> PlotNotificationReason.INACTIVITY;
        };
    }
}
