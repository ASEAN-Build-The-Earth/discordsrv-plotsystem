package asia.buildtheearth.asean.discord.plotsystem.core.system.io;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.PluginProvider;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Language Configuration file.
 *
 * <p>Manage {@code system.yml} as {@link SystemLang} and {@code message.yml} as {@link MessageLang}</p>
 */
public class LangConfiguration extends PluginProvider {

    private final LanguageFile<SystemLang> systemLang = new LanguageFile<>();

    private static final String SYSTEM_LANG_FILE = "lang/system.yml";

    private final LanguageFile<MessageLang> messagesLang = new LanguageFile<>();

    private static final String MESSAGE_LANG_FILE = "lang/message.yml";

    /**
     * Register a language configuration for this plugin
     *
     * @param plugin The plugin instance to register to
     */
    public LangConfiguration(DiscordPS plugin) {
        super(plugin);
    }

    /**
     * Get the system lang manager
     *
     * @return {@link LangManager} managing {@link SystemLang}
     */
    public LangManager<SystemLang> getSystemLang() {
        return this.systemLang;
    }

    /**
     * Get the message lang manager
     *
     * @return {@link LangManager} managing {@link MessageLang}
     */
    public LangManager<MessageLang> getMessagesLang() {
        return this.messagesLang;
    }

    public void loadLanguageFiles(boolean create) throws IOException, InvalidConfigurationException {
        File systemLang = prepareLanguageFile(SYSTEM_LANG_FILE, create);
        File messagesLang = prepareLanguageFile(MESSAGE_LANG_FILE, create);

        this.tryLoadLang(this.systemLang, systemLang, SYSTEM_LANG_FILE);
        this.tryLoadLang(this.messagesLang, messagesLang, MESSAGE_LANG_FILE);
    }


    public File prepareLanguageFile(String location, boolean create) throws IOException {
        File file = new File(this.plugin.getDataFolder(), location);

        if (!file.exists()) {
            if(!create)
                throw new IOException("Language file at '" + location + "' does not exist to load.");

            if(!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            this.plugin.saveResource(location, false);
        }

        return file;
    }


    /**
     * Try to load language file into {@link FileConfiguration loader} instance,
     * falling back to the embedded resource data if failed.
     *
     * @param loader The file configuration to load in to
     * @param resource The resource file to be loaded
     * @param path The file path to load from
     * @throws IOException If the fallback method of loading from resource data returned null
     * @throws InvalidConfigurationException If the configuration resource is invalid
     */
    private void tryLoadLang(FileConfiguration loader, File resource, String path) throws IOException, InvalidConfigurationException {
        try {
            loader.load(resource);
        } catch (Exception ex) {
            InputStream resourceData = this.plugin.getResource(path);
            if(resourceData == null) throw new IOException(
                "Fallback method to load " + path + " from resource failed with null value"
            );
            DiscordPS.error("System Language File failed to load from data folder, falling back to embedded resource data.");
            loader.load(new InputStreamReader(resourceData));
        }
    }

}
