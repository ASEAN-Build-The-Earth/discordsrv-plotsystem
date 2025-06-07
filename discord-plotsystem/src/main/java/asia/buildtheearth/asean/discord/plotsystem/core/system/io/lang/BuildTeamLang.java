package asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang;

import asia.buildtheearth.asean.discord.plotsystem.core.system.io.MessageLang;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class BuildTeamLang {

    /**
     * Available build team country
     */
    private static final TeamLang BUILD_TEAM_COUNTRY = new TeamLang("build-team.country.");

    /**
     * Available build team project
     */
    private static final TeamLang BUILD_TEAM_PROJECT = new TeamLang("build-team.city-project.");

    public record TeamLang(String path) {

        @Contract(pure = true)
        public @NotNull MessageLang getName(String key) {
            return () -> path + key + ".name";
        }

        @Contract(pure = true)
        public @NotNull MessageLang getDescription(String key) {
            return () -> path + key + ".description";
        }
    }

    public static TeamLang getCountry() {
        return BUILD_TEAM_COUNTRY;
    }

    public static TeamLang getCityProject() {
        return BUILD_TEAM_PROJECT;
    }
}
