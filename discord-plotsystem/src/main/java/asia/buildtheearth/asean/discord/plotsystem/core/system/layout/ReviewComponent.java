package asia.buildtheearth.asean.discord.plotsystem.core.system.layout;

import asia.buildtheearth.asean.discord.components.api.*;
import asia.buildtheearth.asean.discord.components.api.Container;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.core.system.PlotData;
import asia.buildtheearth.asean.discord.plotsystem.utils.FileUtil;
import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Headless component that is display a container for review message of any plot.
 */
public class ReviewComponent implements LayoutComponent<Container> {

    /**
     * Pattern to match separator keyword in a message
     *
     * @see #applyContentList(List, Consumer)
     */
    public final static Pattern SEPARATOR_PATTERN = Pattern.compile("(?m)^((===)|(---)|(___))\\s*$");

    private final @NotNull List<String> content;
    private final @Nullable List<Message.Attachment> attachments;
    private final @Nullable Color accentColor;
    private final @Nullable List<File> reviewMedia;

    /**
     * Prepare a review component.
     *
     * @param rawContent The raw message content of this component.
     * @param attachments Optional attachment to apply to this message.
     * @param accentColor Optional accent color to apply to this message.
     * @param reviewMedia Optional media file to apply to this message.
     *
     * @see #build() To build the component
     */
    public ReviewComponent(@NotNull String rawContent,
                           @Nullable List<Message.Attachment> attachments,
                           @Nullable Color accentColor,
                           @Nullable List<File> reviewMedia) {

        this.content = parseRawContent(rawContent);
        this.attachments = attachments;
        this.accentColor = accentColor;
        this.reviewMedia = reviewMedia;
    }

    /**
     * Optionally get review media from snowflake ID.
     *
     * <p>Suppose a snowflake ID is appended at the end of the review file, the value will be found.</p>
     *
     * @param plotID The plotID to check for review media
     * @param reviewID The review snowflake ID that the file ends with
     * @return If found, a non-null optional of a list of media file(s)
     */
    public static Optional<List<File>> getOptMedia(int plotID, @Nullable String reviewID) {
        if(reviewID == null) return Optional.empty();

        File mediaFolder = PlotData.prepareMediaFolder(plotID);
        if(!mediaFolder.exists()) return Optional.empty();

        Optional<List<File>> optFiles;
        Predicate<File> filter = file -> file.isFile() && FileUtil.getFilenameFromFile(file).endsWith(reviewID);
        Function<List<File>, Optional<List<File>>> mapper = files -> {
            List<File> filtered = files.stream().filter(filter).toList();
            if(filtered.isEmpty()) return Optional.empty();
            else return Optional.of(filtered);
        };

        try {
            List<File> files = FileUtil.findImagesFileByPrefix(
                Constants.PLOT_REVIEW_IMAGE_FILE, mediaFolder
            );
            if(files.isEmpty()) optFiles = Optional.empty();
            else optFiles = Optional.of(files);
        }
        catch (IOException ignored) {
            optFiles = Optional.empty();
        }

        return optFiles.flatMap(mapper);
    }

    /**
     * Apply parsed content list to {@link ComponentV2}.
     *
     * <p>The applier ignore blank string and transform every index into a {@link TextDisplay}.</p>
     * <p>With additional {@link Separator} component for the following match:
     * <ul><li>{@code ___} For a hidden separator</li>
     *     <li>{@code ---} For a normal separator</li>
     *     <li>{@code ===} For a normal separator with expanded padding</li>
     * </ul></p>
     *
     * @param content The content list.
     * @param eachComponent The applier on each parsed member.
     */
    public static void applyContentList(@NotNull List<String> content,
                                        @NotNull Consumer<ComponentV2> eachComponent) {
        for (int i = 0; i < content.size(); i++) {
            if(StringUtils.isBlank(content.get(i))) continue;

            switch (content.get(i)) {
                case "___" -> eachComponent.accept(new Separator(i, false));
                case "---" -> eachComponent.accept(new Separator(i, true));
                case "===" -> eachComponent.accept(new Separator(i, true, true));
                default -> eachComponent.accept(new TextDisplay(i, content.get(i)));
            }
        }
    }

    /**
     * Parse raw content to a list of string splitting any separator keyword.
     *
     * @param rawContent The raw message content
     * @return Split message content
     */
    public static @NotNull List<String> parseRawContent(String rawContent) {
        Matcher matcher = SEPARATOR_PATTERN.matcher(rawContent);

        List<String> messages = new ArrayList<>();

        int lastEnd = 0;
        while (matcher.find()) {
            String before = rawContent.substring(lastEnd, matcher.start()).strip();

            if (!before.isEmpty()) messages.add(before);

            messages.add(matcher.group(1).strip());
            lastEnd = matcher.end();
        }

        if (lastEnd < rawContent.length()) {
            String after = rawContent.substring(lastEnd).strip();
            if (!after.isEmpty()) messages.add(after);
        }
        return messages;
    }

    @Override
    public Container build() {
        Container container = new Container();

        applyContentList(this.content, container::addComponent);

        if(this.accentColor != null)
            container.setAccentColor(this.accentColor);

        Optional<List<File>> optMedia = Optional.ofNullable(this.reviewMedia);

        List<String> mediaFiles = new ArrayList<>();

        optMedia.ifPresent(files -> files.forEach(file -> mediaFiles.add("attachment://" + file.getName())));

        if(this.attachments != null && !this.attachments.isEmpty())
            this.attachments.forEach(attachment -> mediaFiles.add(attachment.getUrl()));

        if(!mediaFiles.isEmpty()) {
            MediaGallery media = new MediaGallery(this.content.size());
            mediaFiles.forEach(media::addMedia);
            container.addComponent(media);
        }

        return container;
    }
}
