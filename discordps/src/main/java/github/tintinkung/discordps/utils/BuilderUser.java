package github.tintinkung.discordps.utils;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.net.URL;

public class BuilderUser {

    @Deprecated
    public static String getAsAvatarURL(OfflinePlayer builder) {
        if (builder instanceof Player) {
            return DiscordSRV.getAvatarUrl((Player) builder);
        } else {
            return DiscordSRV.getAvatarUrl(builder.getName(), builder.getUniqueId());
        }
    }


    @Nullable
    public static Member getAsDiscordMember(OfflinePlayer builder) {
        String userId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(builder.getUniqueId());
        if(userId != null) return DiscordUtil.getMemberById(userId);
        else return null;
    }
}
