package asia.buildtheearth.asean.discord.plotsystem.commands.providers;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.Interaction;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.LangManager;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.LanguageFile;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.SystemLang;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.CommandInteractions;
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
