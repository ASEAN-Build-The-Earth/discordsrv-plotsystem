package asia.buildtheearth.asean.discord.plotsystem.core.system.buttons;

import asia.buildtheearth.asean.discord.components.WebhookDataBuilder;
import asia.buildtheearth.asean.discord.components.api.*;
import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.ReviewComponent;
import github.scarsz.discordsrv.DiscordSRV;
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

import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Notification.ErrorMessage;

public class PlotFeedback implements PluginButtonHandler, SimpleButtonHandler {
    @Override
    public void onInteracted(@NotNull PluginButton button, @NotNull ButtonClickEvent event) {
        // Response
        event.deferReply(true).queue();

        String plotID = button.getPayload();

        try {
            Route.CompiledRoute route = Route.Interactions.CREATE_FOLLOWUP.compile(
                DiscordSRV.getPlugin().getJda().getSelfUser().getApplicationId(),
                event.getInteraction().getToken()
            ).withQueryParams("with_components", String.valueOf(true));

            WebhookDataBuilder.WebhookData webhookData = getFeedback(null, button.getIDLong());

            // Prepare the request action
            MultipartBody requestBody = webhookData.prepareRequestBody();
            RestAction<Object> action = new RestActionImpl<>(DiscordSRV.getPlugin().getJda(), route, requestBody);

            // Queue the action and handle for error
            action.submit().whenComplete((ok, error) -> {
                if(error == null) return;

                // Handle error if failed
                event.getHook().sendMessageEmbeds(getErrorEmbed()).queue();

                Notification.sendErrorEmbed(
                    ErrorMessage.PLOT_FEEDBACK_GET_EXCEPTION,
                    error.toString(),
                    plotID,
                    event.getUser().getId()
                );
            });
        } catch (SQLException ex) {
            // Reply owner that it failed
            event.deferReply(true).addEmbeds(getErrorEmbed()).queue();

            Notification.sendErrorEmbed(
                ErrorMessage.PLOT_FEEDBACK_GET_EXCEPTION,
                ex.toString(),
                plotID,
                event.getUser().getId()
            );
        }
    }

    public static @NotNull WebhookDataBuilder.WebhookData getFeedback(@Nullable String title, long feedbackID) throws SQLException {
        WebhookEntry entry = WebhookEntry.getByMessageID(feedbackID);

        if(entry == null || entry.feedback() == null)
            throw new SQLException("Trying to get feedback that does not exist in the database!");

        String feedbackRaw = entry.feedback();
        String entryID = Long.toUnsignedString(feedbackID);
        Optional<List<File>> reviewMedia = ReviewComponent.getOptMedia(entry.plotID(), entryID);
        ReviewComponent component = new ReviewComponent(
            feedbackRaw,
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
