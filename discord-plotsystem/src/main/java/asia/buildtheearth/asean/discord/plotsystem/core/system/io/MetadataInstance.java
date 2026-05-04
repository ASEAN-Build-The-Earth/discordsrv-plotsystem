package asia.buildtheearth.asean.discord.plotsystem.core.system.io;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

public interface MetadataInstance {
    @Contract(" -> new") @NotNull @Unmodifiable MetadataInstance load();
}
