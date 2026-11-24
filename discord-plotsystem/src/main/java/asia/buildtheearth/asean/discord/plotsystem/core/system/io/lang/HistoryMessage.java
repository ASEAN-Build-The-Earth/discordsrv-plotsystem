package asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang;

import asia.buildtheearth.asean.discord.plotsystem.core.system.io.MessageLang;
import com.alpsbte.plotsystem.api.model.PlotAbandonReason;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum HistoryMessage implements MessageLang {
    INITIAL_CREATION("history-messages.initial-creation"),
    ON_CREATED("history-messages.on-created"),
    ON_SUBMITTED("history-messages.on-submitted"),
    ON_APPROVED("history-messages.on-approved"),
    ON_REJECTED("history-messages.on-rejected"),
    ON_ARCHIVED("history-messages.on-archived"),
    ON_RECLAIMED("history-messages.on-reclaimed"),
    ON_UNDO_REVIEW("history-messages.on-undo-review"),
    ON_UNDO_SUBMIT("history-messages.on-undo-submit"),
    ON_SYSTEM_FETCH("history-messages.on-system-fetch"),
    ON_ABANDONED_INACTIVE("history-messages.on-abandoned.inactive"),
    ON_ABANDONED_MANUALLY("history-messages.on-abandoned.manually"),
    ON_ABANDONED_COMMANDS("history-messages.on-abandoned.commands"),
    ON_ABANDONED_SYSTEM("history-messages.on-abandoned.system");

    private final String path;

    HistoryMessage(String path) {
        this.path = path;
    }

    /**
     * Get the abandon message for each Plot-System abandon reason.
     *
     * @param reason The Plot-System abandon reason
     * @return An enum definition for {@link HistoryMessage}
     */
    public static HistoryMessage getAbandonMessage(PlotAbandonReason reason) {
        return switch (reason) {
            case INACTIVITY -> ON_ABANDONED_INACTIVE;
            case MANUAL -> ON_ABANDONED_MANUALLY;
            case COMMAND -> ON_ABANDONED_COMMANDS;
            case null, default -> ON_ABANDONED_SYSTEM;
        };
    }

    @Override
    @Contract(pure = true)
    public @NotNull String getKey() {
        return  this.path;
    }
}
