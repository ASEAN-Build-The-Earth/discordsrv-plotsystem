package asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang;

import asia.buildtheearth.asean.discord.plotsystem.core.system.io.MessageLang;
import org.jetbrains.annotations.NotNull;

public enum PlotInteraction implements MessageLang {
    INTERACTED_BAD_OWNER("interactions.plot-button-bad-owner"),
    HELP_GET_FAILURE("interactions.plot-help-get-failed"),
    FEEDBACK_GET_FAILURE("interactions.plot-feedback-get-failed");

    private final String path;

    PlotInteraction(String path) {
        this.path = path;
    }

    @Override
    public @NotNull String getKey() {
        return this.path;
    }
}
