package asia.buildtheearth.asean.discord.plotsystem.core.system.io;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class LanguageFile<T extends LangConfig> extends YamlConfiguration implements LangManager<T> {

    public static final String NULL_LANG = "undefined";

    public record EmbedLang(String title, String description) {}

    public @NotNull String get(@NotNull String key) {
        return this.getString(key, NULL_LANG);
    }

    public @NotNull String get(@NotNull T config) {
        return this.getString(config.getKey(), NULL_LANG);
    }

    public @NotNull String get(@NotNull T config, @NotNull String defaultValue) {
        return this.getString(config.getKey(), defaultValue);
    }

    public @NotNull EmbedLang getEmbed(@NotNull String key, @Nullable String defaultValue) {
        List<String> lang = this.getLangPair(key);

        String title = !lang.isEmpty() ? lang.getFirst() != null? lang.getFirst() : defaultValue : defaultValue;
        String description = lang.size() > 1 ? lang.get(1) != null? lang.get(1) : defaultValue : defaultValue;

        return new EmbedLang(title, description);
    }

    public @NotNull EmbedLang getEmbed(@NotNull T config) {
        return this.getEmbed(config.getKey(), NULL_LANG);
    }

    public @NotNull EmbedLang getEmbed(@NotNull T config, @Nullable String defaultValue) {
        return this.getEmbed(config.getKey(), defaultValue);
    }

    public @NotNull EmbedBuilder getEmbedBuilder(@NotNull T config,
                                        @NotNull Function<String, String> title,
                                        @NotNull Function<String, String> description) {
        List<String> lang = this.getLangPair(config.getKey());
        EmbedBuilder embed = new EmbedBuilder();

        if(!lang.isEmpty() && lang.getFirst() != null)
            embed.setTitle(title.apply(lang.getFirst()));

        if(lang.size() > 1 && lang.get(1) != null)
            embed.setDescription(description.apply(lang.get(1)));

        return embed;
    }

    public @NotNull EmbedBuilder getEmbedBuilder(@NotNull T config) {
        return this.getEmbedBuilder(config, Function.identity(), Function.identity());
    }

    public @NotNull EmbedBuilder getEmbedBuilder(@NotNull T config,
                                                 @NotNull Function<String, String> description) {
        return this.getEmbedBuilder(config, Function.identity(), description);
    }

    /**
     * Extract nullable pair from language key
     *
     * @param key config key
     * @return [null, null], [null, "value"], ["value", "value"], ["value", null]
     */
    @NotNull
    private List<@Nullable String> getLangPair(String key) {
        List<?> list = this.getList(key, Stream.of(null, null).toList());

        // Produce [null, "value"], ["value", "value"], ["value", null]
        List<@Nullable String> lang = new ArrayList<>();
        for(Object obj : list) lang.add(obj == null ? null : obj.toString());

        return lang;
    }
}
