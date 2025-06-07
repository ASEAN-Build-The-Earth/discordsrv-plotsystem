package asia.buildtheearth.asean.discord.plotsystem.core.system.buttons;

import asia.buildtheearth.asean.discord.components.WebhookDataBuilder;
import asia.buildtheearth.asean.discord.components.api.ComponentV2;
import asia.buildtheearth.asean.discord.components.api.TextDisplay;
import asia.buildtheearth.asean.discord.plotsystem.core.system.layout.ReviewComponent;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.components.buttons.PluginButton;
import asia.buildtheearth.asean.discord.components.buttons.PluginButtonHandler;
import asia.buildtheearth.asean.discord.components.buttons.SimpleButtonHandler;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotInformation;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.PlotInteraction;
import github.scarsz.discordsrv.dependencies.jda.api.requests.RestAction;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.RestActionImpl;
import github.scarsz.discordsrv.dependencies.jda.internal.requests.Route;
import github.scarsz.discordsrv.dependencies.okhttp3.MultipartBody;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class PlotHelp implements PluginButtonHandler, SimpleButtonHandler {
    @Override
    public void onInteracted(PluginButton button, @NotNull ButtonClickEvent event) {
        event.deferReply(true).queue();

        String title = DiscordPS.getMessagesLang().get(PlotInformation.HELP_LABEL);
        String content = DiscordPS.getMessagesLang().get(PlotInformation.HELP_CONTENT);

        Route.CompiledRoute route = Route.Interactions.CREATE_FOLLOWUP.compile(
                DiscordSRV.getPlugin().getJda().getSelfUser().getApplicationId(),
                event.getInteraction().getToken()
        ).withQueryParams("with_components", String.valueOf(true));

        // Borrow display from review component
        ReviewComponent component = new ReviewComponent(content, null, Constants.BLUE, null);

        Collection<ComponentV2> components = StringUtils.isBlank(title)
                ? Collections.singletonList(component.build())
                : List.of(new TextDisplay(title), component.build());

        WebhookDataBuilder.WebhookData webhookData = new WebhookDataBuilder()
                .setComponentsV2(components)
                .forceComponentV2()
                .build();

        // Prepare the request action
        MultipartBody requestBody = webhookData.prepareRequestBody();
        RestAction<Object> action = new RestActionImpl<>(DiscordSRV.getPlugin().getJda(), route, requestBody);

        // Queue the action and handle for error
        action.submit().whenComplete((ok, error) -> {
            if(error == null) return;

            // Fallback to normal embed if ComponentV2 failed
            MessageEmbed helpEmbed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(content)
                .setColor(Constants.GREEN)
                .build();

            event.deferReply(true).addEmbeds(helpEmbed).submit().exceptionally(failure -> {
                event.getHook().sendMessageEmbeds(getErrorEmbed()).queue();
                DiscordPS.error("Exception occurred trying to send plot help message as an embed.", failure);
                return null;
            });
            DiscordPS.error("Exception occurred trying to send plot help message as ComponentV2.", error);
        });
    }

    private @NotNull MessageEmbed getErrorEmbed() {
        return DiscordPS.getMessagesLang()
            .getEmbedBuilder(PlotInteraction.HELP_GET_FAILURE)
            .setColor(Constants.RED)
            .build();
    }

    @Override
    public void onInteractedBadOwner(@NotNull ButtonClickEvent event) {
        MessageEmbed notOwnerEmbed = DiscordPS.getMessagesLang()
                .getEmbedBuilder(PlotInteraction.INTERACTED_BAD_OWNER)
                .setColor(Constants.RED)
                .build();
        event.deferReply(true).addEmbeds(notOwnerEmbed).queue();
    }
}
