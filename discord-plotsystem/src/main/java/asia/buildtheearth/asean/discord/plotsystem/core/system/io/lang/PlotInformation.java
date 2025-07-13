package asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang;

import asia.buildtheearth.asean.discord.plotsystem.core.system.io.MessageLang;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum PlotInformation implements MessageLang {
    // Information metadata
    INFO_TITLE("plot.plot-info-title"),
    SHOWCASE_TITLE("plot.plot-showcase-title"),
    THREAD_NAME("plot.thread-name"),
    HISTORIES_TITLE("plot.plot-histories"),
    HISTORIES_PREFIX("plot.histories-prefix"),

    // Google Map button
    MAP_LABEL("plot.google-map"),

    // Documentation button
    DOCS_LABEL("plot.documentation.label"),
    DOCS_URL("plot.documentation.url"),

    // Feedback Button labels
    FEEDBACK_LABEL("plot.feedback-label.feedback"),
    REJECTED_FEEDBACK_LABEL("plot.feedback-label.rejected-feedback"),
    APPROVED_FEEDBACK_LABEL("plot.feedback-label.approved-feedback"),
    REJECTED_NO_FEEDBACK_LABEL("plot.feedback-label.rejected-no-feedback"),
    APPROVED_NO_FEEDBACK_LABEL("plot.feedback-label.approved-no-feedback"),
    NEW_FEEDBACK_NOTIFICATION("plot.feedback-label.feedback-notification"),

    // Help Button
    HELP_LABEL("plot.help.label"),
    HELP_TITLE("plot.help.title"),
    HELP_CONTENT("plot.help.content");

    private final String path;

    PlotInformation(String path) {
        this.path = path;
    }

    @Override
    @Contract(pure = true)
    public @NotNull String getKey() {
        return  this.path;
    }
}
