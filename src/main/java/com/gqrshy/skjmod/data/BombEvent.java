package com.gqrshy.skjmod.data;

import com.gqrshy.skjmod.bombbell.BombBellDetector.MessageSource;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class BombEvent {
    private static final DateTimeFormatter TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    
    private final BombType bombType;
    private final String world;
    private final long timestamp;
    private final String processedMessage;
    private final String originalMessage;
    private final MessageSource source;
    private final String playerName;
    private final String detectionPattern;
    
    // 追加情報
    private final String serverRegion;
    private final boolean isFromRelay;
    
    public BombEvent(BombType bombType, String world, long timestamp, 
                    String processedMessage, String originalMessage, MessageSource source, 
                    String playerName, String detectionPattern) {
        this.bombType = bombType;
        this.world = world;
        this.timestamp = timestamp;
        this.processedMessage = processedMessage;
        this.originalMessage = originalMessage;
        this.source = source;
        this.playerName = playerName;
        this.detectionPattern = detectionPattern;
        
        // 追加情報の推定
        this.serverRegion = estimateServerRegion(world);
        this.isFromRelay = source == MessageSource.CHAT && 
                          (originalMessage.contains("[Guild]") || originalMessage.contains("[Party]"));
    }
    
    /**
     * サーバー地域の推定
     */
    private String estimateServerRegion(String world) {
        if (world == null) return "Unknown";
        
        String upperWorld = world.toUpperCase();
        if (upperWorld.startsWith("EU")) return "Europe";
        if (upperWorld.startsWith("US") || upperWorld.startsWith("WC")) return "North America";
        if (upperWorld.startsWith("AS")) return "Asia";
        if (upperWorld.startsWith("SA")) return "South America";
        
        return "Unknown";
    }
    
    /**
     * 有効期限の計算
     */
    public long getExpiryTime() {
        return timestamp + (bombType.getDurationMinutes() * 60 * 1000L);
    }
    
    /**
     * まだ有効かチェック
     */
    public boolean isStillActive() {
        return System.currentTimeMillis() < getExpiryTime();
    }
    
    /**
     * 残り時間（分）
     */
    public long getRemainingMinutes() {
        long remaining = getExpiryTime() - System.currentTimeMillis();
        return Math.max(0, remaining / (60 * 1000L));
    }
    
    /**
     * フォーマットされた時刻文字列
     */
    public String getFormattedTime() {
        return TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp));
    }
    
    /**
     * 短縮表示用の文字列
     */
    public String getShortDescription() {
        return String.format("%s on %s (%dm)", 
            bombType.getShortName(), world, bombType.getDurationMinutes());
    }
    
    /**
     * 詳細表示用の文字列
     */
    public String getDetailedDescription() {
        return String.format("%s %s Bomb on %s at %s%s", 
            bombType.getEmoji(),
            bombType.getDisplayName(),
            world,
            getFormattedTime(),
            playerName != null ? " by " + playerName : ""
        );
    }
    
    // Getters
    public BombType getBombType() { return bombType; }
    public String getWorld() { return world; }
    public long getTimestamp() { return timestamp; }
    public String getProcessedMessage() { return processedMessage; }
    public String getOriginalMessage() { return originalMessage; }
    public MessageSource getSource() { return source; }
    public String getPlayerName() { return playerName; }
    public String getDetectionPattern() { return detectionPattern; }
    public String getServerRegion() { return serverRegion; }
    public boolean isFromRelay() { return isFromRelay; }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BombEvent)) return false;
        
        BombEvent other = (BombEvent) obj;
        return bombType == other.bombType && 
               Objects.equals(world, other.world) && 
               Math.abs(timestamp - other.timestamp) < 2000; // 2秒以内なら同じとみなす
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(bombType, world, timestamp / 2000); // 2秒単位でハッシュ
    }
    
    @Override
    public String toString() {
        return String.format("BombEvent{type=%s, world=%s, player=%s, time=%d, pattern=%s}", 
                           bombType, world, playerName, timestamp, detectionPattern);
    }
}