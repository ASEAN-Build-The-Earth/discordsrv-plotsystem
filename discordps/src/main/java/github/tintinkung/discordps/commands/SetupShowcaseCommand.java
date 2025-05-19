package github.tintinkung.discordps.commands;

import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.interactions.OnSetupWebhook;
import org.jetbrains.annotations.NotNull;

class SetupShowcaseCommand extends SetupWebhookCommand {

    public SetupShowcaseCommand(@NotNull String name, @NotNull String outputFile, @NotNull String webhookChannel, @NotNull String webhookName) {
        super(name, outputFile, "Setup Plot-System showcase channel setup", webhookChannel, webhookName);
    }

    @Override
    public void onCommandTriggered(@NotNull InteractionHook hook, @NotNull OnSetupWebhook payload)  {
        // Exit if webhook already has been configured
        if(DiscordPS.getPlugin().getShowcase() != null) {
            hook.sendMessageEmbeds(ALREADY_CONFIGURED_ERROR.apply(getOutputPath().toString())).queue();
            DiscordPS.getPlugin().exitSlashCommand(payload.eventID);
            return;
        }

        this.onSetupWebhook(hook, payload);
    }
}
