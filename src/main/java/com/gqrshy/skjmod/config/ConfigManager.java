package com.gqrshy.skjmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.gqrshy.skjmod.SKJMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class ConfigManager {
    private static final String CONFIG_FILE = "skjmod.json";
    private final Gson gson;
    private final Path configPath;
    private ModConfig config;
    
    public ConfigManager() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        this.config = new ModConfig();
    }
    
    public void loadConfig() {
        try {
            if (Files.exists(configPath)) {
                String json = Files.readString(configPath);
                this.config = gson.fromJson(json, ModConfig.class);
                if (this.config == null) {
                    this.config = new ModConfig();
                }
                SKJMod.LOGGER.info("Configuration loaded from: " + configPath);
            } else {
                SKJMod.LOGGER.info("Configuration file not found, creating default configuration");
                saveConfig();
            }
        } catch (IOException e) {
            SKJMod.LOGGER.error("Failed to load configuration, using defaults", e);
            this.config = new ModConfig();
        } catch (Exception e) {
            SKJMod.LOGGER.error("Invalid configuration format, using defaults", e);
            this.config = new ModConfig();
        }
    }
    
    public void saveConfig() {
        try {
            String json = gson.toJson(config);
            Files.writeString(configPath, json, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            SKJMod.LOGGER.info("Configuration saved to: " + configPath);
        } catch (IOException e) {
            SKJMod.LOGGER.error("Failed to save configuration", e);
        }
    }
    
    public ModConfig getConfig() {
        return config;
    }
    
    public void setConfig(ModConfig config) {
        this.config = config;
    }
}