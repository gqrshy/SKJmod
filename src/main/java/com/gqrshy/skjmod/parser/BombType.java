package com.gqrshy.skjmod.parser;

import java.util.Arrays;
import java.util.Optional;

public enum BombType {
    COMBAT_XP("Combat Experience", "戦闘経験値", 0xFF6B6B, 20),
    PROFESSION_XP("Profession Experience", "職業経験値", 0x4ECDC4, 20),
    PROFESSION_SPEED("Profession Speed", "職業スピード", 0x45B7D1, 10),
    DUNGEON("Dungeon", "ダンジョン", 0x9B59B6, 10),
    LOOT("Loot", "ルート", 0xF39C12, 20),
    LOOT_CHEST("Loot Chest", "ルートチェスト", 0xF1C40F, 20);
    
    private final String englishName;
    private final String displayName;
    private final int color;
    private final int durationMinutes;
    
    BombType(String englishName, String displayName, int color, int durationMinutes) {
        this.englishName = englishName;
        this.displayName = displayName;
        this.color = color;
        this.durationMinutes = durationMinutes;
    }
    
    public String getEnglishName() {
        return englishName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getColor() {
        return color;
    }
    
    public int getDurationMinutes() {
        return durationMinutes;
    }
    
    public static Optional<BombType> fromString(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        
        String trimmedName = name.trim();
        
        return Arrays.stream(BombType.values())
                .filter(bombType -> bombType.englishName.equalsIgnoreCase(trimmedName))
                .findFirst();
    }
    
    public static Optional<BombType> fromEnumName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        
        try {
            return Optional.of(BombType.valueOf(name.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
    
    @Override
    public String toString() {
        return englishName;
    }
}