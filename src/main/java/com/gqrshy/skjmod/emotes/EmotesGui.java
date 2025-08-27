package com.gqrshy.skjmod.emotes;

import com.gqrshy.skjmod.config.ConfigManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EmotesGui extends Screen {

    private final KeyBinding keyBinding;
    private final ButtonWidget configButton;
    public final static int squareCount = 8;
    private final List<Pair<Integer, Integer>> squarePositions = new ArrayList<>();

    public static final int squareSize = 60; // size of each emote square
    public static final int radius = 100; // distance from center to square center
    private static final List<String> emotes = new ArrayList<>();

    protected EmotesGui(KeyBinding keyBinding) {
        super(Text.of("Emotes"));
        this.keyBinding = keyBinding;
        configButton = ButtonWidget.builder(Text.of("Configure"), button -> {
            MinecraftClient.getInstance().setScreen(new EmotesConfigGui(this.keyBinding));
        }).dimensions(0, 0, 100, 30).build();
        Screens.getButtons(this).add(configButton);
        updateFromConfig();
    }

    public static void updateFromConfig() {
        File emotesFile = new File(MinecraftClient.getInstance().runDirectory, "config/skjmod/emotes.json");
        JsonObject savedEmotes = readJsonFile(emotesFile);
        emotes.clear();

        if (!savedEmotes.has("favorites")) {
            emotes.addAll(Collections.nCopies(squareCount, ""));
            return;
        }

        JsonArray favorites = savedEmotes.get("favorites").getAsJsonArray();
        for (JsonElement favorite : favorites) {
            emotes.add(favorite.getAsString());
        }
        emotes.addAll(Collections.nCopies(squareCount - favorites.size(), ""));
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

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        final int middleX = width / 2;
        final int middleY = height / 2;

        // Create 8 positions around the center
        if (squarePositions.isEmpty()) {
            for (int i = 0; i < squareCount; i++) {
                double angle = Math.toRadians(i * ((double) 360 / squareCount)); // 8 directions, every 45 degrees
                int x = (int) (middleX + radius * Math.cos(angle));
                int y = (int) (middleY + radius * Math.sin(angle));
                squarePositions.add(new Pair<>(x, y));
            }
        }

        // Draw the emote squares
        for (int i = 0; i < squarePositions.size(); i++) {
            Pair<Integer, Integer> pos = squarePositions.get(i);
            int squareX = pos.getLeft() - squareSize / 2;
            int squareY = pos.getRight() - squareSize / 2;

            context.fill(squareX, squareY, squareX + squareSize, squareY + squareSize, (getActiveEmoji(mouseX, mouseY, middleX, middleY)) == i ? Color.GRAY.getRGB() : Color.BLACK.getRGB());

            // Draw centered emote text
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.of(emotes.get(i)),
                    pos.getLeft(),
                    pos.getRight() - 4,
                    Color.WHITE.getRGB()
            );
            // Draw emote number
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.of(Integer.toString(i + 1)),
                    pos.getLeft() - squareSize / 2 + 6,
                    pos.getRight() - squareSize / 2 + 4,
                    Color.WHITE.getRGB()
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!configButton.isMouseOver(mouseX, mouseY)) {
            int clickedEmoji = getActiveEmoji((int) mouseX, (int) mouseY, width / 2, height / 2);
            executeEmote(clickedEmoji);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_8) {
            executeEmote(keyCode - GLFW.GLFW_KEY_1);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyBinding.matchesKey(keyCode, scanCode)) {
            this.close();
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private static int getActiveEmoji(int mouseX, int mouseY, int centerX, int centerY) {
        double angle = Math.atan2(mouseY - centerY, mouseX - centerX); // Radians from -π to π

        // Normalize angle to [0, 2π)
        if (angle < 0) {
            angle += 2 * Math.PI;
        }

        // Divide circle into 8 equal sectors (each 45 degrees or π/4 radians)
        int index = (int) ((angle + Math.PI / squareCount) / (Math.PI / (squareCount / 2.))) % squareCount;

        return index; // Value from 0 to 7
    }

    private void executeEmote(int quadrant) {
        if (MinecraftClient.getInstance().getNetworkHandler() != null && emotes.size() > quadrant && !emotes.get(quadrant).isEmpty()) {
            this.close();
            String emoteName = emotes.get(quadrant);
            // Convert display names to correct command names
            String command = convertEmoteNameToCommand(emoteName);
            MinecraftClient.getInstance().getNetworkHandler().sendCommand("emote " + command);
        }
    }
    
    private String convertEmoteNameToCommand(String emoteName) {
        // Use configurable mappings instead of hardcoded switch
        return EmoteMappings.convertDisplayNameToCommand(emoteName);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}