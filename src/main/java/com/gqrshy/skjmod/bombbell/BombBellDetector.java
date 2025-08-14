package com.gqrshy.skjmod.bombbell;

import net.minecraft.text.Text;
import com.gqrshy.skjmod.data.BombEvent;
import com.gqrshy.skjmod.data.BombType;
import com.gqrshy.skjmod.discord.WynnTrackerClient;
import com.gqrshy.skjmod.config.SKJConfig;
import com.gqrshy.skjmod.utils.ChatUtils;
import com.gqrshy.skjmod.utils.SKJLogger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class BombBellDetector {
    // AvoMod2の最新正規表現パターン（2024年アップデート対応）
    private static final Pattern AVOMOD_PATTERN = Pattern.compile(
        ".+ has thrown an? (?<bombName>.+) Bomb on (?<world>.+)"
    );
    
    // Wynntilsスタイルのパターン（調査書から推定）
    private static final Pattern WYNNTILS_PATTERN = Pattern.compile(
        "(?<player>.+) has thrown an? (?<bombName>.+) Bomb on world (?<world>\\d+)"
    );
    
    // 追加の検出パターン
    private static final Pattern ACTIVATION_PATTERN = Pattern.compile(
        "(?<bombName>.+) Bomb has been activated on (?<world>.+)"
    );
    
    // Unicode文字対応パターン（AvoMod2旧実装）
    private static final Pattern UNICODE_PATTERN = Pattern.compile(
        "^(?:\uE01E\uE002|\uE001) .+ has thrown an? (?<bombName>.+) Bomb on (?<world>.+)$"
    );
    
    // ギルドリレーパターン（Wynntilsの自動投稿を検出）
    private static final Pattern GUILD_RELAY_PATTERN = Pattern.compile(
        "\\[Guild\\] .+: (?<bombName>.+) bomb (?:on|at) (?<world>.+)"
    );
    
    private final WynnTrackerClient wynnTrackerClient;
    private final SKJConfig config;
    private final Map<String, Long> recentBombs = new ConcurrentHashMap<>();
    
    // 重複検知設定
    private static final long DUPLICATE_WINDOW = 5000; // 5秒
    
    // 全検出パターンのリスト
    private final List<DetectionPattern> detectionPatterns;
    
    public BombBellDetector(WynnTrackerClient wynnTrackerClient, SKJConfig config) {
        this.wynnTrackerClient = wynnTrackerClient;
        this.config = config;
        
        // 検出パターンの初期化
        this.detectionPatterns = initializeDetectionPatterns();
        
        SKJLogger.info("BombBell Detector initialized with " + detectionPatterns.size() + " patterns");
    }
    
    /**
     * 検出パターンの初期化（優先順位付き）
     */
    private List<DetectionPattern> initializeDetectionPatterns() {
        List<DetectionPattern> patterns = new ArrayList<>();
        
        // 優先順位1: AvoMod2の最新パターン
        patterns.add(new DetectionPattern("AvoMod2_Main", AVOMOD_PATTERN, PatternType.STANDARD));
        
        // 優先順位2: Wynntilsスタイルパターン
        patterns.add(new DetectionPattern("Wynntils_Style", WYNNTILS_PATTERN, PatternType.STANDARD));
        
        // 優先順位3: ギルドリレーパターン（Wynntilsの自動投稿）
        patterns.add(new DetectionPattern("Guild_Relay", GUILD_RELAY_PATTERN, PatternType.GUILD_RELAY));
        
        // 優先順位4: アクティベーションパターン
        patterns.add(new DetectionPattern("Activation", ACTIVATION_PATTERN, PatternType.ACTIVATION));
        
        // 優先順位5: Unicode対応パターン
        patterns.add(new DetectionPattern("Unicode", UNICODE_PATTERN, PatternType.UNICODE));
        
        return patterns;
    }
    
    /**
     * ゲームメッセージの処理
     */
    public void onGameMessage(Text message, boolean overlay) {
        if (!config.getBombBellConfig().isEnabled()) return;
        
        processMessage(message, MessageSource.GAME);
    }
    
    /**
     * チャットメッセージの処理
     */
    public void onChatMessage(Text message, boolean filterMask) {
        if (!config.getBombBellConfig().isEnabled()) return;
        if (!config.getBombBellConfig().isGuildRelayEnabled()) return;
        
        processMessage(message, MessageSource.CHAT);
    }
    
    /**
     * メッセージ処理の統合メソッド
     */
    private void processMessage(Text message, MessageSource source) {
        try {
            // メッセージの前処理（WynntilsとAvoMod2の手法を統合）
            String processedMessage = preprocessMessage(message, source);
            
            if (processedMessage == null || processedMessage.isEmpty()) {
                return;
            }
            
            // 基本的なボムキーワードチェック（性能最適化）
            if (!containsBombKeywords(processedMessage)) {
                return;
            }
            
            // パターンマッチングによる検出
            BombEvent bombEvent = detectBombEvent(processedMessage, source, message);
            
            if (bombEvent != null) {
                handleBombEvent(bombEvent);
            }
            
        } catch (Exception e) {
            SKJLogger.error("Error processing message: " + e.getMessage(), e);
        }
    }
    
    /**
     * メッセージの前処理（WynntilsとAvoMod2の手法統合）
     */
    private String preprocessMessage(Text message, MessageSource source) {
        String messageText = message.getString();
        
        // Wynntilsスタイル: タイムスタンプ除去
        messageText = ChatUtils.removeTimestamp(messageText);
        
        // AvoMod2スタイル: フォーマット文字除去
        messageText = ChatUtils.getUnformattedString(messageText);
        
        // ソースに応じた追加処理
        if (source == MessageSource.CHAT) {
            // チャットプレフィックスの処理
            messageText = ChatUtils.removeChatPrefixes(messageText);
        }
        
        return messageText.trim();
    }
    
    /**
     * 基本的なボムキーワードチェック
     */
    private boolean containsBombKeywords(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("bomb") && 
               (lowerMessage.contains("thrown") || 
                lowerMessage.contains("activated") ||
                lowerMessage.contains("on world") ||
                lowerMessage.contains("on server"));
    }
    
    /**
     * ボムイベントの検出（複数パターン対応）
     */
    private BombEvent detectBombEvent(String message, MessageSource source, Text originalMessage) {
        for (DetectionPattern pattern : detectionPatterns) {
            // パターンタイプとソースの適合性チェック
            if (!isPatternApplicable(pattern, source)) {
                continue;
            }
            
            Matcher matcher = pattern.getPattern().matcher(message);
            if (matcher.find()) {
                BombEvent event = createBombEventFromMatcher(
                    matcher, pattern, message, source, originalMessage
                );
                
                if (event != null) {
                    SKJLogger.debug("Bomb detected with pattern: " + pattern.getName());
                    return event;
                }
            }
        }
        
        return null;
    }
    
    /**
     * パターンがソースに適用可能かチェック
     */
    private boolean isPatternApplicable(DetectionPattern pattern, MessageSource source) {
        switch (pattern.getType()) {
            case GUILD_RELAY:
                return source == MessageSource.CHAT && config.getBombBellConfig().isGuildRelayEnabled();
            case UNICODE:
                return config.getBombBellConfig().isUnicodePatternEnabled();
            default:
                return true;
        }
    }
    
    /**
     * マッチャーからボムイベントを作成
     */
    private BombEvent createBombEventFromMatcher(Matcher matcher, DetectionPattern pattern, 
            String message, MessageSource source, Text originalMessage) {
        
        try {
            String bombName = matcher.group("bombName");
            String world = matcher.group("world");
            String playerName = null;
            
            // プレイヤー名の取得（パターンに応じて）
            try {
                playerName = matcher.group("player");
            } catch (IllegalArgumentException e) {
                // プレイヤーグループがない場合は無視
                playerName = ChatUtils.extractPlayerName(message);
            }
            
            // 爆弾タイプの識別
            BombType bombType = BombType.fromDisplayName(bombName);
            if (bombType == null) {
                SKJLogger.warn("Unknown bomb type detected: " + bombName);
                return null;
            }
            
            // 重複チェック
            if (isDuplicate(bombType, world)) {
                SKJLogger.debug("Duplicate bomb event ignored: " + bombType + " on " + world);
                return null;
            }
            
            // サーバーフィルタリング
            if (!config.getBombBellConfig().isServerAllowed(world)) {
                SKJLogger.debug("Bomb on blocked server ignored: " + world);
                return null;
            }
            
            // 爆弾タイプフィルタリング
            if (!config.getBombBellConfig().isEnabledForBombType(bombType.getDisplayName())) {
                SKJLogger.debug("Filtered bomb type ignored: " + bombType);
                return null;
            }
            
            // イベント作成
            return new BombEvent(
                bombType,
                world,
                System.currentTimeMillis(),
                message,
                originalMessage.getString(),
                source,
                playerName,
                pattern.getName()
            );
            
        } catch (Exception e) {
            SKJLogger.error("Error creating bomb event: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 重複チェック
     */
    private boolean isDuplicate(BombType bombType, String world) {
        if (!config.getBombBellConfig().isFilterDuplicates()) {
            return false;
        }
        
        String eventKey = bombType.name() + ":" + world;
        long currentTime = System.currentTimeMillis();
        Long lastTime = recentBombs.get(eventKey);
        
        if (lastTime != null && (currentTime - lastTime) < DUPLICATE_WINDOW) {
            return true;
        }
        
        recentBombs.put(eventKey, currentTime);
        return false;
    }
    
    /**
     * ボムイベントの処理
     */
    private void handleBombEvent(BombEvent event) {
        SKJLogger.info(String.format(
            "BombBell detected: %s on %s by %s (Pattern: %s)",
            event.getBombType().getDisplayName(),
            event.getWorld(),
            event.getPlayerName() != null ? event.getPlayerName() : "Unknown",
            event.getDetectionPattern()
        ));
        
        // Discord/WynnTracker通知
        if (config.getBombBellConfig().isDiscordNotificationEnabled()) {
            wynnTrackerClient.sendBombNotification(event);
        }
    }
    
    /**
     * 古いエントリのクリーンアップ
     */
    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        recentBombs.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > DUPLICATE_WINDOW * 2
        );
    }
    
    // 内部クラス
    public enum MessageSource {
        GAME,    // サーバー生成メッセージ
        CHAT     // プレイヤーチャット
    }
    
    private static class DetectionPattern {
        private final String name;
        private final Pattern pattern;
        private final PatternType type;
        
        public DetectionPattern(String name, Pattern pattern, PatternType type) {
            this.name = name;
            this.pattern = pattern;
            this.type = type;
        }
        
        public String getName() { return name; }
        public Pattern getPattern() { return pattern; }
        public PatternType getType() { return type; }
    }
    
    private enum PatternType {
        STANDARD,       // 標準パターン
        GUILD_RELAY,    // ギルドリレー
        ACTIVATION,     // アクティベーション
        UNICODE         // Unicode対応
    }
}