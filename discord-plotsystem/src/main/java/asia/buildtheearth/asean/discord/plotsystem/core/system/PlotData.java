package asia.buildtheearth.asean.discord.plotsystem.core.system;

import asia.buildtheearth.asean.discord.plotsystem.api.PlotCreateData;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.database.ThreadStatus;
import asia.buildtheearth.asean.discord.plotsystem.utils.CoordinatesUtil;
import asia.buildtheearth.asean.discord.plotsystem.utils.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static asia.buildtheearth.asean.discord.plotsystem.Constants.PLOT_IMAGE_FILE;

/**
 * Class responsible for managing plot information/metadata
 * provided by an initial sets of information provided by the API.
 *
 * <p>Data collections:</p>
 * <ul>
 *     <li>Parse {@link PlotCreateData initial} data for plot specific metadata.<ul>
 *         <li>Plot's initial status</li>
 *         <li>Plot's geographic location</li></ul></li>
 *     <li>Fetch plot's image file(s) in their respective data folder.</li>
 *     <li>Fetch owner's information such as their Discord and avatar info.</li>
 * </ul>
 */
public class PlotData extends MemberOwnable {

    private final Map<String, File> imageFiles;
    private final File imagesFolder;

    private final PlotCreateData plot;
    private final String geoCoordinates;
    private final String displayCords;

    private @NotNull ThreadStatus primaryStatus;
    private final @NotNull Set<Long> statusTags;

    /**
     * Construct a plot data using an initial create data.
     *
     * @param plot The initial information defining this plot.
     * @see PlotData
     */
    public PlotData(@NotNull PlotCreateData plot) {
        super(plot.ownerUUID());
        this.plot = plot;

        // Plot location
        double[] geoCords = plot.geoCoordinates() == null? new double[] {0, 0} : plot.geoCoordinates();
        this.geoCoordinates = CoordinatesUtil.formatGeoCoordinatesNumeric(geoCords);
        this.displayCords = CoordinatesUtil.formatGeoCoordinatesNSEW(geoCords);

        // Fetch plot specific image files
        this.imagesFolder = prepareMediaFolder(plot.plotID());
        this.imageFiles = new HashMap<>();
        this.fetchMediaFolder();

        // Plot Status
        this.primaryStatus = ThreadStatus.valueOf(plot.status().getName());
        this.statusTags = new HashSet<>(Collections.singletonList(primaryStatus.toTag().getTag().getIDLong()));
    }

    /**
     * Get the primary status of this plot.
     *
     * @return The thread status.
     */
    public @NotNull ThreadStatus getPrimaryStatus() {
        return this.primaryStatus;
    }

    /**
     * Set a new primary status for this plot data.
     *
     * @param status New primary status to set to.
     * @param override If {@code true}, will remove the previous status if existed.
     *                 Setting {@code false} will retain the previous status without it being primary status.
     */
    public void setPrimaryStatus(@NotNull ThreadStatus status, boolean override) {
        if(override)
            this.statusTags.remove(this.primaryStatus.toTag().getTag().getIDLong());
        this.statusTags.add(status.toTag().getTag().getIDLong());
        this.primaryStatus = status;
    }

    /**
     * Add a non-primary status tag to this plot data.
     *
     * <p>Added tag will appear on {@link #getStatusTags()}</p>
     * <p>Note: this tag will not get returned by {@link #getPrimaryStatus()}</p>
     * @param status A tag to add
     */
    public void addStatusTag(@NotNull ThreadStatus status) {
        this.statusTags.add(status.toTag().getTag().getIDLong());
    }

    /**
     * Get all status tag(s) applied to this plot.
     *
     * @return Set of the tag's snowflake ID long.
     */
    public @NotNull Set<Long> getStatusTags() {
        return this.statusTags;
    }

    /**
     * Get all media file(s) of this plot fetched initially by the constructor.
     *
     * <p>Use {@link #fetchMediaFolder()} to update the media file(s)</p>
     *
     * @return Collection of {@linkplain File} instance.
     */
    public Collection<File> getImageFiles() { return this.imageFiles.values(); }

    /**
     * Get the plot's location.
     *
     * @return Plot's location formatted as {@code NSEW}
     */
    public String getDisplayCords() {
        return displayCords;
    }

    /**
     * Get the plot's location
     *
     * @return Plot's location formatted as {@code lat, long}
     */
    public String getGeoCoordinates() {
        return geoCoordinates;
    }

    /**
     * Get the initial plot-create data provided during construction.
     *
     * @return Un-modified plot-create data instance.
     */
    public PlotCreateData getPlot() {
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
            DiscordPS.error("Failed to find plot's image files", ex);
        }
        return imageFiles;
    }
}
