package github.tintinkung.discordps.core.utils;

import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.json.JSONObject;
import github.scarsz.discordsrv.dependencies.okhttp3.*;
import github.tintinkung.discordps.DiscordPS;

@FunctionalInterface
interface CoordinatesConversion  {

    double[] convertToGeo(double xCords, double yCords) throws RuntimeException;

    default double[] convertToGeo(HttpUrl httpURL) throws RuntimeException {
        Request.Builder requestBuilder = new Request.Builder()
                .url(httpURL)
                .header("User-Agent",
                        String.join("/",
                                DiscordPS.getPlugin().getName(),
                                DiscordPS.getPlugin().getDescription().getVersion()
                        )
                );
        OkHttpClient httpClient = DiscordSRV.getPlugin().getJda().getHttpClient();

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            int status = response.code();
            if (status == 404) {
                // 404 = Invalid Webhook (most likely to have been deleted)
                // return;
            }

            ResponseBody body = response.body();
            if(body != null) {
                try {
                    JSONObject jsonObj = new JSONObject(body.string());
                    DiscordPS.info("Got BTE API response: " + body.string());
                } catch (Throwable ignored) {
                }
            }

        } catch (Exception e) {
            DiscordSRV.error("Failed to deliver webhook message to Discord: " + e.getMessage());
            DiscordSRV.debug(Debug.MINECRAFT_TO_DISCORD, e);

        }

        return new double[]{0, 0};
    }
}

