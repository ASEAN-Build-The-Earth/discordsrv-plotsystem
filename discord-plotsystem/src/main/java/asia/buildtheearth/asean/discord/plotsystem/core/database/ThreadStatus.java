package asia.buildtheearth.asean.discord.plotsystem.core.database;

import asia.buildtheearth.asean.discord.plotsystem.core.providers.WebhookStatusProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.AvailableTag;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * The saved thread status as SQL enum.
 * Must be sync with {@link AvailableTag} in the lowercase manner.
 */
public enum ThreadStatus implements WebhookStatusProvider {
    on_going,
    finished,
    rejected,
    approved,
    archived,
    abandoned;

    public static final ThreadStatus[] VALUES = values();

    /**
     * Construct a SQL compatible string for this enum.
     *
     * @return The SQL enum entry
     */
    public static @NotNull String constructEnumString() {
        StringBuilder enumBuilder = new StringBuilder();
        enumBuilder.append("(");
        for(int i = 0; i < VALUES.length; i++) {
            enumBuilder.append("'");
            enumBuilder.append(VALUES[i]);
            enumBuilder.append("'");
            if(i + 1 < VALUES.length) enumBuilder.append(",");
        }
        enumBuilder.append(")");
        return enumBuilder.toString();
    }


    public static @NotNull ThreadStatus fromPlotStatus(@NotNull PlotStatus status) {
        return switch (status) {
            case completed, unreviewed -> ThreadStatus.finished;
            case unfinished -> ThreadStatus.on_going;
            case unclaimed -> ThreadStatus.abandoned;
        };
    }

    @Override
    @Contract(value = " -> this", pure = true)
    public @NotNull ThreadStatus toStatus() {
        return this;
    }

    @Override
    public @NotNull AvailableTag toTag() {
        return valueOf(AvailableTag.class, this.name().toUpperCase(Locale.ENGLISH));
    }
}
