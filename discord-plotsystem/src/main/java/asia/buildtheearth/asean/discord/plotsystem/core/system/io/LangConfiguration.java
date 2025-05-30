package asia.buildtheearth.asean.discord.plotsystem.core.system.io;

import asia.buildtheearth.asean.discord.plotsystem.DiscordPS;
import asia.buildtheearth.asean.discord.plotsystem.core.providers.PluginProvider;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LangConfiguration extends PluginProvider {

    private LanguageFile<SystemLang> systemLang;

    private LanguageFile<MessageLang> messagesLang;

    public LangConfiguration(DiscordPS plugin) {
        super(plugin);
    }

    public LangManager<SystemLang> getSystemLang() {
        return this.systemLang;
    }

    public LangManager<MessageLang> getMessagesLang() {
        return this.messagesLang;
    }

    public void initLanguageFiles() throws IOException, InvalidConfigurationException {
        File systemLang = new File(this.plugin.getDataFolder(), "lang/system.yml");
        if (!systemLang.exists()) {
            this.plugin.saveResource("system.yml", false);
        }

        File messagesLang = new File(this.plugin.getDataFolder(), "lang/message.yml");
        if (!messagesLang.exists()) {
            this.plugin.saveResource("message.yml", false);
        }

        // Load config from resource to the plugin
        this.systemLang = new LanguageFile<>();
        this.messagesLang = new LanguageFile<>();

        this.tryLoadLang(this.systemLang, systemLang, "lang/system.yml");
        this.tryLoadLang(this.messagesLang, messagesLang, "lang/message.yml");
    }


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
