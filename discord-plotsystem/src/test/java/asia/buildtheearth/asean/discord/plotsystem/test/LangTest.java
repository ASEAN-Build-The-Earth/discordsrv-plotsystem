package asia.buildtheearth.asean.discord.plotsystem.test;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.LanguageFile;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification;
import asia.buildtheearth.asean.discord.plotsystem.test.mock.MockPluginServer;
import github.scarsz.discordsrv.DiscordSRV;
import org.junit.jupiter.api.*;
import org.mockbukkit.mockbukkit.ServerMock;

@DisplayName("Languages")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class LangTest implements MockPluginServer {

    protected static ServerMock server;
    protected static DiscordPS plugin;
    protected static github.scarsz.discordsrv.DiscordSRV discordSRV;

    @Override
    public void onServerStarted(ServerMock server, DiscordSRV discordSRV, DiscordPS plugin) {
        PluginTest.server = server;
        PluginTest.discordSRV = discordSRV;
        PluginTest.plugin = plugin;
    }

    @Test
    public void ErrorLangTest() {
        for(Notification.ErrorMessage message : Notification.ErrorMessage.values()) {
            LanguageFile.EmbedLang lang = DiscordPS.getSystemLang().getEmbed(message, null);

            Assertions.assertNull(lang.title(), message.getClass().getName() + " is expected to have null title.");
            Assertions.assertNotNull(lang.description(), message.getClass().getName() + " has null description lang.");
        }
    }

    @AfterAll
    @Override
    public void onServerStop() {
        MockPluginServer.super.onServerStop();
    }
}
