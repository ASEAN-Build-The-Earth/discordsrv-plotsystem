package github.tintinkung.discordps.core.system;

import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.ConfigPaths;

import github.scarsz.discordsrv.dependencies.jda.api.exceptions.ParsingException;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.scarsz.discordsrv.dependencies.commons.codec.DecoderException;
import github.scarsz.discordsrv.dependencies.commons.codec.binary.Hex;
import github.scarsz.discordsrv.objects.ExpiringDualHashBidiMap;

import github.tintinkung.discordps.core.database.ThreadStatus;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.awt.color.ColorSpace;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * Plot-System forum management tags
 * configured with {@link ConfigPaths}
 */
public enum AvailableTags {
    ON_GOING(ConfigPaths.TAG_ON_GOING, ConfigPaths.EMBED_COLOR_ON_GOING, Color.GRAY),
    FINISHED(ConfigPaths.TAG_FINISHED, ConfigPaths.EMBED_COLOR_FINISHED, Color.YELLOW),
    REJECTED(ConfigPaths.TAG_REJECTED, ConfigPaths.EMBED_COLOR_REJECTED, Color.RED),
    APPROVED(ConfigPaths.TAG_APPROVED, ConfigPaths.EMBED_COLOR_APPROVED, Color.GREEN),
    ARCHIVED(ConfigPaths.TAG_ARCHIVED, ConfigPaths.EMBED_COLOR_ARCHIVED, Color.CYAN);

    private static final ExpiringDualHashBidiMap<String, String> tagCache = new ExpiringDualHashBidiMap<>(TimeUnit.SECONDS.toMillis(10));
    private static final ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);

    private TagReference tagRef;

    private final String tagPath;
    private final String colorPath;

    @Nullable
    private String tagID;

    @NotNull
    private Color color;

    AvailableTags(String config, String colorConfig, @NotNull Color defaultColor) {
        tagPath = config;
        colorPath = colorConfig;
        color = defaultColor;
    }

    public ThreadStatus toStatus() {
        return valueOf(ThreadStatus.class, this.name().toLowerCase(Locale.ENGLISH));
    }

    /**
     * Get the configured tag's embed color or default if invalid.
     * @return {@link java.awt.Color}
     */
    @NotNull
    public Color getColor() {
        return color;
    }

    /**
     * Get the forum tag data including snowflake ID and a name.
     * @return {@link TagReference} object of this enum,
     * Null if the enum has not been applied yet (handled by {@link github.tintinkung.discordps.core.WebhookManager})
     */
    public TagReference getTag() {
        return this.tagRef;
    }

    /**
     * Cache all available tag from provided API response data.
     * @param availableTags The Discord API available_tags data array.
     * @throws ParsingException If the response does not contain proper tag object keys (likely due to API version changes).
     */
    public static void initCache(@NotNull DataArray availableTags) throws ParsingException {
        for (int i = 0; i < availableTags.length(); i++) {
            DataObject tagObj = availableTags.getObject(i);

            synchronized (tagCache) {
                tagCache.put(tagObj.getString("id"), tagObj.getString("name"));
            }

            if(!tagObj.getBoolean("moderated")) {
                DiscordPS.warning("The available tag '" + tagObj.getString("name") + "' does not have secure permission.");
                DiscordPS.warning("Please enable allowing only moderator to apply tag to ensure secured management.");
            }
        }
    }

    /**
     * Resolve all the tag identity by this plugin's file config.
     * (handled by {@link github.tintinkung.discordps.core.WebhookManager})
     * @param config The config file reference.
     * @throws NoSuchElementException If the config file entry does not exist for some tag.
     */
    public static void resolveAllTag(FileConfiguration config) throws NoSuchElementException {
        for(AvailableTags tag : AvailableTags.values()) tag.resolve(config);
    }

    /**
     * Apply all stored tag cache to {@link TagReference}
     * (handled by {@link github.tintinkung.discordps.core.WebhookManager})
     * @throws RuntimeException If the cache is not initialized properly.
     */
    public static void applyAllTag() throws RuntimeException {
        for(AvailableTags tag : AvailableTags.values()) tryApplyTag(tag);
    }

    private void resolve(@NotNull FileConfiguration config) throws NoSuchElementException {
        // Resolve tag ID
        this.tagID = config.getString(this.tagPath);
        if(tagID == null) throw new NoSuchElementException("Tag ID not configured for " + this.name());

        // Resolve tag's embed color
        try {
            String configColor = config.getString(this.colorPath);
            if(configColor != null) {
                byte[] colorHex = Hex.decodeHex(configColor.replace("#", ""));

                if(colorHex.length == colorSpace.getNumComponents()) {
                    float[] comp = new float[colorHex.length];

                    // Convert signed byte to unsigned (0 – 255)
                    for(int i = 0; i < colorHex.length; ++i) {
                        byte bit = colorHex[i];
                        comp[i] = (bit & 0xFF) / 255.0f;
                    }

                    this.color = new Color(colorSpace, comp, 1.0f);
                    DiscordPS.debug("Registered tag '" + tagID + "' with embed color " + this.color);
                    return;
                }
                else DiscordPS.error("The configured tag embed color '" + tagID + "' is invalid.");
            }
            DiscordPS.error("Embed color of tag '" + tagID + "' is not configured, fallback to default color.");
        } catch (DecoderException | IllegalArgumentException ex) {
            DiscordPS.error("Failed to parse configured embed color of tag '" + tagID + "', fallback to default color.", ex);
        }
    }

    private void apply(TagReference tag) {
        this.tagRef = tag;
    }

    private static void tryApplyTag(@NotNull AvailableTags tag) throws RuntimeException {
        RuntimeException error = new RuntimeException("The configured tag ID for '" + tag.name() + "' does not exist in the discord server, please check the config file.");
        if(tagCache.isEmpty()) throw new RuntimeException("Available Tags cache has not been initialized.");
        if(tag.tagID == null) throw new RuntimeException("Trying to apply tag enum '" + tag.name() + "' but its value has not been resolved, possibly missing config field.");

        try {
            // If provided tag is snowflake ID
            Checks.isSnowflake(tag.tagID, tag.name());

            if(tagCache.containsKey(tag.tagID)) {
                String tagName = tagCache.get(tag.tagID);
                DiscordPS.debug("Registered tag '" + tag.name() + "' for tag: " + tagName);

                tag.apply(new TagReference(tag.tagID, tagName));
            }
            else {
                DiscordPS.error(error);
                throw error;
            }
        }
        catch (IllegalArgumentException ex) {
            // If provided tag is a String name
            if(tagCache.containsValue(tag.tagID)) {
                String tagID = tagCache.getKey(tag.tagID);
                DiscordPS.debug("Registered tag '" + tag.name() + "' for tag: " + tag.tagID);

                tag.apply(new TagReference(tagID, tag.tagID));
            }
            else {
                DiscordPS.error(error);
                throw error;
            }
        }
    }

    /**
     * The discord API forum tag object since the DiscordSRV JDA version does not support it yet.
     * @see <a href="https://discord.com/developers/docs/resources/channel#forum-tag-object">Discord API</a>
     */
    public static class TagReference {
        private final String tagID;
        private final String name;

        public TagReference(String tagID, String name) {
            this.tagID = tagID;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String getID() {
            return tagID;
        }
    }
}
