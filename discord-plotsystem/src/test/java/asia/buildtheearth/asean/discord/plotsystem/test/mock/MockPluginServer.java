package asia.buildtheearth.asean.discord.plotsystem.test.mock;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.database.ThreadStatus;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.function.Supplier;

/**
 * Interface to mock minecraft server lifecycle.
 *
 * @see #onServerMock()
 * @see #onServerStarted(ServerMock, DiscordSRV, DiscordPS)
 * @see #onServerStop()
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public interface MockPluginServer {

    /**
     * Invoked before every test triggering server start event with server instance and its initialized plugins.
     *
     * @see #onServerStarted(ServerMock, DiscordSRV, DiscordPS)
     */
    @BeforeAll
    default void onServerMock() {
        String classLoader = JavaPlugin.class.getProtectionDomain().getCodeSource().getLocation().getPath();

        Assertions.assertTrue(
                classLoader.matches(".*/io/papermc/paper/paper-api/.*"),
                "Expected 'JavaPlugin' to be loaded from 'io.papermc.paper:paper-api' artifact,\n" +
                        "but it was loaded from: " + classLoader + "\nPossible cause: " +
                        "spigot-api is not excluded in test scope and overrides paper-api due to classpath order."
        );

        // Initialize a mock server
        ServerMock server = Assertions.assertDoesNotThrow((ThrowingSupplier<ServerMock>) MockBukkit::mock,
                "MockBukkit should be able to mock the server using papermc-api"
        );

        Assertions.assertDoesNotThrow(() -> server.getLogger().info("[TEST] MockBukkit Server has started."));

        DiscordSRV discordSRV = Assertions.assertDoesNotThrow(() -> {
            server.getLogger().info("[TEST] ==========[ MOCK DiscordSRV START ]==========");
            server.getLogger().info("[TEST] Expecting a non-fatal error from MockDiscordSRV. This is normal.");

            // Load DiscordSRV plugin as a mock, should throw non-fatal exception on instantiation
            DiscordSRV plugin = MockBukkit.loadWith(MockDiscordSRV.class,
                new PluginDescriptionFile(
                    DiscordPS.DISCORD_SRV_SYMBOL,
                    DiscordPS.DISCORD_SRV_VERSION,
                    DiscordSRV.class.getName()
                ));

            server.getLogger().info("[TEST] End of expected error.");
            server.getLogger().info("[TEST] ==========[ MOCK DiscordSRV END ]============");

            return plugin;
        }, "Creating DiscordSRV mock instance should not throw fatal exception.");

        // DiscordSRV should exist in the plugin manager
        Assertions.assertNotNull(discordSRV);
        Assertions.assertNotNull(server.getPluginManager().getPlugin(DiscordPS.DISCORD_SRV_SYMBOL));

        // Load our plugin after DiscordSRV exist in plugin manager
        DiscordPS plugin = Assertions.assertDoesNotThrow(() -> MockBukkit.loadWith(MockDiscordPS.class,
                new PluginDescriptionFile(
                        MockDiscordPS.class.getSimpleName(),
                        DiscordPS.VERSION,
                        DiscordPS.class.getName()
                )
        ), "Creating DiscordPlotSystem mock instance should not throw fatal exception.");

        this.onServerStarted(server, discordSRV, plugin);
    }

    /**
     * Invoked after all tests un-mocking the mock-bukkit server by default.
     *
     * @see AfterAll
     * @see org.mockbukkit.mockbukkit.MockBukkit#unmock
     */
    @AfterAll
    default void onServerStop() {
        MockBukkit.unmock();
    }

    /**
     * Invoked on server started and all its instance is validated.
     *
     * @param server The {@link ServerMock} instance
     * @param discordSRV The {@link DiscordSRV} plugin instance
     * @param plugin The {@link DiscordPS} plugin instance
     */
    void onServerStarted(ServerMock server, DiscordSRV discordSRV, DiscordPS plugin);

    /**
     * Generate discord forum tag for the main forum channel.
     *
     * @param supplier Conventionally {@link DataObject#empty()}
     * @param index The index of the tag to generate, must be in the ordinal of {@link ThreadStatus#VALUES}
     * @return A data object representing discord forum tag
     */
    static @NotNull DataObject generateAvailableTag(@NotNull Supplier<DataObject> supplier, int index) {
        return supplier.get().put("id", index).put("name", ThreadStatus.VALUES[index]).put("moderated", true);
    }
}
