package com.gqrshy.skjmod.parser;

import com.gqrshy.skjmod.SKJMod;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BombMessageParser {
    private static final String BOMB_REGEX = 
        "(?<player>.+) has thrown an? (?<bombName>.+) Bomb on (?<world>[A-Z]{2}\\d+)";
    
    private static final Pattern BOMB_PATTERN = Pattern.compile(BOMB_REGEX);
    
    public Optional<BombInfo> parseBombMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return Optional.empty();
        }
        
        Matcher matcher = BOMB_PATTERN.matcher(message.trim());
        
        if (!matcher.matches()) {
            SKJMod.LOGGER.debug("Message does not match bomb pattern: {}", message);
            return Optional.empty();
        }
        
        try {
            String playerName = matcher.group("player").trim();
            String bombName = matcher.group("bombName").trim();
            String worldName = matcher.group("world").trim();
            
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
    
    private boolean isValidWorldName(String worldName) {
        if (worldName == null || worldName.length() < 3) {
            return false;
        }
        
        return worldName.matches("^[A-Z]{2}\\d+$");
    }
}