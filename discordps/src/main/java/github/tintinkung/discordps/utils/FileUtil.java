package github.tintinkung.discordps.utils;

import github.scarsz.discordsrv.dependencies.commons.codec.binary.Base64;
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

public abstract class FileUtil {

    public static String convertImageFileToDataURI(File imageFile) throws IOException {
        // Detect content type (e.g. image/png, image/jpeg)
        String contentType = Files.probeContentType(imageFile.toPath());
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Unsupported or unknown image type: " + contentType);
        }

        // Read file into byte array
        FileInputStream inputStream = new FileInputStream(imageFile);
        byte[] imageBytes = inputStream.readAllBytes();
        inputStream.close();

        // Base64 encode
        String base64 = Base64.encodeBase64String(imageBytes);

        // Return the data URI
        return "data:" + contentType + ";base64," + base64;
    }

    public static List<File> findImagesFileByPrefix(String filePrefix, File folder) throws IOException {
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

    public static @Nullable File findImageFileByPrefix(String filePrefix) throws IOException {
        List<File> files = findImagesFileByPrefix(filePrefix, DiscordPS.getPlugin().getDataFolder());
        if(files.isEmpty()) return null;
        else return files.getFirst();
    }

    public static @NotNull List<File> findImagesFileByPrefix(String filePrefix) throws IOException {
        return findImagesFileByPrefix(filePrefix, DiscordPS.getPlugin().getDataFolder());
    }

    private static HttpRequest setTimeout(HttpRequest httpRequest) {
        return httpRequest
                .connectTimeout(Math.toIntExact(TimeUnit.SECONDS.toMillis(30)))
                .readTimeout(Math.toIntExact(TimeUnit.SECONDS.toMillis(30)));
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
}
