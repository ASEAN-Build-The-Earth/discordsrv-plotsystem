package github.tintinkung.discordps.commands;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.commands.build.SubcommandData;
import github.tintinkung.discordps.Debug;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.events.SetupHelpEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.*;

final class SetupHelpCommand extends SubcommandData implements SetupHelpEvent {

    public SetupHelpCommand(@NotNull String name) {
        super(name, "See the Discord-PlotSystem integration checklist");
    }

    @Override
    public void onSetupHelp(InteractionHook hook) {
        // ChatGPT cooked all this, crazy right?
        Debug debugger = DiscordPS.getDebugger();

        EmbedBuilder titleEmbed = new EmbedBuilder();
        boolean hasWarning = debugger.hasAnyWarning();
        boolean isReady = DiscordPS.getPlugin().isReady();

        if (isReady) {
            if (!hasWarning) {
                titleEmbed
                    .setTitle(":white_check_mark: Discord-PlotSystem is Ready!")
                    .setDescription("The plugin is fully configured and ready for use.")
                    .setColor(Color.GREEN);

                    if(DiscordPS.getPlugin().isDebuggingEnabled()) {
                        titleEmbed.addField(":warning: Notices", "The configuration commands `/setup webhook` and `/setup help` "
                            + "are exposed to public use by default. To secure these commands, disable them in the config file at: ```"
                            + DiscordPS.getPlugin().getDataFolder().getAbsolutePath() + "/config.yml```\n"
                            + "Set the field `debugging: false`, then restart the plugin and the command will be disabled.",
                            false);
                    }
            } else {
                titleEmbed
                    .setTitle(":warning: Discord-PlotSystem is Ready (with warnings)")
                    .setDescription("The plugin is usable, but consider resolving these warnings.")
                    .setColor(Color.ORANGE);
            }
        } else {
            titleEmbed
                    .setTitle(":x: Discord-PlotSystem is NOT Ready!")
                    .setDescription("There are unresolved configuration or runtime errors preventing startup.")
                    .setColor(Color.RED);
        }

        // 🧾 Setup Checklist
        MessageEmbed checklistEmbedTitle = new EmbedBuilder()
                .setTitle("🧾 Setup Checklist")
                .setDescription("All checks must pass for full functionality.")
                .setColor(Color.CYAN)
                .build();

        boolean anyFailed = false;

        List<MessageEmbed> checklistEmbeds = new ArrayList<>();

        checklistEmbeds.add(checklistEmbedTitle);

        for (Debug.ErrorGroup group : Debug.ErrorGroup.values()) {
            EmbedBuilder groupEmbed = makeGroupEmbed(group, debugger);

            checklistEmbeds.add(groupEmbed.build());
        }

        // ⚠️ Warnings
        EmbedBuilder warningEmbed = null;
        if(hasWarning) {
            warningEmbed = new EmbedBuilder().setTitle(":warning:️ Warnings").setColor(Color.YELLOW.darker());
            for (Map.Entry<Debug.Warning, String> entry : debugger.allThrownWarnings()) {
                warningEmbed.addField("• " + entry.getKey().name(), entry.getValue(), false);
            }
        }

        // Send embeds
        hook.sendMessageEmbeds(titleEmbed.build()).setEphemeral(true).queue();
        hook.sendMessageEmbeds(checklistEmbeds).setEphemeral(true).queue();

        if (warningEmbed != null) {
            hook.sendMessageEmbeds(warningEmbed.build()).setEphemeral(true).queue();
        }
    }

    private @NotNull EmbedBuilder makeGroupEmbed(Debug.ErrorGroup group, Debug debugger) {
        EmbedBuilder groupEmbed = new EmbedBuilder();

        boolean failed = debugger.hasGroup(group);

        groupEmbed.setTitle(failed ? ":red_circle: " + group.getUnresolved() : ":green_circle: " + group.getResolved());
        groupEmbed.setColor(failed? Color.RED : Color.GREEN);
        groupEmbed.setDescription(getInstruction(group, failed));


        // Add each individual error in this group
        for (Map.Entry<Debug.Error, String> entry : debugger.allThrownErrors()) {

            // Find error in the group
            if (entry.getKey().getGroup() == group) {

                // Special message for tags configurations
                if(entry.getKey() == Debug.Error.WEBHOOK_TAG_VALIDATION_FAILED) {
                    groupEmbed.addField(
                        ":orange_circle: Webhook forum channel is missing required tags",
                        "> The field `available-tag` in `config.yml` must be configured "
                        + "with the available and existing tags in the forum channel. "
                        + "The value can be either the tag name or the snowflake ID.",
                        false);
                    continue;
                }

                groupEmbed.addField(
                    ":warning: " + entry.getKey().name().replace("_", " "),
                    "> " + entry.getValue(),
                    false);
            }
        }
        return groupEmbed;
    }

    public String getInstruction(Debug.ErrorGroup group, boolean failed) {
        String configPath = DiscordPS.getPlugin().getDataFolder().getAbsolutePath();

        return switch (group) {
            case CONFIG_VALIDATION -> failed
                    ? "An internal error occurred while loading the configuration files. This should not happen—please contact the developer for support."
                    : "Successfully validated configuration files at:\n"
                    + "`" + configPath + "/webhook.yml`\n"
                    + "`" + configPath + "/config.yml`";

            case PLUGIN_VALIDATION -> failed
                    ? "Failed to validate the DiscordSRV plugin. Please ensure it is installed and enabled on the server."
                    : "Successfully subscribed to the DiscordSRV API.";

            case DATABASE_CONNECTION -> failed
                    ? "Unable to connect to the Plot-System database. Please verify your settings in:\n"
                    + "`" + configPath + "/config.yml`"
                    : "Successfully connected to the Plot-System database.";

            case WEBHOOK_REFS_VALIDATION -> failed
                    ? "One or more required webhook configurations could not be validated. Please check your webhook setup."
                    : "All webhook configurations are valid.\n"
                    + "Source file: `" + configPath + "/webhook.yml`";

            case WEBHOOK_REFS_CONFIRMATION -> failed
                    ? "The configured webhook's channel could not be validated. Please ensure it is set to a proper forum channel and has the required tags."
                    : "Webhook channel successfully validated.";
        };
    }

}
