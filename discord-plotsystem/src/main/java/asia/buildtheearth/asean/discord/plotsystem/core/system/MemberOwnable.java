package asia.buildtheearth.asean.discord.plotsystem.core.system;

import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.kevinsawicki.http.HttpRequest;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.utils.AvatarUtil;
import asia.buildtheearth.asean.discord.plotsystem.utils.FileUtil;
import asia.buildtheearth.asean.discord.plotsystem.utils.MemberUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/**
 * Superclass of anything own-able by plot-system member
 */
public class MemberOwnable {

    private static final String AVATAR_FORMAT = "png";
    private static final int AVATAR_SIZE = 16;

    private final OfflinePlayer owner;
    private final URL avatarURL;

    private final @Nullable File avatarFile;
    private final @Nullable Member ownerDiscord;

    /**
     * Create an owned entity with String formatted {@link java.util.UUID}
     *
     * @param memberUUID The member UUID that must be parsable by {@link java.util.UUID#fromString(String)}
     */
    @Contract("null -> fail")
    public MemberOwnable(String memberUUID) {
        if(memberUUID == null) throw new IllegalArgumentException("Ownable object must have Non-null member UUID");

        this.owner = Bukkit.getOfflinePlayer(UUID.fromString(memberUUID));
        this.ownerDiscord = MemberUtil.getAsDiscordMember(owner);

        // Builder avatar image (storing at /media/cache/UUID/avatar-image-UUID.png)
        this.avatarURL = AvatarUtil.getAvatarUrl(memberUUID, AVATAR_SIZE, AVATAR_FORMAT);
        File avatarFileLocation = prepareAvatarFile(memberUUID);

        if(downloadAvatarToFile(avatarURL, avatarFileLocation))
            this.avatarFile = avatarFileLocation;
        else this.avatarFile = null;
    }

    /**
     * Get the owned member
     *
     * @return Minecraft OfflinePlayer
     */
    public OfflinePlayer getOwner() {
        return this.owner;
    }

    /**
     * Is the owned member has discord account linked via {@link github.scarsz.discordsrv.objects.managers.AccountLinkManager AccountLinkManager}
     *
     * @return Whether owner's discord is null or not
     */
    public boolean isOwnerHasDiscord() {
        return this.ownerDiscord != null;
    }

    /**
     * Get the owner discord
     *
     * @return Optional {@link Member} object
     */
    public Optional<Member> getOwnerDiscord() {
        return Optional.ofNullable(this.ownerDiscord);
    }

    /**
     * Format owner as discord mention {@code <@snowflake>} or minecraft player name if not exist.
     *
     * @return Mentionable or the minecraft player name
     */
    public String getOwnerMentionOrName() {
        StringBuilder mention = new StringBuilder();
        getOwnerDiscord().ifPresentOrElse(
                (discord) -> mention.append("<@").append(discord.getId()).append(">"),
                () -> mention.append(getOwner().getName()));
        return mention.toString();
    }

    /**
     * Formats the owner's name for display.
     * <p>
     * If the owner's Discord information is available, the result will be:
     * {@code @discord-name (minecraft-username)}. Otherwise, it will fall back to:
     * {@code @minecraft-username}.
     *
     * @return the formatted owner name
     */
    public String formatOwnerName() {
        return ownerDiscord != null? formatOwnerName(ownerDiscord) : "@" + getOwner().getName();
    }

    /**
     * Format member's name when there exist {@link Member Discord Member} instance of this member
     *
     * @param owner The discord member instance of this member
     * @return {@code @discord-name (minecraft-username)}
     */
    public String formatOwnerName(@NotNull Member owner) {
        return "@" + owner.getUser().getName() + " (" + getOwner().getName() + ")";
    }

    /**
     * Get the member's downloadable avatar URL
     *
     * @return The {@link URL} created with this member's UUID
     * @see AvatarUtil#getAvatarUrl(String, int, String)
     */
    public @NotNull URL getAvatarURL() {
        return this.avatarURL;
    }

    /**
     * Get the member's avatar file if existed
     *
     * @return The optional file
     */
    public Optional<File> getAvatarFile() {
        return Optional.ofNullable(this.avatarFile);
    }

    /**
     * Try to get attachment url if this member's avatar file exist.
     *
     * @return The attachment url {@code attachment://avatar-image.png}
     *         which will fall back to https URL if this member's avatar file does not exist.
     */
    public String getAvatarAttachmentOrURL() {
        if(this.getAvatarFile().isPresent())
            return "attachment://" + this.getAvatarFile().get().getName();
        else return this.getAvatarURL().toString();
    }

    /**
     * Resolve for member's avatar file from their UUID
     *
     * @param memberUUID The string formatted {@link java.util.UUID}
     * @return The resolved member's avatar file location
     */
    private @NotNull File prepareAvatarFile(String memberUUID) {
        // DataFolder/media/cache/UUID/avatar-image.jpg
        Path mediaPath = DiscordPS.getPlugin().getDataFolder().toPath().resolve("media/cache/" + memberUUID);

        // Make player's directory if not exist
        if(mediaPath.toFile().mkdirs()) DiscordPS.debug("Created player media cache for UUID: " + mediaPath);

        String fileSuffix = this.getOwnerDiscord().isPresent()
                ? this.getOwnerDiscord().get().getId()
                : this.getOwner().getUniqueId().toString();

        return mediaPath.resolve( Constants.BUILDER_AVATAR_FILE  + "-" + fileSuffix + "." + AVATAR_FORMAT).toFile();
    }

    /**
     * Create and download avatar file from a URL if existed.
     *
     * @param avatarURL The avatar URL to download to
     * @param avatarFile The avatar file location to be written as
     * @return <p>{@code true} if the avatar file already exist, or the file successfully downloaded to the given location.</p>
     *         <p>{@code false} if either {@link HttpRequest.HttpRequestException} or {@link IOException} occurred during the process.</p>
     */
    private boolean downloadAvatarToFile(URL avatarURL, @NotNull File avatarFile) {
        // Try download player's minecraft avatar
        try {
            // Download if not exist
            if(avatarFile.createNewFile())
                FileUtil.downloadFile(avatarURL, avatarFile);

            return true;
        }
        catch (HttpRequest.HttpRequestException ex) {
            DiscordPS.error("Failed to download URL for player avatar image: " + ex.getMessage(), ex);
        }
        catch (IOException ex) {
            DiscordPS.error("IO Exception occurred trying to read player media folder at: "
                    + avatarFile.getAbsolutePath() + ": " + ex.getMessage(), ex);
        }
        return false;
    }
}