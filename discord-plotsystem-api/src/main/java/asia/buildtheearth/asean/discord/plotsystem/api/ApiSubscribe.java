package asia.buildtheearth.asean.discord.plotsystem.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event subscriber in the DiscordPlotSystem event system.
 *
 * @deprecated API is deprecated for official Plot-System's API module.
 *             DiscordPlotSystemAPI will no longer release starting by 1.3.0.
 */
@Deprecated(since = "1.2.5")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ApiSubscribe { }
