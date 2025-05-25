package github.tintinkung.discordps.core.system.io.lang;

import github.tintinkung.discordps.core.system.io.SystemLang;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum SetupHelpCommand implements SystemLang {
    DESC("description.command"),

    EMBED_NOT_READY(          "embeds.not-ready"),
    EMBED_READY_WITH_WARNING( "embeds.ready-with-warning"),
    EMBED_READY(              "embeds.ready"),
    EMBED_CHECKLIST(          "embeds.checklist"),
    EMBED_DEBUGGING_NOTICE(   "embeds.debugging-notice"),
    EMBED_TAGS_CONFIG_INFO(   "embeds.tags-config-info"),

    MESSAGE_WARNINGS("messages.warnings");

    private final String path;

    SetupHelpCommand(String path) {
        this.path = path;
    }

    @Override
    @Contract(pure = true)
    public @NotNull String getKey() {
        return LangPaths.SETUP_HELP + this.path;
    }
}
