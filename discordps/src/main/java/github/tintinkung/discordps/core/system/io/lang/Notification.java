package github.tintinkung.discordps.core.system.io.lang;

import github.tintinkung.discordps.core.system.io.NotificationLang;
import github.tintinkung.discordps.core.system.io.SystemLang;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.util.Optional;

import static github.tintinkung.discordps.Constants.RED;
import static github.tintinkung.discordps.Constants.GREEN;
import static github.tintinkung.discordps.Constants.BLUE;
import static github.tintinkung.discordps.Constants.PURPLE;
import static github.tintinkung.discordps.Constants.YELLOW;
import static github.tintinkung.discordps.Constants.ORANGE;
import static github.tintinkung.discordps.core.system.io.lang.Format.*;

public class Notification {

    public static final SystemLang SYSTEM_ERROR = () -> LangPaths.SYSTEM_NOTIFICATION + "system-error.error-title";

    public static final SystemLang LABEL_ERROR = () -> LangPaths.SYSTEM_NOTIFICATION + "system-error.error-label";

    public sealed interface PluginNotification extends NotificationLang permits PluginMessage { }

    public sealed interface PlotNotification extends NotificationLang permits PlotMessage, CommandNotification {
        /**
         * Get the formatter tokens available in this message
         *
         * @return Message's {@link Format} arguments if exist
         */
        @NotNull String[] getLangArgs();
    }

    public sealed interface CommandNotification extends PlotNotification permits CommandMessage { }

    public sealed interface ErrorNotification extends NotificationLang permits ErrorMessage {
        /**
         * Get the formatter tokens available in this message
         *
         * @return Optional of a language {@link Format} arguments if exist
         */
        @NotNull Optional<String[]> getOptLangArgs();
    }

    public enum Config { DISABLED, ENABLED, WITH_CONTENT }

    public enum PluginMessage implements PluginNotification {
        PLUGIN_STARTED(GREEN, "plugin-started"),
        PLUGIN_STOPPING_GRACEFUL(ORANGE, "plugin-stopping-graceful"),
        PLUGIN_STOPPING_ON_ERROR(RED, "plugin-stopping-on-error");

        private final @NotNull String path;
        private final @NotNull Color accentColor;

        PluginMessage(@NotNull Color accentColor, @NotNull String path) {
            this.accentColor = accentColor;
            this.path = path;
        }

        /** {@inheritDoc} */
        @Override
        @Contract(pure = true)
        public @NotNull String getKey() {
            return LangPaths.SYSTEM_NOTIFICATION + this.path;
        }

        /** {@inheritDoc} */
        @Override
        @Contract(pure = true)
        public @NotNull Color getAccentColor() {
            return this.accentColor;
        }
    }

    public enum CommandMessage implements CommandNotification {
        PLOT_FETCH(BLUE, "plot-commands.plot-fetch", LABEL, USER_ID, PLOT_ID),
        PLOT_DELETE(RED, "plot-commands.plot-delete", MESSAGE_ID, THREAD_ID, USER_ID),
        PLOT_ARCHIVE(BLUE, "plot-commands.plot-archive", PLOT_ID, USER_ID, THREAD_ID),
        PLOT_SHOWCASE(GREEN, "plot-commands.plot-showcase", USER_ID, THREAD_ID);

        private final @NotNull String path;
        private final @NotNull Color accentColor;
        private final @NotNull String[] langArgs;

        CommandMessage(@NotNull Color accentColor, @NotNull String path, @NotNull String... args) {
            this.accentColor = accentColor;
            this.path = path;
            this.langArgs = args;
        }

        /** {@inheritDoc} */
        @Override
        @Contract(pure = true)
        public @NotNull String getKey() {
            return LangPaths.SYSTEM_NOTIFICATION + this.path;
        }

        /** {@inheritDoc} */
        @Override
        @Contract(pure = true)
        public @NotNull Color getAccentColor() {
            return this.accentColor;
        }


        /** {@inheritDoc} */
        @Override
        @Contract(pure = true)
        public @NotNull String[] getLangArgs() {
            return this.langArgs;
        }
    }

