package com.gqrshy.skjmod.emotes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnlockedEmotesDetector {

    private static final Set<String> detectedEmotes = new HashSet<>();
    private static Set<String> lastSavedEmotes = new HashSet<>();
    
    public static void initialize() {
        // Load default emotes from config
        detectedEmotes.addAll(EmoteMappings.getDefaultEmotes());
        
        // Save initial emotes
        saveSettings();
    }

    public static String extractEmoteName(String input) {
        Pattern pattern = Pattern.compile("^([A-Za-z0-9_]+) Emote$");
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            return matcher.group(1).toLowerCase();
        } else {
            return null;
        }
    }
    
    public static void afterRender(MinecraftClient client, Screen screen, Screen screen1) {
        if (client.player == null || screen == null) return;
        if (!(screen1 instanceof GenericContainerScreen) || !screen1.getTitle().getString().equals("\uDAFF\uDFF8\uE033\uDAFF\uDF80\uF016")) // Emotes in silly wynncraft text
            return;
        ScreenHandler screenHandler = client.player.currentScreenHandler;
        for (Slot slot : screenHandler.slots) {
            if (slot.getStack().isOf(Items.AIR) || slot.inventory.equals(client.player.getInventory())) {
                continue;
            }
            String emoteName = extractEmoteName(slot.getStack().getName().getString());
            if (emoteName != null) {
                detectedEmotes.add(emoteName);
            }
        }
        if (!lastSavedEmotes.equals(detectedEmotes)) {
            lastSavedEmotes = new HashSet<>(detectedEmotes);
            saveSettings();
        }
    }

    private static void saveSettings() {
        File emotesFile = new File(MinecraftClient.getInstance().runDirectory, "config/skjmod/emotes.json");
        JsonObject savedEmotes = readJsonFile(emotesFile);

        if (!savedEmotes.has("unlocked")) {
            savedEmotes.add("unlocked", new JsonArray());
        }

        JsonArray newUnlocked = new JsonArray();

        for (String emote : detectedEmotes) {
            newUnlocked.add(emote);
        }

        savedEmotes.add("unlocked", newUnlocked);

        writeJsonFile(emotesFile, savedEmotes);
    }

    private static JsonObject readJsonFile(File file) {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                return new JsonObject();
            }
            try (FileReader reader = new FileReader(file)) {
                return com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException e) {
            return new JsonObject();
        }
    }

    private static void writeJsonFile(File file, JsonObject jsonObject) {
        try {
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file)) {
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                gson.toJson(jsonObject, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}