package asia.buildtheearth.asean.discord.plotsystem.api.test.mock;

import asia.buildtheearth.asean.discord.plotsystem.api.DiscordPlotSystem;
import asia.buildtheearth.asean.discord.plotsystem.api.PlotCreateData;
import asia.buildtheearth.asean.discord.plotsystem.api.test.utils.UnimplementedOperationException;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * Mock a {@link org.bukkit.plugin.java.JavaPlugin} with all its implementations <i>unimplemented</i>
 * as we physically cannot instantiate the {@link asia.buildtheearth.asean.discord.plotsystem.api.DiscordPlotSystemAPI plugin} itself without proper class loader.
 *
 * <p>This class provides static mock information for one {@link PlotCreateData} to test the data provider.</p>
 *
 * <p>On load, it create a mock {@link Logger} to redirect all logging functions into it.</p>
 *
 * @see MockDiscordPlotSystemAPI
 */
public abstract class MockDiscordPlotSystemPlugin extends MockDiscordPlotSystemAPI implements DiscordPlotSystem {
    private final Logger mockLogger;
    protected final MockLogger logger;
    public static final int MOCK_PLOT_ID = 1;
    public static final String MOCK_UUID = "00000000-0000-0000-0000-000000000000";
    public static final PlotCreateData.PlotStatus MOCK_STATUS = PlotCreateData.PlotStatus.ON_GOING;
    public static final String MOCK_CITY_ID = "MockID";
    public static final String MOCK_COUNTRY = "MockCountry";
    public static final double[] MOCK_COORDINATES = new double[] { 0, 0 };
    public static final Map<Integer, PlotCreateData> MOCK_PROVIDER = Map.of(MOCK_PLOT_ID,
        new PlotCreateData(MOCK_PLOT_ID, MOCK_UUID, MOCK_STATUS, MOCK_CITY_ID, MOCK_COUNTRY, MOCK_COORDINATES)
    );

    /**
     * Load the plugin and initialize a mocked plugin logger.
     *
     * @see #onLoad()
     */
    public MockDiscordPlotSystemPlugin() {
        this.mockLogger = Logger.getLogger("MockLogger");

        // Clear all existing handlers
        for (Handler handler : this.mockLogger.getHandlers())
            this.mockLogger.removeHandler(handler);

        this.mockLogger.setUseParentHandlers(false);
        this.mockLogger.addHandler(this.logger = new MockLogger());

        this.onLoad();
    }

    /**
     * Enable the <i>Plugin</i> programmatically
     *
     * @see #onEnable()
     */
    public void enablePlugin() {
        this.onEnable();
    }

    public boolean isReady() {
        return true;
    }

    @NotNull
    @Override
    public Logger getLogger() {
        return this.mockLogger;
    }

    @Override
    public void onDisable() {
        throw new UnimplementedOperationException();
    }

    @Override
    public void onLoad() {
        throw new UnimplementedOperationException();
    }

    @Override
    public void onEnable() {
        throw new UnimplementedOperationException();
    }

    @NotNull
    @Override
    public File getDataFolder() {
        throw new UnimplementedOperationException();
    }

    @NotNull
    @Override
    public PluginDescriptionFile getDescription() {
        throw new UnimplementedOperationException();
    }

    @NotNull
    @Override
    public FileConfiguration getConfig() {
        throw new UnimplementedOperationException();
    }

    @Nullable
    @Override
    public InputStream getResource(@NotNull String s) {
        throw new UnimplementedOperationException();
    }

    @Override
    public void saveConfig() {
        throw new UnimplementedOperationException();
    }

    @Override
    public void saveDefaultConfig() {
        throw new UnimplementedOperationException();
    }

    @Override
    public void saveResource(@NotNull String s, boolean b) {
        throw new UnimplementedOperationException();
    }

    @Override
    public void reloadConfig() {
        throw new UnimplementedOperationException();
    }

    @NotNull
    @Override
    public PluginLoader getPluginLoader() {
        throw new UnimplementedOperationException();
    }

    @NotNull
    @Override
    public Server getServer() {
        throw new UnimplementedOperationException();
    }

    @Override
    public boolean isEnabled() {
        throw new UnimplementedOperationException();
    }

    @Override
    public boolean isNaggable() {
        throw new UnimplementedOperationException();
    }

    @Override
    public void setNaggable(boolean b) {
        throw new UnimplementedOperationException();
    }

    @Nullable
    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String s, @Nullable String s1) {
        throw new UnimplementedOperationException();
    }

    @Nullable
    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull String s, @Nullable String s1) {
        throw new UnimplementedOperationException();
    }

    @NotNull
    @Override
    public String getName() {
        throw new UnimplementedOperationException();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        throw new UnimplementedOperationException();
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        throw new UnimplementedOperationException();
    }
}
