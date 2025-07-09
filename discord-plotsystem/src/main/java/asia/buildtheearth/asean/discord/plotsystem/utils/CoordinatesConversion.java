package asia.buildtheearth.asean.discord.plotsystem.utils;

import github.scarsz.discordsrv.dependencies.json.JSONObject;
import github.scarsz.discordsrv.dependencies.okhttp3.*;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;

@FunctionalInterface
interface CoordinatesConversion  {

    double[] convertToGeo(double xCords, double yCords) throws RuntimeException;

    //TODO: This doesn't work yet
    default double[] convertToGeo(HttpUrl httpURL) throws RuntimeException {
        Request.Builder requestBuilder = new Request.Builder()
                .url(httpURL)
                .header("User-Agent",
                        String.join("/",
                                DiscordPS.getPlugin().getName(),
                                DiscordPS.getPlugin().getDescription().getVersion()
                        )
                );
        OkHttpClient httpClient = DiscordPS.getPlugin().getJDA().getHttpClient();

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
                    DiscordPS.debug("Got BTE API response: " + body.string());
                } catch (Throwable ignored) {
                }
            }

        } catch (Exception e) {
            DiscordPS.error("Failed to deliver webhook message to Discord: " + e.getMessage());
        }

        return new double[]{0, 0};
    }
}

