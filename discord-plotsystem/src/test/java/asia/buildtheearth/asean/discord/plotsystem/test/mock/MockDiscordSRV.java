package asia.buildtheearth.asean.discord.plotsystem.test.mock;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordReadyEvent;
import github.scarsz.discordsrv.dependencies.jda.api.AccountType;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.api.requests.Request;
import github.scarsz.discordsrv.dependencies.jda.internal.JDAImpl;
import github.scarsz.discordsrv.dependencies.jda.internal.entities.GuildImpl;
import github.scarsz.discordsrv.dependencies.jda.internal.entities.TextChannelImpl;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.Requester;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.config.AuthorizationConfig;
import org.jetbrains.annotations.NotNull;

import java.util.EventListener;
import java.util.concurrent.ScheduledExecutorService;

public class MockDiscordSRV extends DiscordSRV {
    public static final MockApiManager api = new MockApiManager();
    private Guild mainGuild = null;
    private TextChannel notification = null;
    public JDAImpl jda = null;
    public static DiscordSRV plugin;

    /**
     * Skip DiscordSRV initialization thread entirely.
     */
    @Override
    public void onEnable() {
        plugin = this;

        api.subscribe(new EventListener() {
            // Assume the plugin is ready on ready event
            @Subscribe @SuppressWarnings("unused")
            public void discordReadyEvent(DiscordReadyEvent ready) {

                // Mock a JDA instance
                JDAImpl jdaImpl = new JDAImpl(new AuthorizationConfig(java.util.UUID.randomUUID().toString())) {

                    @NotNull
                    @Override
                    public AccountType getAccountType() {
                        return AccountType.BOT;
                    }

                    @NotNull
                    @Override
                    public ScheduledExecutorService getRateLimitPool() {
                        return super.getRateLimitPool();
                    }

                    @Override
                    public Requester getRequester() {
                        return new Requester(this, this.authConfig) {
                            @Override
                            public <T> void request(Request<T> apiRequest) {
                                apiRequest.onSuccess(null);
                            }
                        };
                    }

                    @Override
                    public TextChannel getTextChannelById(long id) {
                        if (id == MockSnowflake.NOTIFICATION.getMockID())
                            return notification;
                        return new TextChannelImpl(MockSnowflake.getRandom(), (GuildImpl) getMainGuild());
                    }
                };

                mainGuild = new GuildImpl(jdaImpl, MockSnowflake.MAIN_GUILD.getMockID());
                notification = new MockNotification((GuildImpl) mainGuild);
                jda = jdaImpl;
                isReady = true;
            }
        });
    }

    public static DiscordSRV getPlugin() {
        return plugin;
    }

    @Override
    public void onDisable() {
        DiscordSRV.shuttingDown = true;
        DiscordSRV.isReady = false;
    }

    @Override
    public JDAImpl getJda() {
        return this.jda;
    }

    @Override
    public Guild getMainGuild() {
        return this.mainGuild;
    }
}
