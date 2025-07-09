package asia.buildtheearth.asean.discord.plotsystem.core.system;

import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.Debug;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.WebhookProvider;
import github.scarsz.discordsrv.dependencies.jda.api.Permission;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.exceptions.ParsingException;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.RestActionImpl;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.Route;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Validator for the main {@linkplain  PlotSystemWebhook Plot-System webhook}.
 *
 * <p>The validation process is as follows:</p>
 * <ol>
 *     <li>Fetch the webhook guild if the webhook actually exist in the guild</li>
 *     <li>Check if this webhook is created by the bot itself (required by the system)</li>
 *     <li>Resolve status tags from config file (eg. name and color)</li>
 *     <li>Validate status tags if all of it exist by discord API</li>
 * </ol>
 */
public class PlotSystemWebhookValidator {
    private final WebhookProvider webhook;
    private final YamlConfiguration config;

    /**
     * Construct a new validator on a non-null webhook instance.
     *
     * @param webhook The webhook instance, must be {@linkplain PlotSystemWebhook}.
     * @param config The plugin configuration file used for resolving available tags.
     * @see PlotSystemWebhookValidator
     */
    public PlotSystemWebhookValidator(@NotNull PlotSystemWebhook webhook,
                                      @NotNull YamlConfiguration config) {
        this.webhook = webhook.getProvider();
        this.config = config;
    }

    /**
     * Validate this webhook integrity and output error/information to {@link asia.buildtheearth.asean.discord.plotsystem.Debug}.
     *
     * @param others other webhook to validate with within the same request call
     * @return Future that complete when all validation action is completed
     */
    public CompletableFuture<Void> validate(WebhookProvider... others) {
        List<CompletableFuture<?>> validateActions = this.validateWebhookReference(others);

        return CompletableFuture.allOf(validateActions.toArray(new CompletableFuture<?>[0])).handle((ok, error) -> {
            if (error != null) DiscordPS.error(Debug.Error.WEBHOOK_VALIDATION_UNKNOWN_EXCEPTION, error.toString());
            // If webhook reference validation failed, tag validation is guarantee to fails too
            if (DiscordPS.getDebugger().hasGroup(Debug.ErrorGroup.WEBHOOK_REFS_VALIDATION)) {
                DiscordPS.error(Debug.Error.WEBHOOK_TAG_VALIDATION_FAILED);
                return CompletableFuture.<Void>completedFuture(null);
            } // Else validate for it with API calls
            else return this.validateWebhookTags();
        }).thenCompose(Function.identity());
    }

    /**
     * Validate for webhook reference in guild,
     * this request a guild data for each guild the bot is in
     * then check the webhook existence in those guild.
     *
     * @param others Other webhook to be validated within the same API call
     * @return Futures of each guild validation action
     */
    private @NotNull List<CompletableFuture<?>> validateWebhookReference(WebhookProvider... others) {
        // Fetch database to update webhooks every session
        List<CompletableFuture<?>> validateActions = new ArrayList<>();

        for (Guild guild : this.webhook.getJDA().getGuilds()) {

            Member selfMember = guild.getSelfMember();

            if (!selfMember.hasPermission(Permission.MANAGE_WEBHOOKS)) {
                DiscordPS.error(
                    Debug.Error.WEBHOOK_CAN_NOT_MANAGE,
                    "Unable to manage webhooks guild-wide in " + guild
                    + ", please allow the permission MANAGE_WEBHOOKS"
                );
                continue;
            }

            validateActions.add(this.validateWebhooksInGuild(guild, selfMember, others));
        }

        return validateActions;
    }

    /**
     * Configure and resolved status tags does not have valid API reference yet,
     * this method fetch the tag reference from the webhook guild and apply tags base on configured name.
     *
     * @return Future to the validation action
     * @see AvailableTag#resolveAllTag(FileConfiguration)
     * @see AvailableTag#applyAllTag()
     */
    private CompletableFuture<Void> validateWebhookTags() {
        // Order matters here
        // 1st Resolve the tag enum to is corresponding config value
        this.resolveStatusTags();

        // 2nd validate its existence by fetching available tags from this webhook channel
        return this.getAvailableTags(true).submit().handle((optData, error) -> {
            if (error != null)
                DiscordPS.error(Debug.Error.WEBHOOK_TAG_VALIDATION_FAILED, error.getMessage(), error);
            else if (optData.isEmpty()) DiscordPS.error(Debug.Error.WEBHOOK_TAG_VALIDATION_FAILED,
                    "Failed to fetch webhook channel data to validate available tags because API response returned empty.");
            else {
                DataArray availableTags = optData.get();
                AvailableTag.initCache(availableTags);
                AvailableTag.applyAllTag();
            }

            return null;
        });
    }

    /**
     * Resolve all configured status tags from forum config file,
     * output statically as {@link AvailableTag} which resolve for tag color and snowflake id.
     *
     * @see AvailableTag#resolveAllTag(FileConfiguration)
     */
    private void resolveStatusTags() {
        // Resolve the config path into tag enum
        try {
            AvailableTag.resolveAllTag(this.config);
        } catch (NoSuchElementException ex) {
            DiscordPS.error(Debug.Error.WEBHOOK_TAG_CONFIGURATION_FAILED, ex);
        }
    }

