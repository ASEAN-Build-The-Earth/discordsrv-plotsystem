package asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang;

import asia.buildtheearth.asean.discord.plotsystem.core.system.io.MessageLang;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum HistoryMessage implements MessageLang {
    INITIAL_CREATION("history-messages.initial-creation"),
    ON_CREATED("history-messages.on-created"),
    ON_SUBMITTED("history-messages.on-submitted"),
    ON_APPROVED("history-messages.on-approved"),
    ON_REJECTED("history-messages.on-rejected"),
    ON_ABANDONED("history-messages.on-abandoned"),
    ON_ARCHIVED("history-messages.on-archived"),
    ON_RECLAIMED("history-messages.on-reclaimed"),
    ON_SYSTEM_FETCH("history-messages.on-system-fetch");

    private final String path;

    HistoryMessage(String path) {
        this.path = path;
    }

    @Override
    @Contract(pure = true)
    public @NotNull String getKey() {
        return  this.path;
    }
}
