package github.tintinkung.discordps.utils;

import github.scarsz.discordsrv.dependencies.commons.codec.binary.Base64;
import github.scarsz.discordsrv.dependencies.commons.io.FilenameUtils;
import github.scarsz.discordsrv.dependencies.kevinsawicki.http.HttpRequest;
import github.tintinkung.discordps.DiscordPS;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class FileUtil {

    /**
     * Convert file data to base64 URI
     *
     * @param imageFile Image file that must have the content type as image/xxx
     * @return The base64 string formatted as "data:{@code content-type};base64,{@code binaries}"
     * @throws IOException If an exception occurred reading image file data
     */
    public static @NotNull String convertImageFileToDataURI(@NotNull File imageFile) throws IOException {
        // Detect content type (e.g. image/png, image/jpeg)
        String contentType = Files.probeContentType(imageFile.toPath());
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Unsupported or unknown image type: " + contentType);
        }

        // Read file into byte array
        FileInputStream inputStream = new FileInputStream(imageFile);
        byte[] imageBytes = inputStream.readAllBytes();
        inputStream.close();

        String base64 = Base64.encodeBase64String(imageBytes);
        return "data:" + contentType + ";base64," + base64;
    }

    /**
     * Checks if the base name of a file URL starts with the given filename prefix.
     *
     * @param url      The URL string (may include query parameters)
     * @param filename Filename to match with {@link String#startsWith(String)}
     * @param onSuccess  Invoked with the matched filename (excluding query) if succeeds
     * @param onFailure  Invoked with the original URL if the match fails
     */
    public static void matchFilenameByURL(@NotNull String url,
                                          @NotNull String filename,
                                          @NotNull Consumer<String> onSuccess,
                                          @NotNull Consumer<String> onFailure) {
        if (FilenameUtils.getBaseName(url).startsWith(filename))
            onSuccess.accept(getFilenameFromURL(url));
        else onFailure.accept(url);
    }

    /**
     * Get filename from a URL with query parameters excluded.
     * {@snippet :
     *     String url = "www.xxx.com/image.png?param=0";
     *     int queryStart = url.indexOf(63);
     *     String path = (queryStart >= 0) ? url.substring(0, queryStart) : url;
     *     return FilenameUtils.getName(path);
     * }
     * @param url The URL string
     * @return The url stripped down to its filename with extension
     */
    public static String getFilenameFromURL(@NotNull String url) {
        // 63: character '?' where URL query parameters must begin with
        int queryStart = url.indexOf(63);
        String path = (queryStart >= 0) ? url.substring(0, queryStart) : url;
        return FilenameUtils.getName(path);
    }

    /**
     * List all image files with a prefixed name within a folder.
     *
     * @param filePrefix The filename prefix to look for
     * @param folder The folder as {@link File} instance
     * @return All files with the given prefix that exist within given folder
     * @throws IOException If an IOException occurred reading file content-type
     */
    public static @NotNull List<File> findImagesFileByPrefix(@NotNull String filePrefix, @NotNull File folder) throws IOException {
        File[] files = folder.listFiles();
        if (files == null) throw new IOException("Could not list files in the plugin data folder");

        List<File> result = new ArrayList<>();
        for (File file : files) {
            if (file.getName().startsWith(filePrefix) && file.isFile()) {
                String mimeType = Files.probeContentType(file.toPath());
                if (mimeType != null && mimeType.startsWith("image/")) {
                    result.add(file);
                }
            }
        }
        return result;
    }

    /**
     * Get the first image file with a prefixed name within plugin's root folder
     *
     * @param filePrefix The filename prefix to look for
     * @return The first file with the given prefix that exist in plugin's root folder, else null
     * @throws IOException May be thrown by {@link #findImagesFileByPrefix(String,File)}
     */
    public static @Nullable File findImageFileByPrefix(String filePrefix) throws IOException {
        List<File> files = findImagesFileByPrefix(filePrefix, DiscordPS.getPlugin().getDataFolder());
        if(files.isEmpty()) return null;
        else return files.getFirst();
    }

    /**
     * List all image files with a prefixed name within plugin's root folder.
     *
     * @param filePrefix The filename prefix to look for
     * @return All files with the given prefix that exist within plugin's root folder.
     * @throws IOException May be thrown by {@link #findImagesFileByPrefix(String,File)}
     */
    public static @NotNull List<File> findImagesFileByPrefix(String filePrefix) throws IOException {
        return findImagesFileByPrefix(filePrefix, DiscordPS.getPlugin().getDataFolder());
    }

    public static String requestHttp(URL requestUrl) {
        try {
            return setTimeout(HttpRequest.get(requestUrl)).body();
        } catch (HttpRequest.HttpRequestException e) {
            DiscordPS.error("Failed to fetch URL " + requestUrl + ": " + e.getMessage());
            return "";
        }
    }

    public static String requestHttp(String requestUrl) {
        try {
            return setTimeout(HttpRequest.get(requestUrl)).body();
        } catch (HttpRequest.HttpRequestException e) {
            DiscordPS.error("Failed to fetch URL " + requestUrl + ": " + e.getMessage());
            return "";
        }
    }


    public static void downloadFile(URL requestUrl, File destination) throws HttpRequest.HttpRequestException {
        setTimeout(HttpRequest.get(requestUrl)).receive(destination);
    }

    public static void downloadFile(String requestUrl, File destination) throws HttpRequest.HttpRequestException {
        setTimeout(HttpRequest.get(requestUrl)).receive(destination);
    }

    public static boolean exists(String url) {
        try {
            HttpRequest request = setTimeout(HttpRequest.head(url));
            return request.code() / 100 == 2;
        } catch (Exception e) {
            return false;
        }
    }

    private static HttpRequest setTimeout(@NotNull HttpRequest httpRequest) {
        return httpRequest
                .connectTimeout(Math.toIntExact(TimeUnit.SECONDS.toMillis(30)))
                .readTimeout(Math.toIntExact(TimeUnit.SECONDS.toMillis(30)));
    }
}
