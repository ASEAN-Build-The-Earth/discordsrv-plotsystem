package github.tintinkung.discordps.commands.providers;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.tintinkung.discordps.Constants;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.interactions.Interaction;
import github.tintinkung.discordps.core.system.io.LangManager;
import github.tintinkung.discordps.core.system.io.LanguageFile;
import github.tintinkung.discordps.core.system.io.SystemLang;
import github.tintinkung.discordps.core.system.io.lang.CommandInteractions;
import org.jetbrains.annotations.NotNull;

public class SystemCommandProvider<
        I extends Interaction, V extends SystemLang>
        extends SubcommandProvider<I, SystemLang, V> {

    public SystemCommandProvider(@NotNull String name, @NotNull String description) {
        super(name, description);
    }

    @Override
    protected LangManager<SystemLang> getLangManager() {
        return DiscordPS.getSystemLang();
    }

    protected MessageEmbed errorEmbed(String error) {
        return errorEmbed(
            getLangManager().get(CommandInteractions.LABEL_ERROR_OCCURRED),
            getLangManager().get(CommandInteractions.LABEL_ERROR),
            error
        );
    }

    protected MessageEmbed errorEmbed(V description) {
        return errorEmbed(
            getLangManager().get(CommandInteractions.LABEL_ERROR_OCCURRED),
            getLang(description)
        );
    }

    protected MessageEmbed sqlErrorEmbed(V description, String error) {
        return sqlErrorEmbed(
            getLangManager().get(CommandInteractions.LABEL_SQL_ERROR),
            getLang(description),
            error
        );
    }

    protected MessageEmbed errorEmbed(V description, String error) {
        return errorEmbed(
            getLangManager().get(CommandInteractions.LABEL_ERROR_OCCURRED),
            getLang(description),
            error
        );
    }

    protected MessageEmbed errorEmbed(@NotNull LanguageFile.EmbedLang info) {
        return errorEmbed(info.title(), info.description());
    }

    protected MessageEmbed errorEmbed(@NotNull LanguageFile.EmbedLang info, String error) {
        return errorEmbed(info.title(), info.description(), error);
    }

    protected MessageEmbed sqlErrorEmbed(String title, String description, String error) {
        return new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .addField(getLangManager().get(CommandInteractions.LABEL_SQL_ERROR),
                "```" + error + "```", false)
            .setColor(Constants.RED)
            .build();
    }

    protected MessageEmbed errorEmbed(String title, String description, String error) {
        return new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .addField(getLangManager().get(CommandInteractions.LABEL_ERROR),
                "```" + error + "```", false)
            .setColor(Constants.RED)
            .build();
    }

    protected MessageEmbed errorEmbed(String title, String description) {
        return new EmbedBuilder()
            .setTitle(title)
            .setDescription(description)
            .setColor(Constants.RED)
            .build();
    }
}
