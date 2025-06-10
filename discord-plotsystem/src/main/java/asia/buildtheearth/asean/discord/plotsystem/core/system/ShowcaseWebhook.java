package asia.buildtheearth.asean.discord.plotsystem.core.system;

import asia.buildtheearth.asean.discord.components.WebhookDataBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.database.WebhookEntry;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.WebhookProvider;
import asia.buildtheearth.asean.discord.components.api.Container;
import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.ShowcaseComponent;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Special webhook specifically to showcase a completed plot to a configured forum.
 *
 * @see #showcasePlot(PlotData, WebhookEntry)
 */
public final class ShowcaseWebhook extends AbstractShowcaseWebhook {
    /**
     * Initialize webhook instance
     *
     * @param webhook The forum webhook manager instance
     */
    public ShowcaseWebhook(ForumWebhook webhook) {
        super(webhook);
    }


    /**
     * Get the provider of this webhook which
     * provides webhook as references and validation method.
     *
     * @return The provider as interface
     */
    public WebhookProvider getProvider() {
        return this.webhook.getProvider();
    }

    /**
     * Showcase a plot entry using plot data
     *
     * @param plotData The plot data information to showcase
     * @param plotEntry The tracked entry of this plot data
     * @return Queued future which return a non-empty message reference
     *         to the showcased message if successful
     */
    @NotNull
    public CompletableFuture<Optional<MessageReference>>
    showcasePlot(@NotNull PlotData plotData, @NotNull WebhookEntry plotEntry) {
        // Safety check if plot entry and webhook entry is as the same owner
        if(!plotData.getOwner().getUniqueId().equals(UUID.fromString(plotEntry.ownerUUID()))) {
            MemberOwnable entryOwner = new MemberOwnable(plotEntry.ownerUUID());

            String error = "Showcasing plot with un-sync owner! Expected: "
                    + plotData.getOwner().getName() + "(" + plotData.getOwner().getUniqueId() + ") Got tracked entry: "
                    + entryOwner.getOwner().getName() + "(" + entryOwner.getOwner().getUniqueId() + "), "
                    + "the system prioritize data from the provider as the data to display.";

            Notification.sendErrorEmbed(error);
            DiscordPS.error(error);
        }

        final String threadName = PlotSystemThread.THREAD_NAME.apply(plotEntry.plotID(), plotData.formatOwnerName());
        WebhookDataBuilder.WebhookData imageData = new WebhookDataBuilder().setThreadName(threadName).build();
        plotData.getImageFiles().forEach(imageData::addFile);

        return this.webhook.queueNewUpdateAction(
            this.webhook.newThreadFromWebhook(imageData, null, true),
            message -> this.sendShowcaseInformation(message, plotData, owner -> new ShowcaseComponent(
                0, Long.toUnsignedString(plotEntry.threadID()), owner, plotData
            ))
        );
    }

    /**
     * Use the first message of a created showcase thread to send showcase information message.
     *
     * @param message The message reference of the created thread
     * @param plotData The plot data to retrieve showcase information
     * @param builder A builder function to build a {@link ShowcaseComponent}
     * @return A rest action that if successful, return a non-empty optional of the message reference parameter
     */
    private @NotNull RestAction<Optional<MessageReference>>
    sendShowcaseInformation(@NotNull MessageReference message,
                            @NotNull PlotData plotData,
                            @NotNull Function<MemberOwnable, ShowcaseComponent> builder
    ) {
        final Container component = builder.apply(plotData).build();

        WebhookDataBuilder.WebhookData infoData = new WebhookDataBuilder()
                .setComponentsV2(Collections.singletonList(component))
                .forceComponentV2()
                .suppressNotifications()
                .suppressMentions()
                .build();

        plotData.getAvatarFile().ifPresent(infoData::addFile);

        return this.webhook.sendMessageInThread(message.getMessageId(), infoData, true, true)
                .map(ignored -> Optional.of(message));
    }
}
