package github.tintinkung.discordps.core.system;

import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.core.database.WebhookEntry;
import github.tintinkung.discordps.core.providers.WebhookProvider;
import github.tintinkung.discordps.core.system.components.api.Container;
import github.tintinkung.discordps.core.system.io.lang.PlotNotification;
import github.tintinkung.discordps.core.system.layout.ShowcaseComponent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

import static github.tintinkung.discordps.core.system.PlotSystemThread.THREAD_NAME;

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
    public CompletableFuture<Optional<MessageReference>> showcasePlot(@NotNull PlotData plotData,
                                                                      @NotNull WebhookEntry plotEntry) {

        Function<MemberOwnable, ShowcaseComponent> showcaseComponent = owner -> new ShowcaseComponent(
                0,
                Long.toUnsignedString(plotEntry.threadID()),
                owner,
                plotData
        );

        final Container component;
        final String owner;
        final String threadName;
        final Supplier<Optional<File>> avatarFile;

        // Safety check if plot entry and webhook entry is as the same owner
        if(plotData.getOwner().getUniqueId().equals(UUID.fromString(plotEntry.ownerUUID()))) {
            component = showcaseComponent.apply(plotData).build();
            owner = plotData.getOwnerMentionOrName();
            threadName = THREAD_NAME.apply(plotEntry.plotID(), plotData.formatOwnerName());
            avatarFile = plotData::getAvatarFile;
        }
        else {
            MemberOwnable entryOwner = new MemberOwnable(plotEntry.ownerUUID());

            String error = "Showcasing plot with un-sync owner! Expected: "
                    + plotData.getOwner().getName() + "(" + plotData.getOwner().getUniqueId() + ") Got tracked entry: "
                    + entryOwner.getOwner().getName() + "(" + entryOwner.getOwner().getUniqueId() + "), "
                    + "the system prioritize tracked entry as the data to display.";

            Notification.sendErrorEmbed(error);
            DiscordPS.error(error);

            component = showcaseComponent.apply(entryOwner).build();
            owner = entryOwner.getOwnerMentionOrName();
            threadName = THREAD_NAME.apply(plotEntry.plotID(), entryOwner.formatOwnerName());
            avatarFile = entryOwner::getAvatarFile;
        }

        WebhookDataBuilder.WebhookData imageData = new WebhookDataBuilder()
                .setThreadName(threadName)
                .build();

        plotData.getImageFiles().forEach(imageData::addFile);

        WebhookDataBuilder.WebhookData infoData = new WebhookDataBuilder()
                .setComponentsV2(Collections.singletonList(component))
                .forceComponentV2()
                .suppressNotifications()
                .suppressMentions()
                .build();

        return this.webhook.queueNewUpdateAction(
            this.webhook.newThreadFromWebhook(imageData, null, true),
            message -> {
                String messageID = message.getMessageId();
                String threadID = Long.toUnsignedString(plotEntry.threadID());

                avatarFile.get().ifPresent(infoData::addFile);

                // Send notification to plot owner
                // DiscordPS.getPlugin().getWebhook()
                DiscordPS.getPlugin().getWebhook().sendNotification(PlotNotification.ON_SHOWCASED, threadID, owner);

                // Send showcase information to the showcase thread
                return this.webhook.sendMessageInThread(messageID, infoData, true, true)
                        .map(ignored -> Optional.of(message));
            }
        );
    }
}
