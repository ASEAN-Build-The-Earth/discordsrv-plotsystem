package github.tintinkung.discordps.utils;

import github.scarsz.discordsrv.dependencies.okhttp3.HttpUrl;

import java.net.URL;

/**
 * DiscordSRV's resolution to getting avatar url doesn't work with crafatar host
 * (<a href="https://github.com/crafatar/crafatar/issues/322">Issue</a>).
 * Fix this with <a href="https://github.com/JellyLabScripts/FarmHelper/pull/248">mineatar</a> endpoint.
 */
public class AvatarUtil {

    public static final String HOST = "api.mineatar.io";
    public static final String PATH = "face";

    public static URL getAvatarUrl(String UUID, int imageSize, String format) {
        HttpUrl httpURL = new HttpUrl.Builder()
                .scheme("https")
                .host(HOST)
                .addPathSegment(PATH)
                .addPathSegments(UUID)
                .addQueryParameter("scale", String.valueOf(imageSize))
                .addQueryParameter("format", format)
                .build();

        return httpURL.url();
    }

    public static String getFullBodyUrl(String uuid) {
        return "https://api.mineatar.io/body/full/" + uuid + "?scale=256";
    }
}