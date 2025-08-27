package com.gqrshy.skjmod.emotes;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class EmotesMenuButton {
    public static void addEmotesButton(Screen screen, int scaledWidth, int scaledHeight) {
        if (!screen.getTitle().getString().equals("\uDAFF\uDFF8\uE033\uDAFF\uDF80\uF016")) return; // Emotes in silly wynncraft text
        
        final int slots = MinecraftClient.getInstance().player != null ? MinecraftClient.getInstance().player.currentScreenHandler.slots.size() : 0;
        final int rows = slots / 9;
        final int rowHeight = 18;
        final double rowHeightAdjustment = Math.floor(((rows - 3) * -0.5) * rowHeight);
        int topLeftY = (int) ((scaledHeight / 2) - rowHeightAdjustment - (rows - 1) * rowHeight - 12);
        
        Screens.getButtons(screen).add(ButtonWidget.builder(Text.of("Config"), button -> {
            MinecraftClient.getInstance().setScreen(new EmotesConfigGui(EmotesKeybind.keyBinding));
        }).dimensions((scaledWidth / 2) - 90 - 40, topLeftY, 40, 20).build());
    }
}