    /**
     * Method only for webhook validation phase.
     *
     * <p>This fetch for available tags in this webhook channel and validate for its available forum tag(s)</p>
     *
     * @param allowSecondAttempt To retry on bad response
     * @return The request action that if success, return optional of raw data array.
     */
    @NotNull
    private RestAction<Optional<DataArray>> getAvailableTags(boolean allowSecondAttempt) {
        Route.CompiledRoute route = Route
                .get(Route.Channels.MODIFY_CHANNEL.getRoute())
                .compile(Long.toUnsignedString(this.webhook.getChannelID()));

        ForumWebhookImpl.RestResponse<DataArray> response = new ForumWebhookImpl.RestResponse<>(data -> {
            // Additional checks since this route does have channel data
            // Fail the validation if the channel isn't forum channel
            if (data.hasKey("type")) {
                int type = data.getInt("type");
                if (type != 15) {
                    DiscordPS.error(Debug.Error.WEBHOOK_CHANNEL_NOT_FORUM);
                    return null;
                }
            }

            if (!data.hasKey("available_tags")) {
                DiscordPS.error(Debug.Error.WEBHOOK_VALIDATION_UNKNOWN_EXCEPTION,
                    "Webhook channel does not have available tags data to verify, "
                    + "this might indicate breaking API change from discord.");
                return null;
            }

            return data.getArray("available_tags");
        });

        if (allowSecondAttempt) response.setRetryExecution(() -> getAvailableTags(false));

        return new RestActionImpl<>(this.webhook.getJDA(), route, response::execute);
    }

    /**
     * Fetch guild for webhook information and validate if our webhook exist in the guild,
     * this also check for all bot created webhook if it is sync to the configured one or not.
     *
     * @param guild The guild to check for webhook
     * @param bot   The plugin bot as discord member
     */
    private CompletableFuture<Void> validateWebhooksInGuild(@NotNull Guild guild,
                                                            @NotNull Member bot,
                                                            WebhookProvider... others) {

        Route.CompiledRoute route = Route.Guilds.GET_WEBHOOKS.compile(guild.getId());

        // Retrieve guild webhooks as raw data since this outdated api produce null value for newer webhook.
        return new RestActionImpl<List<DataObject>>(this.webhook.getJDA(), route, (response, request) -> {
            DataArray array = response.getArray();
            List<DataObject> webhooks = new ArrayList<>(array.length());

            for (int i = 0; i < array.length(); ++i) {
                try {
                    DataObject webhookObj = array.getObject(i);

                    webhooks.add(webhookObj);
                } catch (Exception e) {
                    DiscordPS.error("[Internal] Error creating webhook from json", e);
                }
            }

            return Collections.unmodifiableList(webhooks);
        }).submit().thenAccept(webhooks -> {
            boolean webhookExist = false;
            List<String> notRegistered = new ArrayList<>();

            for (DataObject webhook : webhooks) {
                try {
                    // Filter for bot created webhook only
                    DataObject user = webhook.getObject("user");

                    long userID = Long.parseUnsignedLong(user.getString("id"));

                    if (userID != bot.getIdLong()) continue;

                    // Check for configured webhooks
                    String name = webhook.getString("name");
                    String username = user.getString("username");

                    long id = Long.parseUnsignedLong(webhook.getString("id"));
                    long channel = Long.parseUnsignedLong(webhook.getString("channel_id"));

                    if (name.startsWith(Constants.DISCORD_SRV_WEBHOOK_PREFIX_LEGACY)
                            || name.startsWith(Constants.DISCORD_SRV_WEBHOOK_PREFIX))
                        continue;

                    if (id == this.webhook.getWebhookID() && channel == this.webhook.getChannelID()) {
                        DiscordPS.debug("Found configured webhook: " + name + ": " + username);
                        webhookExist = true;
                    } else {

                        boolean registered = false;

                        for (WebhookProvider other : others) {
                            if (id == other.getWebhookID() && channel == other.getChannelID()) {
                                DiscordPS.debug("Found configured webhook: " + name + ": " + username);
                                registered = true;
                                break;
                            }
                        }

                        if (!registered) {
                            // name:username(snowflake)
                            notRegistered.add(name + ":" + username + "(" + Long.toUnsignedString(id) + ")");
                            DiscordPS.warning("Found un-registered bot created webhook: " + name + ": " + username);
                        }
                    }
                } catch (ParsingException ex) {
                    DiscordPS.error("Fetching webhook: "
                        + "Exception occur trying to parse webhook data, "
                        + "maybe discord API updated?",
                    ex);
                }
            }

            if (!webhookExist)
                DiscordPS.error(Debug.Error.WEBHOOK_NOT_FOUND_IN_GUILD);

            if (!notRegistered.isEmpty()) DiscordPS.warning(
                Debug.Warning.WEBHOOK_NOT_REGISTERED_DETECTED,
                "Detected un-registered webhook created by this bot " + notRegistered
            );
        });
    }
}
