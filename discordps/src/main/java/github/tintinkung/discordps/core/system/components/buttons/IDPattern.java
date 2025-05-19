package github.tintinkung.discordps.core.system.components.buttons;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum IDPattern {
    PLUGIN("plugin"),
    TYPE("type"),
    ID("id"),
    USER("user"),
    PAYLOAD("payload");

    private final String group;
    /**
     * Plugin interaction button {@code custom_id} convention.
     * <p>
     * Matches component IDs in the format: {@code <plugin>/<type>/<id>/<user>} or {@code <plugin>/<type>/<id>/<user>/<payload>}.
     * <p>
     * Regex pattern used: {@code "^(?<plugin>\\w+)/(?<type>\\w+)/(?<id>\\w+)/(?<user>\\w+)(?:/(?<payload>.*?))?/?$"}
     */
    public static final Pattern COMPONENT_PATTERN = Pattern.compile(
        String.format("^" +
            "(?<%s>\\w+)/" +
            "(?<%s>\\w+)/" +
            "(?<%s>\\w+)/" +
            "(?<%s>\\w+)" +
            "(?:/(?<%s>.*?))?/?$",
            PLUGIN.group,
            TYPE.group,
            ID.group,
            USER.group,
            PAYLOAD.group
        )
    );

    IDPattern(@NotNull String group) {
        this.group = group;
    }

    /**
     * Parses a {@code custom_id} string to extract the components based on the {@link IDPattern} convention.
     * The returned {@link EnumMap} maps {@link IDPattern} values to their corresponding extracted string values.
     *
     * @param customId the {@code custom_id} string to be parsed
     * @return an {@link EnumMap} with the extracted components, or {@code null} if the {@code custom_id} doesn't match the pattern
     */
    @Nullable
    public static EnumMap<IDPattern, String> parseCustomID(String customId) {
        Matcher matcher = COMPONENT_PATTERN.matcher(customId);
        if (!matcher.matches()) return null;

        EnumMap<IDPattern, String> result = new EnumMap<>(IDPattern.class);
        for (IDPattern component : values()) {

            String value = matcher.group(component.group);
            if (value != null) result.put(component, value);
        }
        return result;
    }
}
