package github.tintinkung.discordps.core.utils;

/**
 * The discord API forum tag object since the DiscordSRV JDA version does not support it yet.
 * @see <a href="https://discord.com/developers/docs/resources/channel#forum-tag-object">Discord API</a>
 */
public class TagReference {
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