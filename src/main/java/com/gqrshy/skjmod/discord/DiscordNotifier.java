package com.gqrshy.skjmod.discord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.gqrshy.skjmod.SKJMod;
import com.gqrshy.skjmod.config.ConfigManager;
import com.gqrshy.skjmod.parser.BombInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class DiscordNotifier {
    private final ConfigManager configManager;
    private final HttpClient httpClient;
    private final Gson gson;
    
    public DiscordNotifier(ConfigManager configManager) {
        this.configManager = configManager;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new GsonBuilder().create();
    }
    
    public void sendBombNotification(BombInfo bombInfo) {
        String webhookUrl = configManager.getConfig().getDiscordWebhookUrl();
        
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            SKJMod.LOGGER.warn("Discord webhook URL is not configured");
            return;
        }
        
        CompletableFuture.supplyAsync(() -> {
            try {
                WebhookMessage message = createWebhookMessage(bombInfo);
                return sendWebhookRequest(webhookUrl, message);
            } catch (Exception e) {
                SKJMod.LOGGER.error("Failed to send Discord notification", e);
                return false;
            }
        }).thenAccept(success -> {
            if (success) {
                SKJMod.LOGGER.info("Discord notification sent successfully for: {}", bombInfo);
            } else {
                SKJMod.LOGGER.error("Failed to send Discord notification for: {}", bombInfo);
            }
        });
    }
    
    private WebhookMessage createWebhookMessage(BombInfo bombInfo) {
        String title = String.format("**%s Bomb** thrown on `%s`", 
                bombInfo.getBombType().getEnglishName(), bombInfo.getWorldName());
        
        String playerValue = String.format("`%s`", bombInfo.getPlayerName());
        WebhookMessage.Field playerField = new WebhookMessage.Field(
                "Player", playerValue, true);
        
        String timestampValue = String.format("<t:%d:f>", getUnixTimestamp(bombInfo.getTimestamp()));
        WebhookMessage.Field timestampField = new WebhookMessage.Field(
                "Thrown At", timestampValue, true);
        
        String remainingTimeValue = calculateRemainingTime(bombInfo);
        WebhookMessage.Field remainingTimeField = new WebhookMessage.Field(
                "Remaining Time", remainingTimeValue, true);
        
        // Calculate expiration time by adding bomb duration to the thrown time (in seconds)
        // Bomb durations: Combat/Profession XP/Loot/Loot Chest = 20min, Profession Speed/Dungeon = 10min
        long thrownUnixTime = getUnixTimestamp(bombInfo.getTimestamp());
        long durationInSeconds = bombInfo.getBombType().getDurationMinutes() * 60L;
        long expirationUnixTime = thrownUnixTime + durationInSeconds;
        String expiresAtValue = String.format("<t:%d:R>", expirationUnixTime);
        WebhookMessage.Field expiresField = new WebhookMessage.Field(
                "Expires", expiresAtValue, true);
        
        WebhookMessage.Embed embed = new WebhookMessage.Embed(
                title, bombInfo.getBombType().getColor(), 
                Arrays.asList(playerField, timestampField, remainingTimeField, expiresField));
        
        return new WebhookMessage(Arrays.asList(embed));
    }
    
    private String calculateRemainingTime(BombInfo bombInfo) {
        LocalDateTime thrownTime = bombInfo.getTimestamp();
        LocalDateTime expirationTime = thrownTime.plusMinutes(bombInfo.getBombType().getDurationMinutes());
        LocalDateTime now = LocalDateTime.now();
        
        if (now.isAfter(expirationTime)) {
            return "🔴 `Expired`";
        }
        
        long minutesRemaining = ChronoUnit.MINUTES.between(now, expirationTime);
        if (minutesRemaining <= 0) {
            return "🔴 `Expired`";
        } else if (minutesRemaining <= 5) {
            return String.format("🔴 `%d min`", minutesRemaining);
        } else if (minutesRemaining <= 10) {
            return String.format("🟡 `%d min`", minutesRemaining);
        } else {
            return String.format("🟢 `%d min`", minutesRemaining);
        }
    }
    
    
    private long getUnixTimestamp(LocalDateTime dateTime) {
        // Use Japan Standard Time (JST) which is UTC+9
        return dateTime.atZone(ZoneId.of("Asia/Tokyo")).toEpochSecond();
    }
    
    private boolean sendWebhookRequest(String webhookUrl, WebhookMessage message) {
        try {
            String jsonPayload = gson.toJson(message);
            
            if (configManager.getConfig().isEnableDebugLog()) {
                SKJMod.LOGGER.debug("Sending Discord webhook: {}", jsonPayload);
            }
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "SKJMod/1.0.0")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return true;
            } else {
                SKJMod.LOGGER.error("Discord webhook failed with status {}: {}", 
                        response.statusCode(), response.body());
                return false;
            }
            
        } catch (Exception e) {
            SKJMod.LOGGER.error("Exception occurred while sending Discord webhook", e);
            return false;
        }
    }
}