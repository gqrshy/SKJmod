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
            // Extract only the bomb message line from potentially multi-line message
            String bombMessageLine = extractBombMessageLine(messageString);
            SKJMod.LOGGER.info("Detected Bombbell message: {}", bombMessageLine);
            
            Optional<BombInfo> bombInfoOpt = messageParser.parseBombMessage(bombMessageLine);
            
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
                SKJMod.LOGGER.warn("Failed to parse Bombbell message: {}", bombMessageLine);
            }
        } else if (configManager.getConfig().isEnableDebugLog()) {
            SKJMod.LOGGER.debug("Message is not a bomb bell message: {}", messageString);
        }
        
        return message;
    }
    
    private boolean isBombBellMessage(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        
        // Split by lines and check each line separately to avoid mixing different messages
        String[] lines = message.split("\\n|\\r\\n|\\r");
        
        for (String line : lines) {
            String cleanLine = line.trim();
            if (cleanLine.contains(" has thrown a") && cleanLine.contains(" Bomb on ")) {
                // Additional check to ensure it's not mixed with other messages
                if (isCleanBombMessage(cleanLine)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean isCleanBombMessage(String line) {
        // Make sure the line doesn't contain unrelated content
        String[] invalidContains = {
            "You don't have enough mana",
            "You are now entering",
            "You are now leaving",
            "Thank you for your business",
            "VolumeOff",
            "Thrown At",
            "Remaining Time",
            "Expires"
        };
        
        for (String invalid : invalidContains) {
            if (line.contains(invalid)) {
                return false;
            }
        }
        
        return true;
    }
    
    private String extractBombMessageLine(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        // Split by lines and find the clean bomb message line
        String[] lines = message.split("\\n|\\r\\n|\\r");
        
        for (String line : lines) {
            String cleanLine = line.trim();
            if (cleanLine.contains(" has thrown a") && cleanLine.contains(" Bomb on ")) {
                if (isCleanBombMessage(cleanLine)) {
                    return cleanLine;
                }
            }
        }
        
        // Fallback to the original message if no clean line found
        return message;
    }
    
    private String preprocessMessage(Text message) {
        String messageString = message.getString().trim();
        
        try {
            // Remove various Unicode characters that might be present before player names
            // This includes support rank indicators, special symbols, private use areas, etc.
            messageString = messageString
                    // Remove private use area characters (common in Minecraft formatting)
                    .replaceAll("[\\uE000-\\uF8FF]", "")
                    // Remove other special Unicode categories
                    .replaceAll("[\\p{So}\\p{Sk}\\p{Mn}\\p{Cf}\\p{Cn}]", "")
                    // Remove control characters
                    .replaceAll("\\p{Cntrl}", "")
                    // Remove any remaining non-printable characters except basic spaces
                    .replaceAll("[^\\p{Print}\\p{Space}]", "")
                    .trim();
            
            // Clean up any multiple spaces that might be left
            messageString = messageString.replaceAll("\\s+", " ");
            
        } catch (Exception e) {
            SKJMod.LOGGER.warn("Failed to preprocess message with regex, using original: {}", e.getMessage());
            // Fall back to basic cleanup if regex fails
            messageString = messageString.replaceAll("\\s+", " ").trim();
        }
        
        if (configManager.getConfig().isEnableDebugLog()) {
            SKJMod.LOGGER.debug("Preprocessed message: '{}' -> '{}'", message.getString(), messageString);
        }
        
        return messageString;
    }
}