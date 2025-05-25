package github.tintinkung.discordps.core.system.embeds;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.core.providers.WebhookStatusProvider;
import github.tintinkung.discordps.core.system.AvailableTag;
import github.tintinkung.discordps.core.system.MemberOwnable;
import github.tintinkung.discordps.core.system.io.LanguageFile;
import github.tintinkung.discordps.core.system.io.MessageLang;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Status embed for tracking plot status.
 * Sent separately from the initial component container.
 *
 * <p>Used as the message ID primary key in the database,
 * tracking all possible plot entries in a thread.</p>
 */
public class StatusEmbed extends EmbedBuilder implements PlotDataEmbed {
    public StatusEmbed(@NotNull MemberOwnable owner, @NotNull ThreadStatus status) {
        super();

        owner.getOwnerDiscord().ifPresentOrElse(
            (member) -> this.setAuthor(member.getUser().getName(), null, member.getEffectiveAvatarUrl()),
            () -> this.setAuthor(owner.getOwner().getName())
        );
        DisplayMessage message = DisplayMessage.fromStatus(status);

        this.setTitle(message.get().title());
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
            return valueOf(status.name().toUpperCase(Locale.ENGLISH));
        }

        public @NotNull LanguageFile.EmbedLang get() {
            return DiscordPS.getMessagesLang().getEmbed(this.message);
        }

        @Override
        public @NotNull ThreadStatus toStatus() {
            return valueOf(ThreadStatus.class, this.name().toLowerCase(Locale.ENGLISH));
        }

        @Override
        public @NotNull AvailableTag toTag() {
            return valueOf(AvailableTag.class, this.name());
        }
    }
}
