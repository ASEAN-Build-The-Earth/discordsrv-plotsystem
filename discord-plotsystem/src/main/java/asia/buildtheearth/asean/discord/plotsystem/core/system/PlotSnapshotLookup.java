package asia.buildtheearth.asean.discord.plotsystem.core.system;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.system.events.PlotSnapshotData;
import com.alpsbte.plotsystem.api.provider.PlotSnapshotProvider;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Shared access to Plot-System's registered {@link PlotSnapshotProvider}.
 */
public final class PlotSnapshotLookup {
    private PlotSnapshotLookup() {}

    public static @Nullable PlotSnapshotProvider findProvider() {
        return Bukkit.getServicesManager().load(PlotSnapshotProvider.class);
    }

    public static PlotSnapshotProvider requireProvider() {
        PlotSnapshotProvider provider = findProvider();
        if(provider == null) {
            throw new IllegalStateException("Plot-System did not register its snapshot provider");
        }
        return provider;
    }

    public static CompletableFuture<Optional<PlotSnapshotData>> get(int plotID) {
        CompletableFuture<Optional<PlotSnapshotData>> result = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(DiscordPS.getPlugin(), () -> {
            try {
                result.complete(requireProvider().getPlotSnapshot(plotID).map(PlotSnapshotData::from));
            }
            catch (RuntimeException ex) {
                result.completeExceptionally(ex);
            }
        });
        return result;
    }
}
