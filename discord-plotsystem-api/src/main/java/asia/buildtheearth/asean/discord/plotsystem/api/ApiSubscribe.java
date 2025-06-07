package asia.buildtheearth.asean.discord.plotsystem.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event subscriber in the DiscordPlotSystem event system.
 * <p>Any method annotated with {@code @ApiSubscribe} will be automatically
 * registered and called when an event matching its parameter type is fired.</p>
 * <p>The method must:</p>
 * <ul>
 *     <li>Be {@code public}</li>
 *     <li>Have exactly one parameter of type {@link asia.buildtheearth.asean.discord.plotsystem.api.events.ApiEvent}</li>
 *     <li>Be in a class registered with the event dispatcher {@link asia.buildtheearth.asean.discord.plotsystem.api.DiscordPlotSystemAPI#subscribe(Object)}</li>
 * </ul>
 *
 * <p>Example Usage</p>
 * <blockquote>{@snippet :
 * public class EventHandler {
 *     @ApiSubscribe
 *     public void onPlotCreate(asia.buildtheearth.asean.discord.plotsystem.api.events.PlotCreateEvent event) {
 *          DiscordPlotSystemAPI.info("Plot created: " + event.getData().toString());
 *     }
 * }}</blockquote>
 *
 * @see asia.buildtheearth.asean.discord.plotsystem.api.events.ApiEvent
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ApiSubscribe { }
