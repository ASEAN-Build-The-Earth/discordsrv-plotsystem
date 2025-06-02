package asia.buildtheearth.asean.discord.plotsystem.commands;

import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.commands.interactions.OnSetupWebhook;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.LangPaths;
import org.jetbrains.annotations.NotNull;

import static asia.buildtheearth.asean.discord.plotsystem.Constants.ORANGE;
import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.SetupWebhookCommand.EMBED_ALREADY_CONFIGURED;

final class SetupShowcaseCommand extends SetupWebhookCommand {

    public SetupShowcaseCommand(@NotNull String name, @NotNull String outputFile, @NotNull String webhookChannel, @NotNull String webhookName) {
        super(
            name,
            outputFile,
            webhookChannel,
            webhookName
        );
        this.setDescription(getLangManager().get(LangPaths.SETUP_SHOWCASE));
    }

    @Override
    public void onCommandTriggered(@NotNull InteractionHook hook, @NotNull OnSetupWebhook payload)  {
        // Exit if webhook already has been configured
        if(DiscordPS.getPlugin().getShowcase() != null) {
            MessageEmbed embed = getEmbed(
                    ORANGE.darker(),
                    EMBED_ALREADY_CONFIGURED,
                    getOutputPath().toString()
            );

            hook.sendMessageEmbeds(embed).queue();
            DiscordPS.getPlugin().exitSlashCommand(payload.eventID);
            return;
        }

        this.onSetupWebhook(hook, payload);
    }
}
