package asia.buildtheearth.asean.discord.plotsystem.core.system.embeds;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.database.ThreadStatus;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.WebhookStatusProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableTag;
import asia.buildtheearth.asean.discord.plotsystem.core.system.MemberOwnable;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.LanguageFile;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.MessageLang;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Status embed for tracking plot status.
 * Sent separately from the initial component container.
 *
 * <p>Used as the message ID primary key in the database,
 * tracking all possible plot entries in a thread.</p>
 */
public class StatusEmbed extends EmbedBuilder implements PlotDataEmbed {

    /**
     * Construct a status embed as a fully configured {@linkplain EmbedBuilder}.
     *
     * <p>Configured field includes:</p>
     * <ul>
     *     <li>The {@linkplain EmbedBuilder#setAuthor(String, String, String) author} information</li>
     *     <li>The accent {@linkplain EmbedBuilder#setColor(java.awt.Color) color} of the status</li>
     *     <li>The {@linkplain EmbedBuilder#setTitle(String, String) title} message and reference URL</li>
     *     <li>The {@linkplain EmbedBuilder#setDescription(CharSequence) description} message from {@link DisplayMessage}</li>
     * </ul>
     *
     * @param owner The owner/author of this embed.
     * @param status The accent status of this embed.
     * @param referenceURL Optional reference URL to be embedded on the title text.
     */
    public StatusEmbed(@NotNull MemberOwnable owner, @NotNull ThreadStatus status, @Nullable String referenceURL) {
        super();

        owner.getOwnerDiscord().ifPresentOrElse(
            (member) -> this.setAuthor(member.getUser().getName(), null, member.getEffectiveAvatarUrl()),
            () -> this.setAuthor(owner.getOwner().getName())
        );
        DisplayMessage message = DisplayMessage.fromStatus(status);

        this.setTitle(message.get().title(), referenceURL);
        this.setDescription(message.get().description());
        this.setColor(status.toTag().getColor());
    }

    /**
     * Saved message presets for displaying by thread status
     */
    public enum DisplayMessage implements WebhookStatusProvider {
        ON_GOING("status-embeds.on-going"),
        FINISHED("status-embeds.finished"),
        REJECTED("status-embeds.rejected"),
        APPROVED("status-embeds.approved"),
        ARCHIVED("status-embeds.archived"),
        ABANDONED("status-embeds.abandoned");

        private final MessageLang message;

        DisplayMessage(String message) {
            this.message = () -> message;
        }

        public static @NotNull DisplayMessage fromStatus(@NotNull ThreadStatus status) {
            return valueOf(status.name().toUpperCase(java.util.Locale.ENGLISH));
        }

        public @NotNull LanguageFile.EmbedLang get() {
            return DiscordPS.getMessagesLang().getEmbed(this.message);
        }

        @Override
        public @NotNull ThreadStatus toStatus() {
            return valueOf(ThreadStatus.class, this.name().toLowerCase(java.util.Locale.ENGLISH));
        }

        @Override
        public @NotNull AvailableTag toTag() {
            return valueOf(AvailableTag.class, this.name());
        }
    }
}
