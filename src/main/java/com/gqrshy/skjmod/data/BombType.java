package com.gqrshy.skjmod.data;

import java.util.Arrays;

public enum BombType {
    // AvoMod2の2024年最新定義 + Wynntils互換性
    COMBAT_XP("Combat Experience", "Combat XP", "combat", 20, "⚔️", 0xFF4444),
    PROFESSION_XP("Profession Experience", "Profession XP", "profession", 20, "🔨", 0x44FF44),
    PROFESSION_SPEED("Profession Speed", "Prof Speed", "speed", 10, "⚡", 0xFFFF44),
    DUNGEON("Dungeon", "Dungeon", "dungeon", 10, "🏰", 0x4444FF),
    LOOT("Loot", "Loot", "loot", 20, "💰", 0xFF44FF),
    LOOT_CHEST("Loot Chest", "Loot Chest", "chest", 20, "🗂️", 0x44FFFF),
    
    // 将来の拡張用（新しい爆弾タイプ）
    MATERIAL("Material", "Material", "material", 20, "🧱", 0x888888),
    SCROLL_CHARGE("Scroll Charge", "Scroll", "scroll", 0, "📜", 0xFFAA44);
    
    private final String displayName;      // 完全な表示名
    private final String shortName;        // 短縮名
    private final String identifier;       // 内部識別子
    private final int durationMinutes;     // 継続時間
    private final String emoji;            // Discord用絵文字
    private final int color;               // Discord埋め込み色
    
    BombType(String displayName, String shortName, String identifier, 
             int durationMinutes, String emoji, int color) {
        this.displayName = displayName;
        this.shortName = shortName;
        this.identifier = identifier;
        this.durationMinutes = durationMinutes;
        this.emoji = emoji;
        this.color = color;
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public String getShortName() { return shortName; }
    public String getIdentifier() { return identifier; }
    public int getDurationMinutes() { return durationMinutes; }
    public String getEmoji() { return emoji; }
    public int getColor() { return color; }
    
    /**
     * 表示名から爆弾タイプを検索（AvoMod2 + Wynntils対応）
     */
    public static BombType fromDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) return null;
        
        String normalized = displayName.trim();
        
        return Arrays.stream(values())
            .filter(type -> 
                type.displayName.equalsIgnoreCase(normalized) ||
                type.shortName.equalsIgnoreCase(normalized) ||
                type.identifier.equalsIgnoreCase(normalized) ||
                // 部分マッチも許可（柔軟な検出）
                normalized.toLowerCase().contains(type.identifier.toLowerCase())
            )
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 部分マッチでの検索（Wynntilsスタイル）
     */
    public static BombType fromPartialMatch(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        
        String lowerText = text.toLowerCase().trim();
        
        // 完全一致を優先
        BombType exactMatch = fromDisplayName(text);
        if (exactMatch != null) return exactMatch;
        
        // 部分マッチ
        return Arrays.stream(values())
            .filter(type -> 
                lowerText.contains(type.displayName.toLowerCase()) ||
                lowerText.contains(type.shortName.toLowerCase()) ||
                lowerText.contains(type.identifier.toLowerCase())
            )
            .findFirst()
            .orElse(null);
    }
    
    /**
     * キーワードベースの検索（チャット解析用）
     */
    public static BombType fromKeywords(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        
        String lowerText = text.toLowerCase();
        
        // 特定のキーワードパターンで検索
        if (lowerText.contains("combat") && lowerText.contains("xp")) return COMBAT_XP;
        if (lowerText.contains("profession") && lowerText.contains("xp")) return PROFESSION_XP;
        if (lowerText.contains("profession") && lowerText.contains("speed")) return PROFESSION_SPEED;
        if (lowerText.contains("dungeon")) return DUNGEON;
        if (lowerText.contains("loot") && lowerText.contains("chest")) return LOOT_CHEST;
        if (lowerText.contains("loot")) return LOOT;
        if (lowerText.contains("material")) return MATERIAL;
        if (lowerText.contains("scroll")) return SCROLL_CHARGE;
        
        return null;
    }
    
    /**
     * 全ての可能な名前のリストを取得
     */
    public String[] getAllNames() {
        return new String[]{displayName, shortName, identifier};
    }
    
    /**
     * デバッグ用の文字列表現
     */
    @Override
    public String toString() {
        return String.format("%s [%s] (%d min)", displayName, identifier, durationMinutes);
    }
}