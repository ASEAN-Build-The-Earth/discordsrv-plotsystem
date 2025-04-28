package github.tintinkung.discordps.core.listeners;

import github.scarsz.discordsrv.dependencies.jda.api.EmbedBuilder;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;

import github.scarsz.discordsrv.dependencies.jda.api.events.interaction.ButtonClickEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.DisconnectEvent;
import github.scarsz.discordsrv.dependencies.jda.api.events.ShutdownEvent;
import github.scarsz.discordsrv.dependencies.jda.api.hooks.ListenerAdapter;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.Button;
import github.scarsz.discordsrv.dependencies.jda.api.requests.CloseCode;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.Checks;
import github.tintinkung.discordps.Constants;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.commands.events.CommandEvent;
import github.tintinkung.discordps.commands.interactions.InteractionEvent;
import github.tintinkung.discordps.commands.interactions.OnSetupWebhook;
import github.tintinkung.discordps.commands.SetupCommand;
import github.tintinkung.discordps.core.database.WebhookEntry;
import github.tintinkung.discordps.core.system.AvailableTags;
import github.tintinkung.discordps.core.system.Notification;
import github.tintinkung.discordps.utils.ComponentUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.sql.SQLException;
import java.util.EnumMap;

/**
 * JDA instance listener
 * DiscordSRV on disconnect does not call any API event, so we have to handle it our own too.
 * @see <a href="https://github.com/DiscordSRV/DiscordSRV/blob/9d4734818ab27069d76f264a4cda74a699806770/src/main/java/github/scarsz/discordsrv/listeners/DiscordDisconnectListener.java#L33">
 *     github.scarsz.discordsrv.listeners.DiscordDisconnectListener
 * </a>
 */
final public class DiscordEventListener extends ListenerAdapter {

    public static CloseCode mostRecentCloseCode = null;
    private final DiscordSRVListener listener;

    public DiscordEventListener(DiscordSRVListener listener) {
        this.listener = listener;
    }

    @Override
    public void onDisconnect(@NotNull DisconnectEvent event) {
        handleCode(event.getCloseCode());
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        handleCode(event.getCloseCode());
    }

