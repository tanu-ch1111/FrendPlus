package com.tanuch;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class FrendPlus extends JavaPlugin {

    private File messageFile;
    private FileConfiguration messageConfig;

    @Override
    public void onEnable() {

        createMessageFile(); // message.yml を生成
        getCommand("frend").setExecutor(new FrendCommand(this));
    }

    @Override
    public void onDisable() {
        saveMessageConfig();
    }

    private void createMessageFile() {
        messageFile = new File(getDataFolder(), "message.yml");
        if (!messageFile.exists()) {
            try {
                saveResource("message.yml", false); // プラグイン内の既定ファイルをコピー
            } catch (IllegalArgumentException e) {
                try {
                    if (messageFile.createNewFile()) {
                        getLogger().info("Created message.yml.");
                    }
                } catch (IOException ex) {
                    getLogger().severe("Could not create message.yml: " + ex.getMessage());
                }
            }
        }
        messageConfig = YamlConfiguration.loadConfiguration(messageFile);
    }

    public FileConfiguration getMessageConfig() {
        if (messageConfig == null) {
            createMessageFile();
        }
        return messageConfig;
    }

    public void reloadMessageConfig() {
        File messageFile = new File(getDataFolder(), "message.yml");
        if (!messageFile.exists()) {
            saveResource("message.yml", false);
        }
        messageConfig = YamlConfiguration.loadConfiguration(messageFile);
    }

    public void saveMessageConfig() {
        if (messageConfig != null && messageFile != null) {
            try {
                messageConfig.save(messageFile);
            } catch (IOException e) {
                getLogger().severe("Could not save message.yml: " + e.getMessage());
            }
        }
    }
}