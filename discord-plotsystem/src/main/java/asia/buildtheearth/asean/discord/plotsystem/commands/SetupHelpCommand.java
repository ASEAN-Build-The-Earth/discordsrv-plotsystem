package asia.buildtheearth.asean.discord.plotsystem.commands;

import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.DebuggingMessage;
import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.InteractionHook;
import asia.buildtheearth.asean.discord.plotsystem.Constants;
import asia.buildtheearth.asean.discord.plotsystem.Debug;
import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.commands.interactions.Interaction;
import asia.buildtheearth.asean.discord.plotsystem.commands.providers.SystemCommandProvider;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.LanguageFile;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.SystemLang;
import asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.Format;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.SetupHelpCommand.*;

final class SetupHelpCommand extends SystemCommandProvider<Interaction,
        asia.buildtheearth.asean.discord.plotsystem.core.system.io.lang.SetupHelpCommand> {

    public SetupHelpCommand(@NotNull String name) {
        super(name, LanguageFile.NULL_LANG);
        this.setDescription(getLang(DESC));
    }

    @Override
    public void onCommandTriggered(InteractionHook hook) {
        Debug debugger = DiscordPS.getDebugger();

        EmbedBuilder titleEmbed = new EmbedBuilder();
        boolean hasWarning = debugger.hasAnyWarning();
        boolean isReady = DiscordPS.getPlugin().isReady();

        if (isReady) {
            if (!hasWarning) {
                LanguageFile.EmbedLang ready = getEmbed(EMBED_READY);
                titleEmbed
                    .setTitle(ready.title())
                    .setDescription(ready.description())
                    .setColor(Constants.GREEN);

                    if(DiscordPS.getPlugin().isDebuggingEnabled()) {
                        LanguageFile.EmbedLang notices = getEmbed(EMBED_DEBUGGING_NOTICE);
                        titleEmbed.addField(notices.title(), notices.description(), false);
                    }
            } else {
                LanguageFile.EmbedLang ready = getEmbed(EMBED_READY_WITH_WARNING);
                titleEmbed
                    .setTitle(ready.title())
                    .setDescription(ready.description())
                    .setColor(Constants.ORANGE);
            }
        } else {
            LanguageFile.EmbedLang notReady = getEmbed(EMBED_NOT_READY);
            titleEmbed
                .setTitle(notReady.title())
                .setDescription(notReady.description())
                .setColor(Constants.RED);
        }

        // 🧾 Setup Checklist
        MessageEmbed checklistEmbedTitle = getLangManager()
            .getEmbedBuilder(EMBED_CHECKLIST)
            .setColor(Constants.BLUE)
            .build();

        List<MessageEmbed> checklistEmbeds = new ArrayList<>();

        checklistEmbeds.add(checklistEmbedTitle);

        for (Debug.ErrorGroup group : Debug.ErrorGroup.values()) {
            EmbedBuilder groupEmbed = makeGroupEmbed(group, debugger);

            checklistEmbeds.add(groupEmbed.build());
        }

        // ⚠️ Warnings
        EmbedBuilder warningEmbed = null;
        if(hasWarning) {
            warningEmbed = new EmbedBuilder()
                .setTitle(getLang(MESSAGE_WARNINGS))
                .setColor(Constants.ORANGE);
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

    private @NotNull EmbedBuilder makeGroupEmbed(Debug.ErrorGroup group, @NotNull Debug debugger) {
        EmbedBuilder groupEmbed = new EmbedBuilder();

        boolean failed = debugger.hasGroup(group);
        LanguageFile.EmbedLang message = makeDebuggingMessage(group, failed);
        String configPath = DiscordPS.getPlugin().getDataFolder().getAbsolutePath();

        groupEmbed.setTitle(message.title());
        groupEmbed.setDescription(message.description().replace(Format.PATH, configPath));
        groupEmbed.setColor(failed? Constants.RED : Constants.GREEN);


        // Add each individual error in this group
        for (Map.Entry<Debug.Error, String> entry : debugger.allThrownErrors()) {

            // Find error in the group
            if (entry.getKey().getGroup() == group) {

                // Special message for tags configurations
                if(entry.getKey() == Debug.Error.WEBHOOK_TAG_VALIDATION_FAILED) {
                    LanguageFile.EmbedLang info = getEmbed(EMBED_TAGS_CONFIG_INFO);
                    groupEmbed.addField(info.title(), info.description(), false);
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

    private @NotNull LanguageFile.EmbedLang makeDebuggingMessage(@NotNull Debug.ErrorGroup group, boolean failed) {
        DebuggingMessage message = DebuggingMessage.valueOf(group.name());
        SystemLang lang = failed? message.getFailed() : message.getPassed();
        return getLangManager().getEmbed(lang);
    }
}
