package github.tintinkung.discordps.core.system.io;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public interface LangManager<T extends LangConfig> {

    @NotNull
    String get(@NotNull T config);

    @NotNull
    LanguageFile.EmbedLang getEmbed(@NotNull T config);

    @NotNull
    LanguageFile.EmbedLang getEmbed(@NotNull T config, @Nullable String defaultValue);

    @NotNull
    EmbedBuilder getEmbedBuilder(@NotNull T config,
                                 @NotNull Function<String, String> title,
                                 @NotNull Function<String, String> description);

    @NotNull
    EmbedBuilder getEmbedBuilder(@NotNull T config);

    @NotNull
    EmbedBuilder getEmbedBuilder(@NotNull T config,
                                 @NotNull Function<String, String> description);
}
