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
    
    public Text onChatMessage(Text message) {
        if (!configManager.getConfig().isEnableNotification()) {
            return message;
        }
        
        String messageString = preprocessMessage(message);
        
        if (configManager.getConfig().isEnableDebugLog()) {
            SKJMod.LOGGER.debug("Received chat message: {}", messageString);
        }
        
        if (isBombBellMessage(messageString)) {
            SKJMod.LOGGER.info("Detected Bombbell message: {}", messageString);
            
            Optional<BombInfo> bombInfoOpt = messageParser.parseBombMessage(messageString);
            
            if (bombInfoOpt.isPresent()) {
                BombInfo bombInfo = bombInfoOpt.get();
                SKJMod.LOGGER.info("Parsed bomb info: Player={}, Type={}, World={}", 
                    bombInfo.getPlayerName(), bombInfo.getBombType(), bombInfo.getWorldName());
                
                if (configManager.getConfig().isBombTypeEnabled(bombInfo.getBombType().name())) {
                    SKJMod.LOGGER.info("Sending Discord notification for bomb: {}", bombInfo);
                    discordNotifier.sendBombNotification(bombInfo);
                } else {
                    SKJMod.LOGGER.info("Bomb type {} is disabled in configuration", 
                            bombInfo.getBombType().name());
                }
            } else {
                SKJMod.LOGGER.warn("Failed to parse Bombbell message: {}", messageString);
            }
        } else if (configManager.getConfig().isEnableDebugLog()) {
            SKJMod.LOGGER.debug("Message is not a bomb bell message: {}", messageString);
        }
        
        return message;
    }
    
    private boolean isBombBellMessage(String message) {
        return message.contains(" has thrown a") && message.contains(" Bomb on ");
    }
    
    private String preprocessMessage(Text message) {
        return message.getString().trim();
    }
}