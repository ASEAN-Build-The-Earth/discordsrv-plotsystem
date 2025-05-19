package github.tintinkung.discordps.utils;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class MemberUtil {

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

    @Nullable
    public static OfflinePlayer getAsMinecraftPlayer(String discordID) {
        UUID userId = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(discordID);
        if(userId != null) return Bukkit.getOfflinePlayer(userId);
        else return null;
    }
}
