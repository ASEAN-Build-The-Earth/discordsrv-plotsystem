package github.tintinkung.discordps.core.system;

import github.scarsz.discordsrv.dependencies.commons.lang3.StringUtils;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.interactions.components.ActionRow;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataArray;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.DataObject;
import github.scarsz.discordsrv.dependencies.jda.api.utils.data.SerializableData;
import github.scarsz.discordsrv.dependencies.jda.internal.utils.AllowedMentionsImpl;

import github.scarsz.discordsrv.dependencies.jda.internal.utils.BufferedRequestBody;
import github.scarsz.discordsrv.dependencies.okhttp3.MediaType;
import github.scarsz.discordsrv.dependencies.okhttp3.MultipartBody;
import github.scarsz.discordsrv.dependencies.okhttp3.RequestBody;
import github.scarsz.discordsrv.dependencies.okio.Okio;
import github.tintinkung.discordps.DiscordPS;
import github.tintinkung.discordps.core.system.components.api.ComponentV2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * A builder for creating {@link WebhookData} objects, which represent payloads for sending messages via Discord webhooks.
 * <p>
 * Supports thread name, username, avatar URL, message content, embeds, and interaction components.
 * Also extends {@link AllowedMentionsImpl}, for allowed mentions settings.
 * </p>
 *
 * <p>usage:</p>
 * <blockquote>{@snippet :
 * WebhookData data = new WebhookDataBuilder()
 *     .setContent("Hello, Discord!")
 *     .suppressNotifications(true) // Suppresses notifications
 *     .build();
 * }</blockquote>
 *
 * <p><b>Note:</b> File attachments <i>cannot</i> be set through this builder. To include files,
 * use {@link WebhookData#addFile(File)} on the built {@link WebhookData} instance before preparing the request body.</p>
 */
public class WebhookDataBuilder extends AllowedMentionsImpl {

    private @Nullable String threadName = null;
    private @Nullable String username = null;
    private @Nullable String webhookAvatarUrl = null;
    private @Nullable String content = null;
    private @Nullable Collection<? extends MessageEmbed> embeds = null;
    private @Nullable Collection<? extends ActionRow> components = null;
    private @Nullable Collection<? extends DataObject> componentsV2 = null;
    private @Nullable Integer flags = null;
    private boolean suppressMentions = false;
    private boolean forceComponentV2 = false;

    /**
     * Constructs a new {@link WebhookDataBuilder} instance with default (null/empty) values.
     * <p>
     * Use the setter methods to configure the desired webhook message options, then call {@link #build()}
     * to produce an immutable {@link WebhookData} object ready for dispatch via the Discord API.
     * </p>
     */
    public WebhookDataBuilder() {
        super();
    }

    /**
     * Sets the avatar URL or data URI for the webhook.
     * If using a data URI, it must be valid image data (e.g., {@code data:image/png;base64,...}).
     * Supported image types are PNG, JPEG, and GIF. It's recommended to keep the data URI under 1 MB.
     *
     * @param webhookAvatarUrl A URL or data URI to override the default avatar of the webhook.
     * @return This builder instance for chaining.
     */
    public WebhookDataBuilder setWebhookAvatarUrl(@Nullable String webhookAvatarUrl) {
        this.webhookAvatarUrl = webhookAvatarUrl;
        return this;
    }

    /**
     * Set the thread name if this webhook data is for executing a new thread on a channel.
     * @param threadName The thread name
     * @return This builder instance for chaining.
     */
    public WebhookDataBuilder setThreadName(@Nullable String threadName) {
        this.threadName = threadName;
        return this;
    }

    /**
     * Override default username of the webhook
     * @param username A username that follows discord API guidelines
     * @return This builder instance for chaining.
     */
    public WebhookDataBuilder setUsername(@Nullable String username) {
        this.username = username;
        return this;
    }

    /**
     * Sets the message content of the webhook.
     * The content is a string of up to 2000 characters.
     *
     * @param content The message contents of the webhook.
     * @return This builder instance for chaining.
     */
    public WebhookDataBuilder setContent(@Nullable String content) {
        this.content = content;
        return this;
    }

    /**
     * Sets the embedded rich content for the webhook message.
     * You can provide up to 10 embeds.
     *
     * @param embeds A collection of embedded rich content (up to 10 embed objects).
     * @return This builder instance for chaining.
     */
    public WebhookDataBuilder setEmbeds(@Nullable Collection<? extends MessageEmbed> embeds) {
        this.embeds = embeds;
        return this;
    }

    /**
     * Sets the interactive message components to include with the message.
     * These could include buttons or other types of UI components.
     *
     * @param components A collection of message components (e.g., buttons).
     * @return This builder instance for chaining.
     */
    public WebhookDataBuilder setComponents(@Nullable Collection<? extends ActionRow> components) {
        this.components = components;
        return this;
    }

    public WebhookDataBuilder setComponentsV2(@Nullable Collection<? extends ComponentV2> componentsV2) {
        this.componentsV2 = componentsV2;
        return this;
    }

    /**
     * Sets the flags for the message to be {@code SUPPRESS_NOTIFICATIONS},
     * This flag disables push and desktop notifications for the message.
     *
     * @return This builder instance for chaining.
     */
    public WebhookDataBuilder suppressNotifications() {
        if(this.flags != null)
            this.flags = flags | (1 << 12);
        else this.flags = (1 << 12);
        return this;
    }

    public WebhookDataBuilder forceComponentV2() {
        this.forceComponentV2 = true;
        if(this.flags != null)
            this.flags = flags | (1 << 15);
        else this.flags = (1 << 15);
        return this;
    }

    /**
     * Suppress all possible mentions in the data.
     * This will send a {@code allowed_mentions} payload of empty {@code parse} data.
     * @return This builder instance for chaining.
     */
    public WebhookDataBuilder suppressMentions() {
        this.suppressMentions = true;
        return this;
    }

    /**
     * Builds the {@link WebhookData} object with the current builder state.
     *
     * @return The constructed WebhookData object.
     */
    public WebhookData build() {
        // Parse JSON Parameters
        DataObject webhookData = DataObject.empty();

        // Set Thread Name
        if(threadName != null) {
            webhookData.put("thread_name", threadName);
        }

        // Override webhook username
        if(username != null) {
            webhookData.put("username", username);
        }

        // Override avatar image
        if(webhookAvatarUrl != null) {
            webhookData.put("avatar_url", webhookAvatarUrl);
        }

        // Content message
        if (StringUtils.isNotBlank(content)) {
            webhookData.put("content", content);
        }

        // Embeds
        if (embeds != null && !forceComponentV2) {
            DataArray embedArray = DataArray.empty();
            for (MessageEmbed embed : embeds) {
                if (embed != null) {
                    embedArray.add(embed.toData());
                }
            }

            webhookData.put("embeds", embedArray);
        }

        // Components
        if (components != null || componentsV2 != null) {
            DataArray componentsArray = DataArray.empty();

            if(forceComponentV2 && componentsV2 != null) {
                for (DataObject component : componentsV2)
                    componentsArray.add(component.toData());
            }
            else if(components != null) {
                for (ActionRow actionRow : components)
                    componentsArray.add(actionRow.toData());
            }

            webhookData.put("components", componentsArray);
        }

        // Flags
        if(flags != null) {
            webhookData.put("flags", flags);
        }

        // Allow Mentions
        if(!suppressMentions) {
            AllowedMentionsImpl allowedMentionsImpl = this;
            DataObject allowedMentions = allowedMentionsImpl.toData();
            webhookData.put("allowed_mentions", allowedMentions);
        }
        else {
            // Suppress all mentions
            // "allowed_mentions": {
            //     "parse": []
            // }
            DataObject suppressMention = DataObject.empty();
            suppressMention.put("parse", DataArray.empty());
            webhookData.put("allowed_mentions", suppressMention);
        }

        return new WebhookData(webhookData.toMap());
    }


    /**
     * Represents the data used to send a message via a Discord webhook.
     *
     * <p>Usage:</p><blockquote>{@snippet :
     *     // Creating Data
     *     WebhookData data = new WebhookDataBuilder().build();
     *     data.addFile(new File("path/to/image.png"));
     *     // Requesting Data
     *     MultipartBody body = data.prepareRequestBody();
     * }</blockquote>
     * <p>Requesting API:</p>
     * <pre>{@code
     *     RestAction restAction = new RestActionImpl<>(jda, route, body, (res, req) -> {});
     * }</pre>
     */
    public static class WebhookData extends DataObject implements SerializableData {

        private @Nullable List<File> attachments = null;

        private WebhookData(@NotNull Map<String, Object> data) {
            super(data);
        }

        /**
         * Add file attachments for the webhook message.
         *
         * @param file The file to add
         */
        public void addFile(File file) {
            if(file.isDirectory()) throw new IllegalArgumentException("Cannot add folder to WebhookData.");

            if(attachments == null) attachments = new ArrayList<>(List.of(file));
            else attachments.add(file);
        }

        /**
         * Prepares the complete request body for sending this webhook message to the Discord API.
         * <p>
         * It constructs a {@code multipart/form-data} body containing the
         * {@code payload_json} part and any file attachments provided via {@link #addFile(File)}.
         * </p>
         *
         * @return A {@link MultipartBody} representing the full webhook payload, ready to be sent.
         */
        public @NotNull MultipartBody prepareRequestBody() {

            MultipartBody.Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

            tryAttachFiles(bodyBuilder);

            bodyBuilder.addFormDataPart(
                    "payload_json",
                    null,
                    RequestBody.create(
                        MediaType.get("application/json"),
                        super.toString()
                    )
            );

            return bodyBuilder.build();
        }

        private void tryAttachFiles(MultipartBody.Builder requestBody) {
            List<String> attachmentIndex = putAllAttachments();

            if (attachmentIndex == null || attachments == null)  return;

            for (int i = 0; i < attachmentIndex.size(); i++) {
                String name = attachmentIndex.get(i);
                File file = attachments.get(i);
                InputStream data = null;

                try { data = new FileInputStream(attachments.get(i)); }
                catch (FileNotFoundException ex) {
                    DiscordPS.error("Trying to add attach file that does not exist: " + ex.getMessage());
                }

                if (data == null) continue;

                try {
                    String mimeType = Files.probeContentType(file.toPath());

                    requestBody.addFormDataPart(
                            "files[" + i + "]",
                            name,
                            new BufferedRequestBody(Okio.source(data), MediaType.parse(mimeType))
                            // RequestBody.create(MediaType.parse("image/png"), data);
                            // IOUtil.createRequestBody(Requester.MEDIA_TYPE_OCTET, data)
                    );
                    DiscordPS.debug("Attaches file " + i + " to form data part: " + requestBody);

                    // data.close();
                }
                catch (RuntimeException | IOException ex) {
                    DiscordPS.error("Failed to attach file to webhook data during request body creation.", ex);
                    try { data.close(); }
                    catch (IOException ignore) {}
                }
            }


        }

        /**
         * Put all (if exist) file attachment to this {@link DataObject}
         * @return List of all attachments (if any) file name.
         */
        private @Nullable List<String> putAllAttachments() {
            if (attachments != null) {
                List<String> attachmentIndex = new ArrayList<>(attachments.size());
                DataArray attachmentArray = DataArray.empty();

                int i = 0;
                for (File file : attachments) {
                    attachmentIndex.add(file.getName());
                    DataObject attachmentObject = DataObject.empty();
                    attachmentObject.put("id", i);
                    attachmentObject.put("filename", file.getName());
                    attachmentArray.add(attachmentObject);
                    i++;
                }

                DiscordPS.debug("Putting attachment: " + attachmentArray.toPrettyString());

                put("attachments", attachmentArray);
                return attachmentIndex;
            }
            return null;
        }
    }
}
