package asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang;

import asia.buildtheearth.asean.discord.plotsystem.core.system.io.SystemLang;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public enum CommandInteractions implements SystemLang {
    BUTTON_CONFIRM("buttons.confirm"),
    BUTTON_DISMISS("buttons.dismiss"),
    BUTTON_CANCEL("buttons.cancel"),
    BUTTON_ATTACH_IMAGE("buttons.attach-image"),
    BUTTON_PROVIDE_IMAGE("buttons.provide-image"),
    BUTTON_ARCHIVE_NOW("buttons.archive-now"),
    BUTTON_SHOWCASE_NOW("buttons.showcase-now"),
    BUTTON_CONFIRM_DELETE("buttons.confirm-delete"),
    BUTTON_CREATE_REGISTER("buttons.create-register"),
    BUTTON_CREATE_UNTRACKED("buttons.create-untracked"),
    BUTTON_CREATE_WEBHOOK("buttons.create-webhook"),
    BUTTON_CONTINUE("buttons.continue"),
    BUTTON_CLEAR("buttons.clear"),
    BUTTON_ADD("buttons.add"),
    BUTTON_EDIT("buttons.edit"),

    EMBED_PLUGIN_NOT_READY("embeds.plugin-not-ready"),
    EMBED_ATTACH_IMAGE_FAILED("embeds.attach-image-error"),
    EMBED_ATTACH_IMAGE("embeds.attach-image-interaction"),
    EMBED_PROVIDE_IMAGE("embeds.provide-image-interaction"),

    LABEL_LATEST("labels.latest"),
    LABEL_SQL_ERROR("labels.sql-error-occurred"),
    LABEL_ERROR_OCCURRED("labels.error-occurred"),
    LABEL_ERROR("labels.error");

    private final String path;

    CommandInteractions(String path) {
        this.path = path;
    }

    @Override
    @Contract(pure = true)
    public @NotNull String getKey() {
        return LangPaths.COMMAND_INTERACTIONS + this.path;
    }
}
