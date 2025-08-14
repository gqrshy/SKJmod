package com.gqrshy.skjmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.gqrshy.skjmod.utils.SKJLogger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SKJConfig {
    private static final String CONFIG_FILE = "config/skjmod.json";
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .serializeNulls()
        .create();
    
    // 基本設定
    private boolean enabled = true;
    private boolean debugMode = false;
    private String modVersion = "1.0.0";
    
    // Mod互換性設定
    private boolean wynntilsCompatibilityMode = false;
    private boolean avoModCompatibilityMode = false;
    
    // Discord/WynnTracker設定
    private String wynnTrackerApiUrl = "";
    private String wynnTrackerApiToken = "";
    private String discordWebhookUrl = "";
    private int connectionTimeout = 15; // 秒
    
    // ボムベル設定
    private BombBellConfig bombBellConfig = new BombBellConfig();
    
    // UI設定
    private UIConfig uiConfig = new UIConfig();
    
    // パフォーマンス設定
    private PerformanceConfig performanceConfig = new PerformanceConfig();
    
    /**
     * 設定の読み込み
     */
    public static SKJConfig load() {
        Path configPath = Paths.get(CONFIG_FILE);
        
        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                SKJConfig config = GSON.fromJson(json, SKJConfig.class);
                
                if (config != null) {
                    config.validateAndMigrate();
                    SKJLogger.info("Configuration loaded successfully");
                    return config;
                }
            } catch (IOException | JsonSyntaxException e) {
                SKJLogger.error("Failed to load configuration: " + e.getMessage());
                SKJLogger.info("Creating backup and using default configuration");
                backupCorruptedConfig(configPath);
            }
        }
        
        // デフォルト設定を作成
        SKJConfig defaultConfig = new SKJConfig();
        defaultConfig.save();
        SKJLogger.info("Default configuration created");
        return defaultConfig;
    }
    
    /**
     * 設定の保存
     */
    public void save() {
        try {
            Path configPath = Paths.get(CONFIG_FILE);
            Files.createDirectories(configPath.getParent());
            
            String json = GSON.toJson(this);
            Files.writeString(configPath, json);
            
            SKJLogger.debug("Configuration saved");
        } catch (IOException e) {
            SKJLogger.error("Failed to save configuration: " + e.getMessage());
        }
    }
    
    /**
     * 設定の検証とマイグレーション
     */
    private void validateAndMigrate() {
        // null チェックと初期化
        if (bombBellConfig == null) bombBellConfig = new BombBellConfig();
        if (uiConfig == null) uiConfig = new UIConfig();
        if (performanceConfig == null) performanceConfig = new PerformanceConfig();
        
        // バージョンマイグレーション
        if (modVersion == null || !modVersion.equals("1.0.0")) {
            SKJLogger.info("Migrating configuration from version " + modVersion + " to 1.0.0");
            modVersion = "1.0.0";
            // ここで必要に応じてマイグレーション処理を追加
        }
        
        // 設定値の検証
        if (connectionTimeout < 5) connectionTimeout = 15;
        if (connectionTimeout > 60) connectionTimeout = 60;
    }
    
    /**
     * 破損した設定ファイルのバックアップ
     */
    private static void backupCorruptedConfig(Path configPath) {
        try {
            Path backupPath = Paths.get(CONFIG_FILE + ".backup." + System.currentTimeMillis());
            Files.copy(configPath, backupPath);
            SKJLogger.info("Corrupted config backed up to: " + backupPath);
        } catch (IOException e) {
            SKJLogger.error("Failed to backup corrupted config: " + e.getMessage());
        }
    }
    
    /**
     * 設定のリセット
     */
    public void reset() {
        SKJConfig defaultConfig = new SKJConfig();
        
        this.enabled = defaultConfig.enabled;
        this.debugMode = defaultConfig.debugMode;
        this.wynntilsCompatibilityMode = defaultConfig.wynntilsCompatibilityMode;
        this.wynnTrackerApiUrl = defaultConfig.wynnTrackerApiUrl;
        this.wynnTrackerApiToken = defaultConfig.wynnTrackerApiToken;
        this.discordWebhookUrl = defaultConfig.discordWebhookUrl;
        this.connectionTimeout = defaultConfig.connectionTimeout;
        this.bombBellConfig = new BombBellConfig();
        this.uiConfig = new UIConfig();
        this.performanceConfig = new PerformanceConfig();
        
        save();
        SKJLogger.info("Configuration reset to defaults");
    }
    
    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isDebugMode() { return debugMode; }
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }
    
    public boolean isWynntilsCompatibilityMode() { return wynntilsCompatibilityMode; }
    public void setWynntilsCompatibilityMode(boolean wynntilsCompatibilityMode) { 
        this.wynntilsCompatibilityMode = wynntilsCompatibilityMode; 
    }
    
    public boolean isAvoModCompatibilityMode() { return avoModCompatibilityMode; }
    public void setAvoModCompatibilityMode(boolean avoModCompatibilityMode) { 
        this.avoModCompatibilityMode = avoModCompatibilityMode; 
    }
    
    public String getWynnTrackerApiUrl() { return wynnTrackerApiUrl; }
    public void setWynnTrackerApiUrl(String wynnTrackerApiUrl) { 
        this.wynnTrackerApiUrl = wynnTrackerApiUrl; 
    }
    
    public String getWynnTrackerApiToken() { return wynnTrackerApiToken; }
    public void setWynnTrackerApiToken(String wynnTrackerApiToken) { 
        this.wynnTrackerApiToken = wynnTrackerApiToken; 
    }
    
    public String getDiscordWebhookUrl() { return discordWebhookUrl; }
    public void setDiscordWebhookUrl(String discordWebhookUrl) { 
        this.discordWebhookUrl = discordWebhookUrl; 
    }
    
    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { 
        this.connectionTimeout = Math.max(5, Math.min(60, connectionTimeout)); 
    }
    
    public BombBellConfig getBombBellConfig() { return bombBellConfig; }
    public UIConfig getUIConfig() { return uiConfig; }
    public PerformanceConfig getPerformanceConfig() { return performanceConfig; }
    
    /**
     * ボムベル専用設定クラス
     */
    public static class BombBellConfig {
        private boolean enabled = true;
        private boolean discordNotificationEnabled = true;
        private boolean overlayEnabled = true;
        private boolean ingameNotificationEnabled = true;
        private boolean guildRelayEnabled = true;
        private boolean partyRelayEnabled = false;
        private boolean unicodePatternEnabled = true;
        private boolean filterDuplicates = true;
        private long duplicateTimeWindow = 5000; // 5秒
        
        // 爆弾タイプフィルター
        private boolean enableCombatXpBombs = true;
        private boolean enableProfessionXpBombs = true;
        private boolean enableProfessionSpeedBombs = true;
        private boolean enableDungeonBombs = true;
        private boolean enableLootBombs = true;
        private boolean enableLootChestBombs = true;
        
        // サーバーフィルター
        private String allowedServers = ""; // 空文字列 = 全サーバー
        private String blockedServers = "";
        
        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public boolean isDiscordNotificationEnabled() { return discordNotificationEnabled; }
        public void setDiscordNotificationEnabled(boolean discordNotificationEnabled) { 
            this.discordNotificationEnabled = discordNotificationEnabled; 
        }
        
        public boolean isOverlayEnabled() { return overlayEnabled; }
        public void setOverlayEnabled(boolean overlayEnabled) { this.overlayEnabled = overlayEnabled; }
        
        public boolean isIngameNotificationEnabled() { return ingameNotificationEnabled; }
        public void setIngameNotificationEnabled(boolean ingameNotificationEnabled) { 
            this.ingameNotificationEnabled = ingameNotificationEnabled; 
        }
        
        public boolean isGuildRelayEnabled() { return guildRelayEnabled; }
        public void setGuildRelayEnabled(boolean guildRelayEnabled) { 
            this.guildRelayEnabled = guildRelayEnabled; 
        }
        
        public boolean isPartyRelayEnabled() { return partyRelayEnabled; }
        public void setPartyRelayEnabled(boolean partyRelayEnabled) { 
            this.partyRelayEnabled = partyRelayEnabled; 
        }
        
        public boolean isUnicodePatternEnabled() { return unicodePatternEnabled; }
        public void setUnicodePatternEnabled(boolean unicodePatternEnabled) { 
            this.unicodePatternEnabled = unicodePatternEnabled; 
        }
        
        public boolean isFilterDuplicates() { return filterDuplicates; }
        public void setFilterDuplicates(boolean filterDuplicates) { 
            this.filterDuplicates = filterDuplicates; 
        }
        
        public long getDuplicateTimeWindow() { return duplicateTimeWindow; }
        public void setDuplicateTimeWindow(long duplicateTimeWindow) { 
            this.duplicateTimeWindow = Math.max(1000, Math.min(30000, duplicateTimeWindow)); 
        }
        
        /**
         * 爆弾タイプが有効かチェック
         */
        public boolean isEnabledForBombType(String bombType) {
            if (bombType == null) return true;
            
            switch (bombType.toLowerCase()) {
                case "combat experience":
                case "combat xp":
                    return enableCombatXpBombs;
                case "profession experience":
                case "profession xp":
                    return enableProfessionXpBombs;
                case "profession speed":
                    return enableProfessionSpeedBombs;
                case "dungeon":
                    return enableDungeonBombs;
                case "loot":
                    return enableLootBombs;
                case "loot chest":
                    return enableLootChestBombs;
                default:
                    return true; // 未知の爆弾タイプは通す
            }
        }
        
        /**
         * サーバーが許可されているかチェック
         */
        public boolean isServerAllowed(String server) {
            if (server == null || server.isEmpty()) return true;
            
            String normalizedServer = server.toLowerCase().trim();
            
            // ブロックリストチェック
            if (!blockedServers.isEmpty()) {
                String[] blocked = blockedServers.toLowerCase().split(",");
                for (String blockedServer : blocked) {
                    if (normalizedServer.contains(blockedServer.trim())) {
                        return false;
                    }
                }
            }
            
            // 許可リストチェック（空でない場合のみ）
            if (!allowedServers.isEmpty()) {
                String[] allowed = allowedServers.toLowerCase().split(",");
                for (String allowedServer : allowed) {
                    if (normalizedServer.contains(allowedServer.trim())) {
                        return true;
                    }
                }
                return false; // 許可リストにない
            }
            
            return true; // 許可リストが空なら全て許可
        }
        
        // Additional getters and setters...
        public boolean isEnableCombatXpBombs() { return enableCombatXpBombs; }
        public void setEnableCombatXpBombs(boolean enableCombatXpBombs) { 
            this.enableCombatXpBombs = enableCombatXpBombs; 
        }
        
        public String getAllowedServers() { return allowedServers; }
        public void setAllowedServers(String allowedServers) { 
            this.allowedServers = allowedServers != null ? allowedServers : ""; 
        }
        
        public String getBlockedServers() { return blockedServers; }
        public void setBlockedServers(String blockedServers) { 
            this.blockedServers = blockedServers != null ? blockedServers : ""; 
        }
    }
    
    /**
     * UI設定クラス
     */
    public static class UIConfig {
        private boolean showOverlay = true;
        private int overlayX = 10;
        private int overlayY = 10;
        private float overlayScale = 1.0f;
        private boolean showTooltips = true;
        private boolean compactMode = false;
        
        // Getters and Setters
        public boolean isShowOverlay() { return showOverlay; }
        public void setShowOverlay(boolean showOverlay) { this.showOverlay = showOverlay; }
        
        public int getOverlayX() { return overlayX; }
        public void setOverlayX(int overlayX) { this.overlayX = overlayX; }
        
        public int getOverlayY() { return overlayY; }
        public void setOverlayY(int overlayY) { this.overlayY = overlayY; }
        
        public float getOverlayScale() { return overlayScale; }
        public void setOverlayScale(float overlayScale) { 
            this.overlayScale = Math.max(0.5f, Math.min(3.0f, overlayScale)); 
        }
        
        public boolean isShowTooltips() { return showTooltips; }
        public void setShowTooltips(boolean showTooltips) { this.showTooltips = showTooltips; }
        
        public boolean isCompactMode() { return compactMode; }
        public void setCompactMode(boolean compactMode) { this.compactMode = compactMode; }
    }
    
    /**
     * パフォーマンス設定クラス
     */
    public static class PerformanceConfig {
        private int maxQueueSize = 100;
        private int cleanupInterval = 30; // 秒
        private boolean enableCaching = true;
        private int cacheSize = 50;
        
        // Getters and Setters
        public int getMaxQueueSize() { return maxQueueSize; }
        public void setMaxQueueSize(int maxQueueSize) { 
            this.maxQueueSize = Math.max(10, Math.min(1000, maxQueueSize)); 
        }
        
        public int getCleanupInterval() { return cleanupInterval; }
        public void setCleanupInterval(int cleanupInterval) { 
            this.cleanupInterval = Math.max(5, Math.min(300, cleanupInterval)); 
        }
        
        public boolean isEnableCaching() { return enableCaching; }
        public void setEnableCaching(boolean enableCaching) { this.enableCaching = enableCaching; }
        
        public int getCacheSize() { return cacheSize; }
        public void setCacheSize(int cacheSize) { 
            this.cacheSize = Math.max(10, Math.min(500, cacheSize)); 
        }
    }
}