package github.tintinkung.discordps.core.system.components;

public class Separator extends ComponentV2 {
    public Separator(boolean divider) {
        super(14, "divider", divider, "spacing", 1);
    }

    public Separator() {
        super(14, "divider", true, "spacing", 1);
    }

    public Separator(int componentID, boolean divider) {
        super(14, componentID, "divider", divider, "spacing", 1);
    }

    public Separator(int componentID, boolean divider, boolean expandPadding) {
        super(14, componentID, "divider", divider, "spacing", expandPadding? 2 : 1);
    }

}