    private void handleCode(CloseCode closeCode) {
        if (closeCode == null) {
            return;
        }
        mostRecentCloseCode = closeCode;
        if (closeCode == CloseCode.DISALLOWED_INTENTS || !closeCode.isReconnect()) {
            DiscordPS.error("Please check if DiscordSRV plugin is loaded correctly");
            DiscordPS.getPlugin().disablePlugin("Discord Plot System cannot connect to DiscordSRV");
        }
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {

        if(listener.getPluginSlashCommand() == null) return;
        if(event.getButton() == null) return;
        if(event.getButton().getId() == null) return;

        Button button = event.getButton();
        EnumMap<ComponentUtil.IDPattern, String> component = ComponentUtil.parseCustomID(button.getId());

        // Invalid button ID (possibly from other bots)
        if(component == null) return;

        // If for whatever reason the button is not from this plugin
        if(!component.get(ComponentUtil.IDPattern.PLUGIN).equals(DiscordPS.getPlugin().getName())) return;

        // Extract our button ID
        String type = component.get(ComponentUtil.IDPattern.TYPE);
        String id   = component.get(ComponentUtil.IDPattern.ID);
        String user = component.get(ComponentUtil.IDPattern.USER);

        Checks.isSnowflake(id, "Internal Error: Button ID");
        Checks.isSnowflake(user, "Internal Error: Button's USER ID");

        long eventID = Long.parseUnsignedLong(id);

        // Setup command and interactions references
        InteractionEvent interactions = listener.getPluginSlashCommand().getInteractions();
        CommandEvent commands = listener.getPluginSlashCommand().getCommands();

        // TODO: maybe handle each case as a handler method.
        switch (type) {
            case Constants.CONFIRM_AVATAR_BUTTON -> {
                if(!user.equals(event.getUser().getId())) break;

                TextChannel channel = event.getInteraction().getTextChannel();
                MessageReference lastMsg = getInteractionLastMessage(channel);

                MessageEmbed errorEmbed = new EmbedBuilder()
                    .setTitle("Please send a message with attachment")
                    .setDescription("And make sure it is the latest message before confirming.")
                    .setColor(Color.RED)
                    .build();

                // Last message is not sent (it is the button message)
                if(lastMsg == null || lastMsg.getMessageIdLong() == event.getMessage().getIdLong()) {
                    channel.sendMessageEmbeds(errorEmbed).queue();
                    event.editButton(button.asEnabled()).queue();
                    break;
                }

                lastMsg.resolve().queue((message -> {

                    if(message.getAttachments().isEmpty()) {
                        channel.sendMessageEmbeds(errorEmbed).queue();
                        event.editButton(button.asEnabled()).queue();
                        return;
                    }

                    event.editComponents(ActionRow.of(button.asDisabled())).queue();
                    commands.fromClass(SetupCommand.class)
                            .getWebhookCommand()
                            .onConfirmAvatar(message, interactions.getAs(OnSetupWebhook.class, eventID));
                }));
            }
            case Constants.PROVIDED_IMAGE_BUTTON -> {
                if(!user.equals(event.getUser().getId())) break;
                event.editComponents(ActionRow.of(button.asDisabled())).queue();

                commands.fromClass(SetupCommand.class)
                        .getWebhookCommand()
                        .onConfirmAvatarProvided(
                    event.getChannel(),
                    interactions.getAs(OnSetupWebhook.class, eventID)
                );
            }
            case Constants.CONFIRM_CONFIG_BUTTON -> {
                if(!user.equals(event.getUser().getId())) break;
                event.editComponents(ActionRow.of(button.asDisabled())).queue();

                commands.fromClass(SetupCommand.class)
                        .getWebhookCommand()
                        .onConfirmConfig(event.getMessage(), interactions.getAs(OnSetupWebhook.class, eventID));
                DiscordPS.getPlugin().exitSlashCommand(eventID);
            }
            case Constants.CANCEL_CONFIG_BUTTON -> {
                if(!user.equals(event.getUser().getId())) break;
                // Make button disabled and turn gray
                event.editComponents(ActionRow.of(
                    Button.secondary(
                        button.getId() + "/" + "Clicked",
                        "Cancelled")
                        .asDisabled()
                    )).queue();
                DiscordPS.getPlugin().exitSlashCommand(eventID);
            }
            case Constants.HELP_BUTTON -> {
                if(!user.equals(event.getUser().getId())) {

                    MessageEmbed notOwnerEmbed = new EmbedBuilder()
                            .setTitle("You don't own this plot")
                            .setDescription("Go build a plot!")
                            .setColor(Color.RED)
                            .build();
                    event.deferReply(true).addEmbeds(notOwnerEmbed).queue();
                    break;
                }

                MessageEmbed helpEmbed = new EmbedBuilder()
                        .setTitle("Help")
                        .setDescription("Please message our support bot <@1361310076308033616> " +
                                "for questions if you need help about this plot. (or just ping a staff here)")
                        .addField("Building",
                                "Click the documentation link above to take a look " +
                                "at a detailed guide on how to build your plot!", false)
                        .setColor(AvailableTags.APPROVED.getColor())
                        .build();
                event.deferReply(true).addEmbeds(helpEmbed).queue();
            }
            case Constants.FEEDBACK_BUTTON -> {
                if(!user.equals(event.getUser().getId())) {

                    MessageEmbed notOwnerEmbed = new EmbedBuilder()
                            .setTitle("You don't own this plot")
                            .setDescription("Go build a plot!")
                            .setColor(Color.RED)
                            .build();
                    event.deferReply(true).addEmbeds(notOwnerEmbed).queue();
                    break;
                }

                try {
                    WebhookEntry entry = WebhookEntry.getByMessageID(Long.parseUnsignedLong(id));

                    if(entry == null || entry.feedback() == null)
                        throw new SQLException("Trying to get feedback that does not exist in the database!");

                    MessageEmbed feedbackEmbed = new EmbedBuilder()
                            .setTitle("Your Feedback")
                            .setDescription(entry.feedback())
                            .addField("Help",
                                "Please message our support bot <@1361310076308033616> " +
                                "for questions if you need help about this plot. (or just ping a staff here)",
                                false)
                            .setColor(entry.status().toTag().getColor())
                            .build();

                    event.deferReply(true).addEmbeds(feedbackEmbed).queue();


                } catch (SQLException ex) {
                    MessageEmbed errorEmbed = new EmbedBuilder()
                            .setTitle("Error :(")
                            .setDescription("Sorry an error occurred trying to get your feedback data. " +
                                    "Please message our support bot <@1361310076308033616> to ask for it.")
                            .setColor(Color.RED)
                            .build();
                    event.deferReply(true).addEmbeds(errorEmbed).queue();

                    String plot = component.get(ComponentUtil.IDPattern.PAYLOAD);

                    Notification.sendMessageEmbeds(new EmbedBuilder()
                            .setTitle(":red_circle: Discord Plot-System Error")
                            .setDescription("Runtime exception **fetching** plot feedback, "
                                    + "The owner of plot ID #`" + plot + "` "
                                    + "cannot view their feedback message!")
                            .addField("Error", "```" + ex.toString() + "```", false)
                            .setColor(Color.RED)
                            .build()
                    );
                }
            }
            default -> {
                throw new RuntimeException("[Internal Error] Button click even is not handled for " + type);
            }
        }
    }

    private static @Nullable MessageReference getInteractionLastMessage(TextChannel channel) {
        try {

            long lastMsgID = channel.getLatestMessageIdLong();
            long channelID = channel.getIdLong();
            long guildID = channel.getGuild().getIdLong();

            return new MessageReference(
                    lastMsgID,
                    channelID,
                    guildID,
                    null,
                    channel.getJDA()
            );
        } catch (IllegalStateException ex) {
            DiscordPS.error("Interaction only support in a text channel");
            return null;
        }
    }
}
