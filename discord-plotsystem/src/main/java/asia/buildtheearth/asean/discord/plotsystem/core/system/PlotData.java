package asia.buildtheearth.asean.discord.plotsystem.core.system;

import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.database.PlotEntry;
import asia.buildtheearth.asean.discord.plotsystem.core.database.ThreadStatus;
import asia.buildtheearth.asean.discord.plotsystem.core.system.embeds.ImageEmbed;
import asia.buildtheearth.asean.discord.plotsystem.core.system.embeds.InfoEmbed;
import asia.buildtheearth.asean.discord.plotsystem.core.system.embeds.PlotDataEmbed;
import asia.buildtheearth.asean.discord.plotsystem.core.system.embeds.StatusEmbed;
import asia.buildtheearth.asean.discord.plotsystem.utils.CoordinatesUtil;
import asia.buildtheearth.asean.discord.plotsystem.utils.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static asia.buildtheearth.asean.discord.plotsystem.Constants.PLOT_IMAGE_FILE;

public class PlotData extends MemberOwnable {

    private final Map<String, File> imageFiles;
    private final File imagesFolder;

    private final PlotEntry plot;
    private final String geoCoordinates;
    private final String displayCords;

    private @NotNull ThreadStatus primaryStatus;
    private final @NotNull Set<Long> statusTags;

    public PlotData(@NotNull PlotEntry plot) {
        super(plot.ownerUUID());
        this.plot = plot;

        // Plot location
        String[] mcLocation = plot.mcCoordinates().split(",");

        double xCords = Double.parseDouble(mcLocation[0].trim());
        double zCords = Double.parseDouble(mcLocation[2].trim());
        double[] geoCords = CoordinatesUtil.convertToGeo(xCords, zCords);

        this.geoCoordinates = CoordinatesUtil.formatGeoCoordinatesNumeric(geoCords);
        this.displayCords = CoordinatesUtil.formatGeoCoordinatesNSEW(geoCords);

        // Fetch plot specific image files
        this.imagesFolder = prepareMediaFolder(plot.plotID());
        this.imageFiles = new HashMap<>();
        this.fetchMediaFolder();

        // Plot Status
        this.primaryStatus = ThreadStatus.fromPlotStatus(plot.status());
        this.statusTags = new HashSet<>(Collections.singletonList(primaryStatus.toTag().getTag().getIDLong()));
    }

//    @Contract("-> new")
//    public PlotDataEmbedBuilder prepareEmbed() {
//        return new PlotDataEmbedBuilder(new InfoEmbed(this), new StatusEmbed((this.primaryStatus)));
//    }

    public @NotNull ThreadStatus getPrimaryStatus() {
        return this.primaryStatus;
    }

    public void setPrimaryStatus(@NotNull ThreadStatus status, boolean override) {
        if(override)
            this.statusTags.remove(this.primaryStatus.toTag().getTag().getIDLong());
        this.statusTags.add(status.toTag().getTag().getIDLong());
        this.primaryStatus = status;
    }

    public void addStatusTag(@NotNull ThreadStatus status) {
        this.statusTags.add(status.toTag().getTag().getIDLong());
    }

    public @NotNull Set<Long> getStatusTags() {
        return this.statusTags;
    }

    public Collection<File> getImageFiles() { return this.imageFiles.values(); }

    public String getDisplayCords() {
        return displayCords;
    }

    public String getGeoCoordinates() {
        return geoCoordinates;
    }

    public PlotEntry getPlot() {
        return plot;
    }

    /**
     * Fetch this plot's media folder into {@link #getImageFiles()}
     *
     * @return True if the list is modified after fetching.
     */
    public boolean fetchMediaFolder() {
        return fetchMediaFolder(
            checkMediaFolder(this.imagesFolder),
            file -> this.imageFiles.putIfAbsent(file.getName(), file) == null
        );
    }

    /**
     * Check the media folder of a plot ID
     *
     * @param plotID The plot ID to look for
     * @return All image files in this plot's media folder
     */
    public static @NotNull List<File> checkMediaFolder(int plotID) {
        return checkMediaFolder(prepareMediaFolder(plotID));
    }

    /**
     * Fetch a media list with a fetcher and return true if the media is modified.
     *
     * @param media The media data to fetch as a list of {@link File}
     * @param fetcher The fetcher which will be invoked on each of the media file that must return {@link Boolean modified}
     * @return True if the fetcher returns True to any of the media files
     */
    public static boolean fetchMediaFolder(@NotNull List<File> media, Function<File, Boolean> fetcher) {
        boolean modified = false;
        for (File file : media)
            if (fetcher.apply(file))
                modified = true;
        return modified;
    }

    /**
     * Return plot media folder by ID and create if not exist.
     *
     * @param plotID The plot ID to look for
     * @return The media folder as {@link File} instance
     */
    public static @NotNull File prepareMediaFolder(int plotID) {
        return DiscordPS.getPlugin().getDataFolder().toPath().resolve("media/plot-" + plotID).toFile();
    }

    /**
     * Check for all image files in the given media folder
     *
     * @param folder The plot's media folder that follows {@code /media/plot-xx/}
     * @return All image files with the prefix {@link Constants#PLOT_IMAGE_FILE} within the media folder
     */
    public static @NotNull List<File> checkMediaFolder(@NotNull File folder) {
        List<File> imageFiles = new ArrayList<>();
        if(!folder.exists()) return imageFiles;
        try {
            imageFiles.addAll(FileUtil.findImagesFileByPrefix(PLOT_IMAGE_FILE, folder));
        }
        catch (IOException ex) {
            DiscordPS.error("Failed to find plot's image files");
        }
        return imageFiles;
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
