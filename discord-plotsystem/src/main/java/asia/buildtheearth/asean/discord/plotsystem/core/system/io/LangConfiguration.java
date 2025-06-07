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

    private LanguageFile<SystemLang> systemLang;

    private LanguageFile<MessageLang> messagesLang;

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

    /**
     * Initialize all language file from resource and load the data into {@link LanguageFile} instance.
     *
     * @throws IOException If the resource failed to load
     * @throws InvalidConfigurationException If the yaml file is invalid
     */
    public void initLanguageFiles() throws IOException, InvalidConfigurationException {
        File systemLang = new File(this.plugin.getDataFolder(), "lang/system.yml");
        if (!systemLang.exists()) {
            if(!systemLang.getParentFile().exists())
                systemLang.getParentFile().mkdirs();
            this.plugin.saveResource("lang/system.yml", false);
        }

        File messagesLang = new File(this.plugin.getDataFolder(), "lang/message.yml");
        if (!messagesLang.exists()) {
            if(!messagesLang.getParentFile().exists())
                messagesLang.getParentFile().mkdirs();
            this.plugin.saveResource("lang/message.yml", false);
        }

        // Load config from resource to the plugin
        this.systemLang = new LanguageFile<>();
        this.messagesLang = new LanguageFile<>();

        this.tryLoadLang(this.systemLang, systemLang, "lang/system.yml");
        this.tryLoadLang(this.messagesLang, messagesLang, "lang/message.yml");
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
