package github.tintinkung.discordps.commands;

import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.interactions.OnSetupWebhook;
import github.tintinkung.discordps.core.system.io.lang.LangPaths;
import org.jetbrains.annotations.NotNull;

import static github.tintinkung.discordps.Constants.ORANGE;
import static github.tintinkung.discordps.core.system.io.lang.SetupWebhookCommand.EMBED_ALREADY_CONFIGURED;

class SetupShowcaseCommand extends SetupWebhookCommand {

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
