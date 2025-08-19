package com.gqrshy.skjmod.listener;

import com.gqrshy.skjmod.SKJMod;
import com.gqrshy.skjmod.config.ConfigManager;
import com.gqrshy.skjmod.discord.DiscordNotifier;
import com.gqrshy.skjmod.parser.BombInfo;
import com.gqrshy.skjmod.parser.BombMessageParser;
import net.minecraft.text.Text;

import java.util.Optional;

public class ChatMessageListener {
    private final ConfigManager configManager;
    private final BombMessageParser messageParser;
    private final DiscordNotifier discordNotifier;
    
    public ChatMessageListener(ConfigManager configManager) {
        this.configManager = configManager;
        this.messageParser = new BombMessageParser();
        this.discordNotifier = new DiscordNotifier(configManager);
    }
    
    public void onChatMessage(Text message, boolean overlay) {
        if (overlay) {
            return;
        }
        
        if (!configManager.getConfig().isEnableNotification()) {
            return;
        }
        
        String messageString = preprocessMessage(message);
        
        if (configManager.getConfig().isEnableDebugLog()) {
            SKJMod.LOGGER.debug("Received chat message: {}", messageString);
        }
        
        if (isBombBellMessage(messageString)) {
            if (configManager.getConfig().isEnableDebugLog()) {
                SKJMod.LOGGER.debug("Detected Bombbell message: {}", messageString);
            }
            
            Optional<BombInfo> bombInfoOpt = messageParser.parseBombMessage(messageString);
            
            if (bombInfoOpt.isPresent()) {
                BombInfo bombInfo = bombInfoOpt.get();
                
                if (configManager.getConfig().isBombTypeEnabled(bombInfo.getBombType().name())) {
                    SKJMod.LOGGER.info("Processed Bomb: {}", bombInfo);
                    discordNotifier.sendBombNotification(bombInfo);
                } else {
                    if (configManager.getConfig().isEnableDebugLog()) {
                        SKJMod.LOGGER.debug("Bomb type {} is disabled in configuration", 
                                bombInfo.getBombType().name());
                    }
                }
            } else {
                SKJMod.LOGGER.warn("Failed to parse Bombbell message: {}", messageString);
            }
        }
    }
    
    private boolean isBombBellMessage(String message) {
        return message.contains(" has thrown a") && 
               message.contains(" Bomb on ") &&
               (message.contains("Combat Experience") ||
                message.contains("Profession Experience") ||
                message.contains("Profession Speed") ||
                message.contains("Dungeon") ||
                message.contains("Loot Chest") ||
                message.contains("Loot"));
    }
    
    private String preprocessMessage(Text message) {
        return message.getString().trim();
    }
}