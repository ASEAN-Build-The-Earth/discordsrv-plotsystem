package github.tintinkung.discordps.core.system;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.scarsz.discordsrv.dependencies.kevinsawicki.http.HttpRequest;
import github.tintinkung.discordps.Constants;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.core.database.PlotEntry;
import github.tintinkung.discordps.core.database.ThreadStatus;
import github.tintinkung.discordps.core.system.embeds.ImageEmbed;
import github.tintinkung.discordps.core.system.embeds.InfoEmbed;
import github.tintinkung.discordps.core.system.embeds.PlotDataEmbed;
import github.tintinkung.discordps.core.system.embeds.StatusEmbed;
import github.tintinkung.discordps.utils.AvatarUtil;
import github.tintinkung.discordps.utils.BuilderUser;
import github.tintinkung.discordps.utils.CoordinatesUtil;
import github.tintinkung.discordps.utils.FileUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;

import static github.tintinkung.discordps.Constants.PLOT_IMAGE_FILE;

public class PlotData {

    private static final String AVATAR_FORMAT = "png";
    private static final int AVATAR_SIZE = 16;

    private final OfflinePlayer owner;

    private final @Nullable Member ownerDiscord;
    private final @Nullable File avatarFile;
    private final List<File> imageFiles;
    private final File imagesFolder;

    private final PlotEntry plot;
    private final ThreadStatus status;
    private final String geoCoordinates;
    private final String displayCords;
    private final Set<Long> statusTags;
    private final URL avatarURL;


    public PlotData(@NotNull PlotEntry plot) {
        this.plot = plot;

        // Plot owner
        this.owner = Bukkit.getOfflinePlayer(UUID.fromString(plot.ownerUUID()));
        this.ownerDiscord = BuilderUser.getAsDiscordMember(owner);

        // Plot location
        String[] mcLocation = plot.mcCoordinates().split(",");

        double xCords = Double.parseDouble(mcLocation[0].trim());
        double zCords = Double.parseDouble(mcLocation[2].trim());
        double[] geoCords = CoordinatesUtil.convertToGeo(xCords, zCords);

        this.geoCoordinates = CoordinatesUtil.formatGeoCoordinatesNumeric(geoCords);
        this.displayCords = CoordinatesUtil.formatGeoCoordinatesNSEW(geoCords);

        // Builder avatar image (storing at /media/UUID/avatar-image.png)
        this.avatarURL = AvatarUtil.getAvatarUrl(plot.ownerUUID(), AVATAR_SIZE, AVATAR_FORMAT);
        File avatarFileLocation = prepareAvatarFile(plot.ownerUUID(), AVATAR_FORMAT);

        if(downloadAvatarToFile(avatarURL, avatarFileLocation))
            this.avatarFile = avatarFileLocation;
        else this.avatarFile = null;

        // Fetch plot specific image files
        this.imagesFolder = prepareMediaFolder(plot.plotID());
        this.imageFiles = checkMediaFolder(this.imagesFolder);

        // Plot Status
        this.status = ThreadStatus.toPlotStatus(plot.status());
        String tagID = status.toTag().getTag().getID();

        Checks.isSnowflake(tagID, "Forum Tag ID");

        this.statusTags = Set.of(Long.parseUnsignedLong(tagID));
    }

    @Contract("-> new")
    public PlotDataEmbedBuilder prepareEmbed() {
        return new PlotDataEmbedBuilder(new InfoEmbed(this), new StatusEmbed((this.status)));
    }

    public OfflinePlayer getOwner() {
        return this.owner;
    }

    public boolean isOwnerHasDiscord() {
        return this.ownerDiscord != null;
    }

    public Optional<Member> getOwnerDiscord() {
        return Optional.ofNullable(this.ownerDiscord);
    }

    public String getOwnerMentionOrName() {
        StringBuilder mention = new StringBuilder();
        getOwnerDiscord().ifPresentOrElse(
                (discord) -> mention.append("<@").append(discord.getId()).append(">"),
                () -> mention.append(getOwner().getName()));
        return mention.toString();
    }

    public String formatOwnerName() {
        return ownerDiscord != null? formatOwnerName(ownerDiscord) : getOwner().getName();
    }

    public String formatOwnerName(@NotNull Member owner) {
        return "@" + owner.getEffectiveName() + " (" + getOwner().getName() + ")";
    }

    public ThreadStatus getStatus() {
        return this.status;
    }

    public Set<Long> getStatusTags() {
        return this.statusTags;
    }

    public Optional<File> getAvatarFile() { return Optional.ofNullable(this.avatarFile); }

    public List<File> getImageFiles() { return this.imageFiles; }

    public String getDisplayCords() {
        return displayCords;
    }

    public String getGeoCoordinates() {
        return geoCoordinates;
    }

    public PlotEntry getPlot() {
        return plot;
    }

    public @NotNull URL getAvatarURL() {
        return avatarURL;
    }

    public @NotNull File prepareAvatarFile(String playerUUID, String format) {
        // DataFolder/media/cache/UUID/avatar-image.jpg
        Path mediaPath = DiscordPS.getPlugin().getDataFolder().toPath().resolve("media/cache/" + playerUUID);

        // Make player's directory if not exist
        if(mediaPath.toFile().mkdirs()) DiscordPS.debug("Created player media cache for UUID: " + mediaPath);


        return mediaPath.resolve( Constants.BUILDER_AVATAR_FILE + "." + format).toFile();
    }

    /**
     * Fetch this plot's media folder into {@link #getImageFiles()}
     *
     * @return True if the list is modified after fetching.
     */
    public boolean fetchMediaFolder() {
        return this.imageFiles.addAll(checkMediaFolder(this.imagesFolder));
    }

    public static @NotNull File prepareMediaFolder(int plotID) {
        Path imagesPath = DiscordPS.getPlugin().getDataFolder().toPath().resolve("media/plot-" + plotID);

        if(imagesPath.toFile().mkdirs()) DiscordPS.debug("Created plot media folder for id: " + plotID);

        return imagesPath.toFile();
    }

    public static @NotNull List<File> checkMediaFolder(int plotID) {
        return checkMediaFolder(prepareMediaFolder(plotID));
    }

    public static @NotNull List<File> checkMediaFolder(File folder) {
        List<File> imageFiles = new ArrayList<>();
        try {
            imageFiles.addAll(FileUtil.findImagesFileByPrefix(PLOT_IMAGE_FILE, folder));
        }
        catch (IOException ex) {
            DiscordPS.error("Failed to find plot's image files");
        }
        return imageFiles;
    }

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

    @Deprecated
    public static class PlotDataEmbedBuilder {
        private final InfoEmbed infoEmbed;
        private final StatusEmbed statusEmbed;
        private final List<PlotDataEmbed> embeds;

        private final List<ImageEmbed> imageEmbeds = new ArrayList<>(4);

        public PlotDataEmbedBuilder(@NotNull InfoEmbed info, @NotNull StatusEmbed status, ImageEmbed... images) {
            this.embeds = new ArrayList<>();
            this.embeds.add(this.infoEmbed = info);
            this.embeds.add(this.statusEmbed = status);

            for(ImageEmbed image : images) {
                imageEmbeds.add(image);
                this.embeds.add(image);
            }
        }

        public InfoEmbed getInfoEmbed() {
            return infoEmbed;
        }

        public StatusEmbed getStatusEmbed() {
            return statusEmbed;
        }

        public List<ImageEmbed> getImageEmbeds() {
            return imageEmbeds;
        }

        public List<MessageEmbed> build() {
            return embeds.stream().map(PlotDataEmbed::build).toList();
        }

    }
}
