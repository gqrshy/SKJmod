package com.gqrshy.skjmod.parser;

import com.gqrshy.skjmod.SKJMod;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BombMessageParser {
    // More specific regex to avoid capturing unwanted text
    private static final String BOMB_REGEX = 
        "^(?<player>[A-Za-z0-9_]{1,16}) has thrown an? (?<bombName>.+) Bomb on (?<world>[A-Z]{2}\\d+)$";
    
    private static final Pattern BOMB_PATTERN = Pattern.compile(BOMB_REGEX);
    
    public Optional<BombInfo> parseBombMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String cleanMessage = message.trim();
        
        // Additional validation to ensure this is actually a bomb message
        if (!isLikelyBombMessage(cleanMessage)) {
            return Optional.empty();
        }
        
        Matcher matcher = BOMB_PATTERN.matcher(cleanMessage);
        
        if (!matcher.matches()) {
            SKJMod.LOGGER.debug("Message does not match bomb pattern: {}", cleanMessage);
            return Optional.empty();
        }
        
        try {
            String playerName = matcher.group("player").trim();
            String bombName = matcher.group("bombName").trim();
            String worldName = matcher.group("world").trim();
            
            // Additional validation for player name
            if (!isValidPlayerName(playerName)) {
                SKJMod.LOGGER.warn("Invalid player name detected: '{}'", playerName);
                return Optional.empty();
            }
            
            if (!isValidWorldName(worldName)) {
                SKJMod.LOGGER.warn("Invalid world name: {}", worldName);
                return Optional.empty();
            }
            
            Optional<BombType> bombTypeOpt = identifyBombType(bombName);
            
            if (bombTypeOpt.isEmpty()) {
                SKJMod.LOGGER.warn("Unknown bomb type: {}", bombName);
                return Optional.empty();
            }
            
            BombInfo bombInfo = new BombInfo(playerName, bombTypeOpt.get(), worldName);
            return Optional.of(bombInfo);
            
        } catch (Exception e) {
            SKJMod.LOGGER.error("Error parsing bomb message: {}", message, e);
            return Optional.empty();
        }
    }
    
    private Optional<BombType> identifyBombType(String bombName) {
        return BombType.fromString(bombName);
    }
    
    private boolean isLikelyBombMessage(String message) {
        // Additional checks to ensure this is actually a bomb message and not console output
        if (message == null || message.isEmpty()) {
            return false;
        }
        
        // Exclude common console messages that might contain similar patterns
        String[] invalidPrefixes = {
            "You don't have enough mana",
            "You are now entering",
            "You are now leaving",
            "Scroll Merchant:",
            "VolumeOff",
            "Thrown At",
            "Remaining Time",
            "Expires"
        };
        
        for (String prefix : invalidPrefixes) {
            if (message.startsWith(prefix)) {
                return false;
            }
        }
        
        // Must contain the core bomb message structure
        return message.contains(" has thrown a") && message.contains(" Bomb on ");
    }
    
    private boolean isValidPlayerName(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return false;
        }
        
        // Common invalid player names that are actually system messages
        String[] invalidNames = {
            "Player", "player", "PLAYER",
            "You", "you", "YOU",
            "System", "system", "SYSTEM",
            "Server", "server", "SERVER",
            "Console", "console", "CONSOLE",
            "VolumeOff", "Thrown", "Remaining", "Expires"
        };
        
        for (String invalid : invalidNames) {
            if (playerName.equalsIgnoreCase(invalid)) {
                return false;
            }
        }
        
        // Check if it looks like a valid Minecraft username
        return playerName.matches("^[A-Za-z0-9_]{1,16}$") && 
               !playerName.matches("^[0-9_]+$"); // Not only numbers and underscores
    }
    
    private boolean isValidWorldName(String worldName) {
        if (worldName == null || worldName.length() < 3) {
            return false;
        }
        
        return worldName.matches("^[A-Z]{2}\\d+$");
    }
}