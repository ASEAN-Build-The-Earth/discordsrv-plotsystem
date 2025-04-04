package github.tintinkung.discordps.core.utils;

import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.ConfigPaths;

import github.scarsz.discordsrv.dependencies.jda.api.exceptions.ParsingException;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.scarsz.discordsrv.objects.ExpiringDualHashBidiMap;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * Plot-System forum management tags
 * configured with {@link ConfigPaths}
 */
public enum AvailableTags {
    FINISHED(ConfigPaths.TAG_FINISHED),
    REJECTED(ConfigPaths.TAG_REJECTED),
    APPROVED(ConfigPaths.TAG_APPROVED),
    ARCHIVED(ConfigPaths.TAG_ARCHIVED);

    private static final ExpiringDualHashBidiMap<String, String> tagCache = new ExpiringDualHashBidiMap<>(TimeUnit.SECONDS.toMillis(10));

    private TagReference tagRef;

    private final String tagPath;

    @Nullable
    private String tagID;

    AvailableTags(String path) {
        tagPath = path;
    };

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
                DiscordPS.warning("The available tag " + tagObj.getString("name") + " does not have secure permission.");
                DiscordPS.warning("Please enable allowing only moderator to apply tag to ensure security.");
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
        this.tagID = config.getString(this.tagPath);
        if(tagID == null) throw new NoSuchElementException("Tag ID not configured for " + this.name());
    }

    private void apply(TagReference tag) {
        this.tagRef = tag;
    }

    private static void tryApplyTag(@NotNull AvailableTags tag) throws RuntimeException {
        RuntimeException error = new RuntimeException("The configured tag ID for " + tag.name() + " is invalid.");
        if(tagCache.isEmpty()) throw new RuntimeException("Available Tags cache has not been initialized.");
        if(tag.tagID == null) throw new RuntimeException("The tag " + tag.name() + " has not been resolved.");

        try {
            // If provided tag is snowflake ID
            Checks.isSnowflake(tag.tagID, tag.name());

            if(tagCache.containsKey(tag.tagID)) {
                String tagName = tagCache.get(tag.tagID);
                DiscordPS.info("Registered tag " + tag.name() + " for tag: " + tagName);

                tag.apply(new TagReference(tag.tagID, tagName));
            }
            else {
                DiscordPS.error(error);
                throw new RuntimeException(error);
            }
        }
        catch (IllegalArgumentException ex) {
            // If provided tag is a String name
            if(tagCache.containsValue(tag.tagID)) {
                String tagID = tagCache.getKey(tag.tagID);
                DiscordPS.info("Registered tag " + tag.name() + " for tag: " + tag.tagID);

                tag.apply(new TagReference(tagID, tag.tagID));
            }
            else {
                DiscordPS.error(error);
                throw error;
            }
        }
    }
}
