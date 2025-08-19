package com.gqrshy.skjmod.parser;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class BombInfo {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final String playerName;
    private final BombType bombType;
    private final String worldName;
    private final LocalDateTime timestamp;
    
    public BombInfo(String playerName, BombType bombType, String worldName, LocalDateTime timestamp) {
        this.playerName = playerName;
        this.bombType = bombType;
        this.worldName = worldName;
        this.timestamp = timestamp;
    }
    
    public BombInfo(String playerName, BombType bombType, String worldName) {
        this(playerName, bombType, worldName, ZonedDateTime.now(ZoneId.of("Asia/Tokyo")).toLocalDateTime());
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public BombType getBombType() {
        return bombType;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public String getFormattedTimestamp() {
        return timestamp.format(TIMESTAMP_FORMAT);
    }
    
    @Override
    public String toString() {
        return String.format("BombInfo{playerName='%s', bombType=%s, worldName='%s', timestamp=%s}",
                playerName, bombType, worldName, getFormattedTimestamp());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        BombInfo bombInfo = (BombInfo) o;
        
        if (!playerName.equals(bombInfo.playerName)) return false;
        if (bombType != bombInfo.bombType) return false;
        if (!worldName.equals(bombInfo.worldName)) return false;
        return timestamp.equals(bombInfo.timestamp);
    }
    
    @Override
    public int hashCode() {
        int result = playerName.hashCode();
        result = 31 * result + bombType.hashCode();
        result = 31 * result + worldName.hashCode();
        result = 31 * result + timestamp.hashCode();
        return result;
    }
}