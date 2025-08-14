package com.gqrshy.skjmod.discord;

import com.gqrshy.skjmod.data.BombEvent;
import com.gqrshy.skjmod.config.SKJConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class DiscordMessageBuilder {
    
    /**
     * WynnTracker API用のペイロードを作成
     */
    public JsonObject createWynnTrackerPayload(BombEvent event, SKJConfig config) {
        JsonObject payload = new JsonObject();
        
        // 基本情報
        payload.addProperty("type", "bomb_bell");
        payload.addProperty("source", "skjmod");
        payload.addProperty("version", "1.0.0");
        payload.addProperty("timestamp", event.getTimestamp());
        
        // ボム情報
        payload.addProperty("bombType", event.getBombType().name());
        payload.addProperty("bombDisplayName", event.getBombType().getDisplayName());
        payload.addProperty("world", event.getWorld());
        payload.addProperty("playerName", event.getPlayerName());
        payload.addProperty("detectionPattern", event.getDetectionPattern());
        
        // メタデータ
        JsonObject metadata = new JsonObject();
        metadata.addProperty("emoji", event.getBombType().getEmoji());
        metadata.addProperty("color", event.getBombType().getColor());
        metadata.addProperty("duration", event.getBombType().getDurationMinutes());
        metadata.addProperty("serverRegion", event.getServerRegion());
        metadata.addProperty("isFromRelay", event.isFromRelay());
        payload.add("metadata", metadata);
        
        return payload;
    }
    
    /**
     * Discord Webhook用のペイロードを作成
     */
    public JsonObject createWebhookPayload(BombEvent event, SKJConfig config) {
        JsonObject payload = new JsonObject();
        
        // Webhook基本設定
        payload.addProperty("username", "SKJmod BombBell Tracker");
        payload.addProperty("avatar_url", "https://cdn.wynncraft.com/img/ico/favicon-32x32.png");
        
        // エンベッド作成
        JsonArray embeds = new JsonArray();
        JsonObject embed = createEmbed(event);
        embeds.add(embed);
        payload.add("embeds", embeds);
        
        return payload;
    }
    
    /**
     * Discord埋め込みメッセージを作成
     */
    private JsonObject createEmbed(BombEvent event) {
        JsonObject embed = new JsonObject();
        
        // 基本情報
        embed.addProperty("title", event.getBombType().getEmoji() + " " + event.getBombType().getDisplayName() + " Bomb");
        embed.addProperty("color", event.getBombType().getColor());
        embed.addProperty("timestamp", java.time.Instant.ofEpochMilli(event.getTimestamp()).toString());
        
        // 説明文
        StringBuilder description = new StringBuilder();
        description.append("**Server:** ").append(event.getWorld()).append("\n");
        description.append("**Duration:** ").append(event.getBombType().getDurationMinutes()).append(" minutes\n");
        description.append("**Region:** ").append(event.getServerRegion()).append("\n");
        
        if (event.getPlayerName() != null) {
            description.append("**Player:** ").append(event.getPlayerName()).append("\n");
        }
        
        description.append("**Detection:** ").append(event.getDetectionPattern());
        
        if (event.isFromRelay()) {
            description.append("\n*🔄 From guild/party relay*");
        }
        
        embed.addProperty("description", description.toString());
        
        // フッター
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "SKJmod • Wynncraft BombBell Tracker");
        footer.addProperty("icon_url", "https://cdn.wynncraft.com/img/ico/favicon-16x16.png");
        embed.add("footer", footer);
        
        // フィールド（詳細情報）
        JsonArray fields = new JsonArray();
        
        // 残り時間フィールド
        if (event.getBombType().getDurationMinutes() > 0) {
            JsonObject remainingField = new JsonObject();
            remainingField.addProperty("name", "⏰ Remaining Time");
            remainingField.addProperty("value", event.getRemainingMinutes() + " minutes");
            remainingField.addProperty("inline", true);
            fields.add(remainingField);
        }
        
        // 検出時刻フィールド
        JsonObject timeField = new JsonObject();
        timeField.addProperty("name", "🕐 Detected At");
        timeField.addProperty("value", event.getFormattedTime());
        timeField.addProperty("inline", true);
        fields.add(timeField);
        
        // ソースフィールド
        JsonObject sourceField = new JsonObject();
        sourceField.addProperty("name", "📡 Source");
        sourceField.addProperty("value", event.getSource().toString());
        sourceField.addProperty("inline", true);
        fields.add(sourceField);
        
        embed.add("fields", fields);
        
        return embed;
    }
    
    /**
     * シンプルなテキストメッセージを作成
     */
    public JsonObject createSimpleMessage(BombEvent event) {
        JsonObject payload = new JsonObject();
        
        String message = String.format("%s **%s Bomb** on **%s** (%d min)%s", 
            event.getBombType().getEmoji(),
            event.getBombType().getDisplayName(),
            event.getWorld(),
            event.getBombType().getDurationMinutes(),
            event.getPlayerName() != null ? " by " + event.getPlayerName() : ""
        );
        
        payload.addProperty("content", message);
        payload.addProperty("username", "SKJmod");
        
        return payload;
    }
    
    /**
     * エラーメッセージを作成
     */
    public JsonObject createErrorMessage(String error, String details) {
        JsonObject payload = new JsonObject();
        
        JsonArray embeds = new JsonArray();
        JsonObject embed = new JsonObject();
        
        embed.addProperty("title", "⚠️ SKJmod Error");
        embed.addProperty("description", "**Error:** " + error + "\n**Details:** " + details);
        embed.addProperty("color", 0xFF0000); // 赤色
        embed.addProperty("timestamp", java.time.Instant.now().toString());
        
        embeds.add(embed);
        payload.add("embeds", embeds);
        
        return payload;
    }
    
    /**
     * 統計メッセージを作成
     */
    public JsonObject createStatsMessage(WynnTrackerClient.NotificationStats stats) {
        JsonObject payload = new JsonObject();
        
        JsonArray embeds = new JsonArray();
        JsonObject embed = new JsonObject();
        
        embed.addProperty("title", "📊 SKJmod Statistics");
        embed.addProperty("color", 0x00FF00); // 緑色
        
        StringBuilder description = new StringBuilder();
        description.append("**Total Sent:** ").append(stats.getTotalSent()).append("\n");
        description.append("**Success:** ").append(stats.getSuccessCount()).append("\n");
        description.append("**Failed:** ").append(stats.getFailureCount()).append("\n");
        description.append("**Success Rate:** ").append(String.format("%.1f%%", stats.getSuccessRate() * 100));
        
        if (stats.getLastSentTime() > 0) {
            long timeSince = System.currentTimeMillis() - stats.getLastSentTime();
            description.append("\n**Last Sent:** ").append(timeSince / 1000).append(" seconds ago");
        }
        
        embed.addProperty("description", description.toString());
        embed.addProperty("timestamp", java.time.Instant.now().toString());
        
        embeds.add(embed);
        payload.add("embeds", embeds);
        
        return payload;
    }
}