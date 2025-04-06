package github.tintinkung.discordps.core.api;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.events.Event;
import org.bukkit.plugin.Plugin;

public interface DiscordPlotSystem extends Plugin {

    String PS_UTIL = "com.alpsbte.plotsystem.utils.conversion.CoordinateConversion";
    String PS_UTIL_CONVERT_TO_GEO = "convertToGeo";
    String PS_UTIL_FORMAT_NUMERIC = "formatGeoCoordinatesNumeric";

    String PLOT_SYSTEM_SYMBOL = "Plot-System"; // PlotSystem main class symbol
    String DISCORD_SRV_SYMBOL = "DiscordSRV"; // DiscordSRV main class symbol



    boolean isDiscordSrvHookEnabled();
    boolean isPlotSystemHookEnabled();
    <E extends Event> E callEvent(E event);
}
