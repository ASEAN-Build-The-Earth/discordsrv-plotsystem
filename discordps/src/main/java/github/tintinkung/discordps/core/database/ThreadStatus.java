package github.tintinkung.discordps.core.database;

import github.tintinkung.discordps.core.system.AvailableTags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum ThreadStatus {
    on_going,
    finished,
    rejected,
    approved,
    archived,
    abandoned;


    public static @NotNull ThreadStatus toPlotStatus(@NotNull PlotStatus status) {
        return switch (status) {
            case completed, unreviewed -> ThreadStatus.finished;
            case unfinished -> ThreadStatus.on_going;
            case unclaimed -> ThreadStatus.abandoned;
        };
    }

    public AvailableTags toTag() {
        return valueOf(AvailableTags.class, this.name().toUpperCase(Locale.ENGLISH));
    }
}
