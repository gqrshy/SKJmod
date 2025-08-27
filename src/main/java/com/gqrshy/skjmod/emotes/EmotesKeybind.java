package com.gqrshy.skjmod.emotes;

import com.gqrshy.skjmod.SKJMod;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class EmotesKeybind {
    private static boolean isHeld = false;
    public static KeyBinding keyBinding;
    
    public static void init() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "Open emotes menu", 
                InputUtil.Type.KEYSYM, 
                InputUtil.GLFW_KEY_V, 
                "SKJMod"
        ));
    }

    public static void onTick() {
        if (keyBinding.isPressed()) {
            if (!isHeld) {
                isHeld = true;
                MinecraftClient.getInstance().setScreen(new EmotesGui(keyBinding));
            }
        } else if (!(MinecraftClient.getInstance().currentScreen instanceof EmotesGui)) {
            isHeld = false;
        }
    }
}