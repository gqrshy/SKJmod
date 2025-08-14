package com.gqrshy.skjmod.discord;

import com.gqrshy.skjmod.data.BombEvent;
import com.gqrshy.skjmod.config.SKJConfig;
import com.gqrshy.skjmod.utils.SKJLogger;
import com.google.gson.JsonObject;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class WynnTrackerClient {
    private final SKJConfig config;
    private final HttpClient httpClient;
    private final ScheduledExecutorService executor;
    private final DiscordMessageBuilder messageBuilder;
    
    // キューイングシステム（レート制限対応）
    private final BlockingQueue<BombEvent> notificationQueue;
    private volatile boolean isRunning;
    
    // レート制限設定
    private static final long MIN_REQUEST_INTERVAL = 1000; // 1秒
    private static final int MAX_RETRIES = 3;
    
    // 統計情報
    private final NotificationStats stats = new NotificationStats();
    
    public WynnTrackerClient(SKJConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
        
        this.executor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "SKJmod-WynnTracker");
            t.setDaemon(true);
            return t;
        });
        
        this.messageBuilder = new DiscordMessageBuilder();
        this.notificationQueue = new LinkedBlockingQueue<>();
        this.isRunning = true;
        
        // キュー処理スレッドの開始
        startQueueProcessor();
        
        SKJLogger.info("WynnTracker client initialized");
    }
    
    /**
     * キュー処理スレッドの開始
     */
    private void startQueueProcessor() {
        executor.submit(() -> {
            while (isRunning) {
                try {
                    BombEvent event = notificationQueue.take(); // ブロッキング
                    processNotification(event);
                    Thread.sleep(MIN_REQUEST_INTERVAL); // レート制限
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    SKJLogger.error("Error in queue processor: " + e.getMessage(), e);
                }
            }
        });
    }
    
    /**
     * ボム通知の送信（キューに追加）
     */
    public void sendBombNotification(BombEvent event) {
        if (!config.getBombBellConfig().isDiscordNotificationEnabled()) {
            return;
        }
        
        try {
            notificationQueue.offer(event);
            SKJLogger.debug("Bomb notification queued: " + event.getBombType());
        } catch (Exception e) {
            SKJLogger.error("Failed to queue notification: " + e.getMessage(), e);
        }
    }
    
    /**
     * 通知の実際の処理
     */
    private void processNotification(BombEvent event) {
        try {
            // WynnTracker用のペイロード作成
            JsonObject payload = messageBuilder.createWynnTrackerPayload(event, config);
            
            // 送信方法の選択
            CompletableFuture<Boolean> result;
            
            if (config.getWynnTrackerApiUrl() != null && !config.getWynnTrackerApiUrl().isEmpty()) {
                // カスタムAPI経由
                result = sendToWynnTrackerApi(payload, event);
            } else if (config.getDiscordWebhookUrl() != null && !config.getDiscordWebhookUrl().isEmpty()) {
                // Webhook経由
                JsonObject webhookPayload = messageBuilder.createWebhookPayload(event, config);
                result = sendToWebhook(webhookPayload, event);
            } else {
                SKJLogger.warn("No Discord endpoint configured");
                return;
            }
            
            // 結果の処理
            result.thenAccept(success -> {
                if (success) {
                    stats.recordSuccess();
                    SKJLogger.debug("Notification sent successfully: " + event.getBombType());
                } else {
                    stats.recordFailure();
                    SKJLogger.warn("Failed to send notification: " + event.getBombType());
                }
            }).exceptionally(throwable -> {
                stats.recordFailure();
                SKJLogger.error("Notification error: " + throwable.getMessage(), throwable);
                return null;
            });
            
        } catch (Exception e) {
            stats.recordFailure();
            SKJLogger.error("Error processing notification: " + e.getMessage(), e);
        }
    }
    
    /**
     * WynnTracker APIに送信
     */
    public CompletableFuture<Boolean> sendToWynnTrackerApi(JsonObject payload, BombEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getWynnTrackerApiUrl()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getWynnTrackerApiToken())
                    .header("User-Agent", "SKJmod/1.0.0")
                    .timeout(Duration.ofSeconds(config.getConnectionTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    SKJLogger.debug("API request successful: " + response.statusCode());
                    return true;
                } else {
                    SKJLogger.warn("API request failed: " + response.statusCode() + " - " + response.body());
                    return false;
                }
            } catch (Exception e) {
                SKJLogger.error("API request error: " + e.getMessage(), e);
                return false;
            }
        }, executor);
    }
    
    /**
     * Webhookに送信
     */
    public CompletableFuture<Boolean> sendToWebhook(JsonObject payload, BombEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getDiscordWebhookUrl()))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "SKJmod/1.0.0")
                    .timeout(Duration.ofSeconds(config.getConnectionTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    SKJLogger.debug("Webhook request successful: " + response.statusCode());
                    return true;
                } else {
                    SKJLogger.warn("Webhook request failed: " + response.statusCode() + " - " + response.body());
                    return false;
                }
            } catch (Exception e) {
                SKJLogger.error("Webhook request error: " + e.getMessage(), e);
                return false;
            }
        }, executor);
    }
    
    /**
     * 接続テスト
     */
    public CompletableFuture<Boolean> testConnection() {
        JsonObject testPayload = new JsonObject();
        testPayload.addProperty("type", "connection_test");
        testPayload.addProperty("source", "skjmod");
        testPayload.addProperty("timestamp", System.currentTimeMillis());
        
        return sendToWynnTrackerApi(testPayload, null);
    }
    
    /**
     * 統計情報の取得
     */
    public NotificationStats getStats() {
        return stats;
    }
    
    /**
     * シャットダウン
     */
    public void shutdown() {
        isRunning = false;
        
        // キューの残りを処理
        while (!notificationQueue.isEmpty()) {
            try {
                BombEvent event = notificationQueue.poll();
                if (event != null) {
                    processNotification(event);
                }
            } catch (Exception e) {
                SKJLogger.error("Error during shutdown processing: " + e.getMessage());
                break;
            }
        }
        
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        
        SKJLogger.info("WynnTracker client shutdown completed");
    }
    
    /**
     * 統計情報クラス
     */
    public static class NotificationStats {
        private int totalSent = 0;
        private int successCount = 0;
        private int failureCount = 0;
        private long lastSentTime = 0;
        
        public synchronized void recordSuccess() {
            totalSent++;
            successCount++;
            lastSentTime = System.currentTimeMillis();
        }
        
        public synchronized void recordFailure() {
            totalSent++;
            failureCount++;
        }
        
        // Getters
        public int getTotalSent() { return totalSent; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public long getLastSentTime() { return lastSentTime; }
        public double getSuccessRate() { 
            return totalSent > 0 ? (double) successCount / totalSent : 0.0; 
        }
    }
}