package com.gqrshy.skjmod;

import com.gqrshy.skjmod.config.ConfigManager;
import com.gqrshy.skjmod.emotes.EmotesKeybind;
import com.gqrshy.skjmod.emotes.UnlockedEmotesDetector;
import com.gqrshy.skjmod.event.ChatMessageCallback;
import com.gqrshy.skjmod.listener.ChatMessageListener;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SKJMod implements ClientModInitializer {
    public static final String MOD_ID = "skjmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    private static SKJMod instance;
    private ConfigManager configManager;
    private ChatMessageListener chatMessageListener;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("SKJ Mod initializing...");
        
        instance = this;
        
        // Initialize config manager
        this.configManager = new ConfigManager();
        this.configManager.loadConfig();
        
        // Initialize chat message listener
        this.chatMessageListener = new ChatMessageListener(configManager);
        
        // Register chat message event listener with custom callback
        ChatMessageCallback.EVENT.register(chatMessageListener::onChatMessage);
        
        // Initialize emote mappings first
        com.gqrshy.skjmod.emotes.EmoteMappings.load();
        
        // Initialize emote keybinds
        EmotesKeybind.init();
        
        // Initialize emote detector with default emotes
        UnlockedEmotesDetector.initialize();
        
        // Register client tick event for emote keybind handling
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            EmotesKeybind.onTick();
        });
        
        // Register screen events for emote detection
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            UnlockedEmotesDetector.afterRender(client, screen, screen);
        });
        
        
        LOGGER.info("SKJ Mod initialized successfully!");
    }
    
    public static SKJMod getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
}