    public enum PlotMessage implements PlotNotification {
        PLOT_CREATED(GREEN, "plot-status.plot-created", OWNER),
        PLOT_SUBMITTED(YELLOW, "plot-status.plot-submitted", TIMESTAMP),
        PLOT_APPROVED(GREEN, "plot-status.plot-approved"),
        PLOT_REJECTED(RED, "plot-status.plot-rejected", OWNER),
        PLOT_INACTIVE_ABANDONED(PURPLE, "plot-status.plot-inactive-abandoned"),
        PLOT_MANUALLY_ABANDONED(PURPLE, "plot-status.plot-manually-abandoned");

        private final @NotNull String path;
        private final @NotNull Color accentColor;
        private final @NotNull String[] langArgs;

        PlotMessage(@NotNull Color accentColor, @NotNull String path) {
            this.accentColor = accentColor;
            this.path = path;
            this.langArgs = new String[] { THREAD_ID, PLOT_ID };
        }

        PlotMessage(@NotNull Color accentColor, @NotNull String path, @NotNull String format) {
            this.accentColor = accentColor;
            this.path = path;
            this.langArgs = new String[] { THREAD_ID, PLOT_ID, format };
        }

        /** {@inheritDoc} */
        @Override
        @Contract(pure = true)
        public @NotNull String getKey() {
            return LangPaths.SYSTEM_NOTIFICATION + this.path;
        }

        /** {@inheritDoc} */
        @Override
        @Contract(pure = true)
        public @NotNull Color getAccentColor() {
            return this.accentColor;
        }

        /** {@inheritDoc} */
        @Override
        @Contract(pure = true)
        public @NotNull String[] getLangArgs() {
            return this.langArgs;
        }
    }

    public enum ErrorMessage implements ErrorNotification {
        // Expected Errors
        PLOT_CREATE_EXCEPTION("system-error.plot-create-exception", PLOT_ID),
        PLOT_UPDATE_EXCEPTION("system-error.plot-update-exception", PLOT_ID, EVENT),
        PLOT_FEEDBACK_SQL_EXCEPTION("system-error.plot-feedback-sql-exception", PLOT_ID),
        PLOT_UPDATE_SQL_EXCEPTION("system-error.plot-update-sql-exception", PLOT_ID, EVENT),
        PLOT_REGISTER_ENTRY_EXCEPTION("system-error.plot-register-entry-exception", PLOT_ID),
        PLOT_FEEDBACK_GET_EXCEPTION("system-error.plot-feedback-get-exception", PLOT_ID, USER_ID),

        // Plot updating failures
        FAILED_ATTACH_BUTTON("system-error.failed-attach-button"),
        FAILED_THREAD_EDIT("system-error.failed-thread-edit"),
        FAILED_MESSAGE_EDIT("system-error.failed-message-edit"),
        FAILED_LAYOUT_EDIT("system-error.failed-layout-edit"),

        // Unknown Errors
        PLOT_UPDATE_UNKNOWN_EXCEPTION("system-error.plot-update-unknown-exception"),
        PLOT_CREATE_UNKNOWN_EXCEPTION("system-error.plot-create-unknown-exception"),
        PLOT_REGISTER_UNKNOWN_EXCEPTION("system-error.plot-register-unknown-exception"),
        PLOT_FEEDBACK_UNKNOWN_EXCEPTION("system-error.plot-feedback-unknown-exception");

        private final @NotNull String path;
        private final @Nullable String[] langArgs;

        ErrorMessage(@NotNull String path) {
            this.path = path;
            this.langArgs = null;
        }

        ErrorMessage(@NotNull String path, @NotNull String... langArgs) {
            this.path = path;
            this.langArgs = langArgs;
        }

        /** {@inheritDoc} */
        @Override
        @Contract(pure = true)
        public @NotNull String getKey() {
            return LangPaths.SYSTEM_NOTIFICATION + this.path;
        }

        /** {@inheritDoc} */
        @Override
        @Contract(pure = true)
        public @NotNull Color getAccentColor() {
            return RED;
        }

        /** {@inheritDoc} */
        @Override
        @Contract(pure = true)
        public @NotNull Optional<String[]> getOptLangArgs() {
            return Optional.ofNullable(this.langArgs);
        }
    }
}