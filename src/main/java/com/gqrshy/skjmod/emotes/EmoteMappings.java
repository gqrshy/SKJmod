package com.gqrshy.skjmod.emotes;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.gqrshy.skjmod.SKJMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.*;
import java.util.*;

public class EmoteMappings {
    private static final Map<String, String> displayNameToCommand = new HashMap<>();
    private static final Set<String> defaultEmotes = new HashSet<>();
    private static final Map<String, String> commandToDisplayName = new HashMap<>();
    private static boolean loaded = false;
    
    public static void load() {
        if (loaded) return;
        
        try {
            // Try to load user config first
            File userConfigFile = new File(MinecraftClient.getInstance().runDirectory, "config/skjmod/emote-mappings.json");
            JsonObject config;
            
            if (userConfigFile.exists()) {
                SKJMod.LOGGER.info("Loading user emote mappings from config file");
                config = readJsonFile(userConfigFile);
            } else {
                SKJMod.LOGGER.info("Loading default emote mappings from resources");
                config = loadDefaultMappings();
                // Create user config file with defaults
                writeJsonFile(userConfigFile, config);
            }
            
            loadMappingsFromJson(config);
            loaded = true;
            SKJMod.LOGGER.info("Loaded emote mappings: {} display->command mappings, {} default emotes", 
                displayNameToCommand.size(), defaultEmotes.size());
            
        } catch (Exception e) {
            SKJMod.LOGGER.error("Failed to load emote mappings", e);
            // Load hardcoded fallbacks
            loadFallbackMappings();
        }
    }
    
    private static JsonObject loadDefaultMappings() throws IOException {
        Identifier mappingsId = Identifier.of("skjmod", "emote-mappings.json");
        Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(mappingsId);
        
        if (resource.isPresent()) {
            try (InputStream inputStream = resource.get().getInputStream();
                 InputStreamReader reader = new InputStreamReader(inputStream)) {
                return com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            }
        } else {
            throw new IOException("Default emote mappings not found in resources");
        }
    }
    
    private static void loadMappingsFromJson(JsonObject config) {
        displayNameToCommand.clear();
        defaultEmotes.clear();
        commandToDisplayName.clear();
        
        // Load display name to command mappings
        if (config.has("displayNameToCommand")) {
            JsonObject mappings = config.getAsJsonObject("displayNameToCommand");
            for (Map.Entry<String, JsonElement> entry : mappings.entrySet()) {
                displayNameToCommand.put(entry.getKey().toLowerCase(), entry.getValue().getAsString().toLowerCase());
            }
        }
        
        // Load default emotes
        if (config.has("defaultEmotes")) {
            JsonArray emotes = config.getAsJsonArray("defaultEmotes");
            for (JsonElement emote : emotes) {
                defaultEmotes.add(emote.getAsString().toLowerCase());
            }
        }
        
        // Load command to display name mappings
        if (config.has("displayNames")) {
            JsonObject displayNames = config.getAsJsonObject("displayNames");
            for (Map.Entry<String, JsonElement> entry : displayNames.entrySet()) {
                commandToDisplayName.put(entry.getKey().toLowerCase(), entry.getValue().getAsString());
            }
        }
    }
    
    private static void loadFallbackMappings() {
        SKJMod.LOGGER.warn("Using fallback emote mappings");
        displayNameToCommand.put("jumping_jacks", "jump");
        displayNameToCommand.put("jumping jacks", "jump");
        
        defaultEmotes.addAll(Arrays.asList("wave", "cheer", "clap", "dance", "hug", "jump", "relax", "flop"));
        
        commandToDisplayName.put("jump", "Jumping Jacks");
        commandToDisplayName.put("wave", "Wave");
        commandToDisplayName.put("cheer", "Cheer");
        commandToDisplayName.put("clap", "Clap");
        commandToDisplayName.put("dance", "Dance");
        commandToDisplayName.put("hug", "Hug");
        commandToDisplayName.put("relax", "Relax");
        commandToDisplayName.put("flop", "Flop");
        loaded = true;
    }
    
    public static String convertDisplayNameToCommand(String displayName) {
        if (!loaded) load();
        return displayNameToCommand.getOrDefault(displayName.toLowerCase(), displayName.toLowerCase());
    }
    
    public static Set<String> getDefaultEmotes() {
        if (!loaded) load();
        return new HashSet<>(defaultEmotes);
    }
    
    public static String getDisplayName(String command) {
        if (!loaded) load();
        return commandToDisplayName.getOrDefault(command.toLowerCase(), command);
    }
    
    private static JsonObject readJsonFile(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            return com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
    
    private static void writeJsonFile(File file, JsonObject jsonObject) throws IOException {
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            gson.toJson(jsonObject, writer);
        }
    }
}