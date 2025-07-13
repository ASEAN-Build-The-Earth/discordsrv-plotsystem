package asia.buildtheearth.asean.discord.plotsystem.core.system.buttons;

import asia.buildtheearth.asean.discord.components.WebhookDataBuilder;
import asia.buildtheearth.asean.discord.components.api.*;
import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.ReviewComponent;
import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.RestActionImpl;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.Route;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.database.WebhookEntry;
import asia.buildtheearth.asean.discord.plotsystem.core.system.Notification;
import asia.buildtheearth.asean.discord.components.buttons.PluginButton;
import asia.buildtheearth.asean.discord.components.buttons.PluginButtonHandler;
import asia.buildtheearth.asean.discord.components.buttons.SimpleButtonHandler;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotInteraction;
import github.scarsz.discordsrv.dependencies.okhttp3.MultipartBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.ErrorMessage;

public class PlotFeedback implements PluginButtonHandler, SimpleButtonHandler {
    @Override
    public void onInteracted(@NotNull PluginButton button, @NotNull ButtonClickEvent event) {
        // Response
        event.deferReply(true).queue();

        PlotFeedback.getFeedback(null, button.getIDLong()).thenCompose((webhookData -> {

            Route.CompiledRoute route = Route.Interactions.CREATE_FOLLOWUP.compile(
                    DiscordPS.getPlugin().getJDA().getSelfUser().getApplicationId(),
                    event.getInteraction().getToken()
            ).withQueryParams("with_components", String.valueOf(true));

            // Prepare the request action
            MultipartBody requestBody = webhookData.prepareRequestBody();
            RestAction<Void> action = new RestActionImpl<>(DiscordPS.getPlugin().getJDA(), route, requestBody);

            // Queue the request
            return action.submit();

        })).whenComplete((ok, error) -> {
            // Handle error if failed
            if(error == null) return;

            event.getHook().sendMessageEmbeds(getErrorEmbed()).queue();

            Notification.sendErrorEmbed(
                    ErrorMessage.PLOT_FEEDBACK_GET_EXCEPTION,
                    error.toString(),
                    button.getPayload(),
                    event.getUser().getId()
            );
        });
    }

    /**
     * Fetch feedback data from the given ID, then parse it to {@linkplain WebhookDataBuilder.WebhookData WebhookData}.
     *
     * @param title Optional title that will be displayed before the feedback embed.
     * @param feedbackID The feedback ID in the database.
     * @return Completable future that complete successfully if the given feedback ID exists and the feedback is not {@code null}.
     */
    @NotNull
    public static CompletableFuture<WebhookDataBuilder.WebhookData> getFeedback(@Nullable String title, long feedbackID) {

        CompletableFuture<WebhookEntry> action = CompletableFuture.supplyAsync(() -> {
            try {
                WebhookEntry entry = WebhookEntry.getByMessageID(feedbackID);

                if(entry == null || entry.feedback() == null)
                    throw new SQLException("Trying to get feedback that does not exist in the database!");

                return entry;
            } catch (SQLException ex) { throw new RuntimeException(ex); }
        });

        return action.orTimeout(60L, TimeUnit.SECONDS).thenApply(entry -> {

            String feedbackRaw = entry.feedback();
            String entryID = Long.toUnsignedString(feedbackID);
            Optional<List<File>> reviewMedia = ReviewComponent.getOptMedia(entry.plotID(), entryID);
            ReviewComponent component = new ReviewComponent(
                    Objects.requireNonNull(feedbackRaw),
                    null,
                    entry.status().toTag().getColor(),
                    reviewMedia.orElse(null)
            );

            Collection<ComponentV2> components = (title == null || StringUtils.isBlank(title))
                    ? Collections.singletonList(component.build())
                    : List.of(new TextDisplay(title), component.build());

            WebhookDataBuilder.WebhookData webhookData = new WebhookDataBuilder()
                    .setComponentsV2(components)
                    .forceComponentV2()
                    .suppressNotifications()
                    .suppressMentions()
                    .build();

            reviewMedia.ifPresent(files -> files.forEach(webhookData::addFile));

            return webhookData;
        });
    }

    private static @NotNull MessageEmbed getErrorEmbed() {
        return DiscordPS.getMessagesLang()
            .getEmbedBuilder(PlotInteraction.FEEDBACK_GET_FAILURE)
            .setColor(Constants.RED)
            .build();
    }

    @Override
    public void onInteractedBadOwner(@NotNull ButtonClickEvent event) {
        MessageEmbed notOwnerEmbed = DiscordPS.getMessagesLang()
                .getEmbedBuilder(PlotInteraction.INTERACTED_BAD_OWNER)
                .setColor(Constants.RED)
                .build();
        event.deferReply(true).addEmbeds(notOwnerEmbed).queue();
    }
}
