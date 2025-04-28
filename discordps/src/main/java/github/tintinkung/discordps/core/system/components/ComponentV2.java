package github.tintinkung.discordps.core.system.components;

import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ComponentV2 extends DataObject {

    public ComponentV2(int type, @NotNull String k1, @NotNull Object v1) {
        super(new HashMap<>(Map.of("type", type, k1, v1)));
    }

    public ComponentV2(int type, @NotNull String k1, @NotNull Object v1, @NotNull String k2, @NotNull Object v2) {
        super(new HashMap<>(Map.of("type", type, k1, v1, k2, v2)));
    }

    public ComponentV2(int type, int id) {
        super(new HashMap<>(Map.of("type", type, "id", id)));
    }

    public ComponentV2(int type, int id, @NotNull String k1, @NotNull Object v1) {
        super(new HashMap<>(Map.of("type", type, "id", id, k1, v1)));
    }

    public ComponentV2(int type, int id, @NotNull String k1, @NotNull Object v1, @NotNull String k2, @NotNull Object v2) {
        super(new HashMap<>(Map.of("type", type, "id", id, k1, v1, k2, v2)));
    }

    public ComponentV2(int type, int id, @NotNull String k1, @NotNull Object v1, @NotNull String k2, @NotNull Object v2, @NotNull String k3, @NotNull Object v3) {
        super(new HashMap<>(Map.of("type", type, "id", id, k1, v1, k2, v2, k3, v3)));
    }
}
