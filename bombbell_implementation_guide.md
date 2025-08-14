# SKJmod - Wynncraft BombBell検出Mod 完全実装ガイド

## 概要
Wynncraftのボムベルイベントをリアルタイムで検出し、Discord WynnTrackerボットに送信するMinecraft Mod「SKJmod」の完全実装ガイドです。AvoMod2とWynntilsの両方の実装を参考に、最新のMinecraft環境に対応します。

## 技術仕様

### 開発環境（更新）
- **Modローダー**: Fabric 1.21.4
- **開発言語**: Java 23
- **IDE**: Visual Studio Code
- **依存関係**: Fabric API, Gson, SLF4J

### 参考実装解析

#### AvoMod2の実装（最新コミット 4f8524fe）
```java
// 最新の正規表現パターン（2024年アップデート対応）
String regex = ".+ has thrown an? (?<bombName>.+) Bomb on (?<world>.+)";

// 爆弾タイプの最新定義
enum BombType {
    COMBAT_XP("Combat Experience"),          // 旧: "Combat XP"
    PROFESSION_XP("Profession Experience"),  // 旧: "Profession XP" 
    PROFESSION_SPEED("Profession Speed"),
    DUNGEON("Dungeon"),
    LOOT("Loot"),
    LOOT_CHEST("Loot Chest")                // 新規追加
}
```

#### Wynntilsの実装（調査書から）
- **ボムベルオーバーレイ機能**: ゲーム内チャットメッセージを解析してボムベル検出
- **ボムベルリレー機能**: チャンピオンプレイヤーがギルドチャットに自動投稿
- **チャット解析方式**: 特定のフレーズを識別してボムベルトラッカーに表示
- **Discord連携**: Wynntils-Botの存在による外部連携の前例

## 完全実装アーキテクチャ

### 1. プロジェクト構造（SKJmod用）
```
src/main/java/com/gqrshy/skjmod/
├── SKJMod.java                              // Modメインクラス
├── bombbell/
│   ├── BombBellDetector.java                // ボムベル検出の中核（Wynntils+AvoMod2スタイル）
│   ├── ChatMessageProcessor.java            // チャット処理（統合アプローチ）
│   ├── BombBellTracker.java                 // トラッカー機能
│   └── BombBellOverlay.java                 // ゲーム内オーバーレイ（Wynntilsスタイル）
├── discord/
│   ├── WynnTrackerClient.java               // WynnTracker専用クライアント
│   ├── DiscordMessageBuilder.java           // メッセージ構築
│   └── NotificationSender.java              // 通知送信管理
├── config/
│   ├── SKJConfig.java                       // 設定管理
│   ├── BombBellConfig.java                  // ボムベル専用設定
│   └── ConfigScreen.java                    // ModMenu連携設定画面
├── data/
│   ├── BombEvent.java                       // イベントデータ
│   ├── BombType.java                        // 爆弾タイプ（両実装統合）
│   ├── ServerInfo.java                      // サーバー情報
│   └── PlayerInfo.java                      // プレイヤー情報
├── utils/
│   ├── ChatUtils.java                       // チャットユーティリティ（Wynntils+AvoMod2）
│   ├── TextFormattingUtils.java             // テキスト処理
│   ├── TimeUtils.java                       // 時間処理
│   └── SKJLogger.java                       // ログ管理
└── mixins/
    ├── ChatHudMixin.java                    // チャットHUD拡張
    └── GameRendererMixin.java               // レンダラー拡張（オーバーレイ用）
```

### 2. 主要実装クラス

#### SKJMod.java（メインエントリーポイント）
```java
package com.gqrshy.skjmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import com.gqrshy.skjmod.bombbell.BombBellDetector;
import com.gqrshy.skjmod.config.SKJConfig;
import com.gqrshy.skjmod.discord.WynnTrackerClient;
import com.gqrshy.skjmod.utils.SKJLogger;

public class SKJMod implements ClientModInitializer {
    public static final String MOD_ID = "skjmod";
    public static final String MOD_NAME = "SKJmod";
    public static final String VERSION = "1.0.0";
    
    private static SKJConfig config;
    private static WynnTrackerClient wynnTrackerClient;
    private static BombBellDetector bombBellDetector;
    
    @Override
    public void onInitializeClient() {
        SKJLogger.info("Initializing " + MOD_NAME + " v" + VERSION);
        
        // 設定の初期化
        config = SKJConfig.load();
        
        // WynnTracker Discord クライアントの初期化
        wynnTrackerClient = new WynnTrackerClient(config);
        
        // ボムベル検出器の初期化（WynntilsとAvoMod2の統合アプローチ）
        bombBellDetector = new BombBellDetector(wynnTrackerClient, config);
        
        // イベントリスナーの登録
        registerEventListeners();
        
        // Mod間連携チェック（Wynntils検出）
        checkModCompatibility();
        
        // クライアント終了時のクリーンアップ
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            config.save();
            wynnTrackerClient.shutdown();
            SKJLogger.info("SKJmod shutdown completed");
        });
        
        SKJLogger.info("SKJmod initialized successfully!");
    }
    
    private void registerEventListeners() {
        // ゲームメッセージ（サーバー通知）の監視
        ClientReceiveMessageEvents.GAME.register(bombBellDetector::onGameMessage);
        
        // プレイヤーチャット（ギルドリレー等）の監視
        ClientReceiveMessageEvents.CHAT.register(bombBellDetector::onChatMessage);
    }
    
    /**
     * 他のMod（特にWynntils）との互換性チェック
     */
    private void checkModCompatibility() {
        try {
            // Wynntilsの検出
            Class.forName("com.wynntils.core.WynntilsMod");
            SKJLogger.info("Wynntils detected - enabling compatibility mode");
            config.setWynntilsCompatibilityMode(true);
        } catch (ClassNotFoundException e) {
            SKJLogger.info("Wynntils not detected - running in standalone mode");
            config.setWynntilsCompatibilityMode(false);
        }
        
        // 将来的には他のModとの連携も追加可能
    }
    
    public static SKJConfig getConfig() { return config; }
    public static WynnTrackerClient getWynnTrackerClient() { return wynnTrackerClient; }
    public static BombBellDetector getBombBellDetector() { return bombBellDetector; }
}
```

#### BombBellDetector.java（検出の中核 - WynntilsとAvoMod2統合）
```java
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
    private final BombBellTracker tracker;
    private final BombBellOverlay overlay;
    
    // 重複検知設定
    private static final long DUPLICATE_WINDOW = 5000; // 5秒
    
    // 全検出パターンのリスト
    private final List<DetectionPattern> detectionPatterns;
    
    public BombBellDetector(WynnTrackerClient wynnTrackerClient, SKJConfig config) {
        this.wynnTrackerClient = wynnTrackerClient;
        this.config = config;
        this.tracker = new BombBellTracker();
        this.overlay = new BombBellOverlay(config);
        
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
        
        // トラッカーに追加
        tracker.addBombEvent(event);
        
        // ゲーム内オーバーレイ表示（Wynntilsスタイル）
        if (config.getBombBellConfig().isOverlayEnabled()) {
            overlay.showBombNotification(event);
        }
        
        // ゲーム内チャット通知
        if (config.getBombBellConfig().isIngameNotificationEnabled()) {
            showIngameNotification(event);
        }
        
        // Discord/WynnTracker通知
        if (config.getBombBellConfig().isDiscordNotificationEnabled()) {
            wynnTrackerClient.sendBombNotification(event);
        }
        
        // イベントの発火（他のMod連携用）
        fireBombEvent(event);
    }
    
    /**
     * ゲーム内通知の表示
     */
    private void showIngameNotification(BombEvent event) {
        // Toast通知、アクションバーメッセージ等の実装
        // 設定に応じて複数の通知方法を提供
    }
    
    /**
     * カスタムイベントの発火
     */
    private void fireBombEvent(BombEvent event) {
        // 他のModとの連携用イベントシステム
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
```

#### BombType.java（統合版 - AvoMod2 + Wynntils）
```java
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
```

#### WynnTrackerClient.java（専用Discord連携）
```java
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
    private final NotificationSender notificationSender;
    
    // キューイングシステム（レート制限対応）
    private final BlockingQueue<BombEvent> notificationQueue;
    private volatile boolean isRunning;
    
    // レート制限設定
    private static final long MIN_REQUEST_INTERVAL = 1000; // 1秒
    private static final int MAX_RETRIES = 3;
    
    public WynnTrackerClient(SKJConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
        
        this.executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "SKJmod-WynnTracker");
            t.setDaemon(true);
            return t;
        });
        
        this.messageBuilder = new DiscordMessageBuilder();
        this.notificationSender = new NotificationSender(httpClient, config);
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
                result = notificationSender.sendToWynnTrackerApi(payload, event);
            } else if (config.getDiscordWebhookUrl() != null && !config.getDiscordWebhookUrl().isEmpty()) {
                // Webhook経由
                JsonObject webhookPayload = messageBuilder.createWebhookPayload(event, config);
                result = notificationSender.sendToWebhook(webhookPayload, event);
            } else {
                SKJLogger.warn("No Discord endpoint configured");
                return;
            }
            
            // 結果の処理
            result.thenAccept(success -> {
                if (success) {
                    SKJLogger.debug("Notification sent successfully: " + event.getBombType());
                } else {
                    SKJLogger.warn("Failed to send notification: " + event.getBombType());
                }
            }).exceptionally(throwable -> {
                SKJLogger.error("Notification error: " + throwable.getMessage(), throwable);
                return null;
            });
            
        } catch (Exception e) {
            SKJLogger.error("Error processing notification: " + e.getMessage(), e);
        }
    }
    
    /**
     * 直接送信（緊急時用）
     */
    public CompletableFuture<Boolean> sendImmediateNotification(BombEvent event) {
        JsonObject payload = messageBuilder.createWynnTrackerPayload(event, config);
        return notificationSender.sendToWynnTrackerApi(payload, event);
    }
    
    /**
     * 接続テスト
     */
    public CompletableFuture<Boolean> testConnection() {
        JsonObject testPayload = new JsonObject();
        testPayload.addProperty("type", "connection_test");
        testPayload.addProperty("source", "skjmod");
        testPayload.addProperty("timestamp", System.currentTimeMillis());
        
        return notificationSender.sendToWynnTrackerApi(testPayload, null);
    }
    
    /**
     * 統計情報の取得
     */
    public NotificationStats getStats() {
        return notificationSender.getStats();
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
```

#### ChatUtils.java（WynntilsとAvoMod2統合ユーティリティ）
```java
package com.gqrshy.skjmod.utils;

import net.minecraft.text.Text;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ChatUtils {
    // タイムスタンプパターン（複数形式対応）
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(
        "^\\[\\d{1,2}:\\d{2}(?::\\d{2})?\\]\\s*"
    );
    
    // Minecraft フォーマットコード
    private static final Pattern FORMAT_CODE_PATTERN = Pattern.compile("§[0-9a-fk-or]");
    
    // Unicode制御文字（AvoMod2で使用）
    private static final Pattern UNICODE_CONTROL_PATTERN = Pattern.compile("[\uE000-\uF8FF]");
    
    // チャットプレフィックスパターン
    private static final Pattern CHAT_PREFIX_PATTERN = Pattern.compile(
        "^\\[(Guild|Party|Team|Staff|VIP|Champion)\\]\\s*"
    );
    
    // プレイヤー名抽出パターン
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile(
        "^([\\w\\[\\]§0-9a-fk-or]+?)\\s*[:>]"
    );
    
    /**
     * タイムスタンプを除去（Wynntilsスタイル）
     */
    public static String removeTimestamp(String message) {
        if (message == null) return "";
        return TIMESTAMP_PATTERN.matcher(message).replaceFirst("");
    }
    
    /**
     * タイムスタンプを除去したTextオブジェクトを作成
     */
    public static Text textWithoutTimeStamp(Text message) {
        String messageStr = message.getString();
        String withoutTimestamp = removeTimestamp(messageStr);
        return Text.literal(withoutTimestamp);
    }
    
    /**
     * フォーマットされていない文字列を取得（AvoMod2スタイル拡張）
     */
    public static String getUnformattedString(String text) {
        if (text == null) return "";
        
        String result = text;
        
        // Minecraftフォーマットコードの除去
        result = FORMAT_CODE_PATTERN.matcher(result).replaceAll("");
        
        // Unicode制御文字の除去
        result = UNICODE_CONTROL_PATTERN.matcher(result).replaceAll("");
        
        // 余分な空白の除去
        result = result.replaceAll("\\s+", " ").trim();
        
        return result;
    }
    
    /**
     * チャットメッセージからメッセージ部分のみを抽出
     */
    public static String getChatMessageWithOnlyMessage(Text message) {
        String fullMessage = message.getString();
        
        // タイムスタンプの除去
        String withoutTimestamp = removeTimestamp(fullMessage);
        
        // プレフィックスの除去
        String withoutPrefixes = removeChatPrefixes(withoutTimestamp);
        
        return getUnformattedString(withoutPrefixes);
    }
    
    /**
     * チャットプレフィックスの除去
     */
    public static String removeChatPrefixes(String message) {
        if (message == null) return "";
        
        String result = message;
        
        // 標準的なプレフィックスを除去
        result = CHAT_PREFIX_PATTERN.matcher(result).replaceFirst("");
        
        // カスタムプレフィックス（角括弧内）の除去
        result = result.replaceFirst("^\\[[^\\]]*\\]\\s*", "");
        
        return result.trim();
    }
    
    /**
     * プレイヤー名の抽出
     */
    public static String extractPlayerName(String message) {
        if (message == null || message.isEmpty()) return null;
        
        Matcher matcher = PLAYER_NAME_PATTERN.matcher(message);
        if (matcher.find()) {
            String playerName = matcher.group(1);
            return getUnformattedString(playerName);
        }
        
        return null;
    }
    
    /**
     * ボムキーワードを含むかチェック
     */
    public static boolean containsBombKeywords(String message) {
        if (message == null) return false;
        
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("bomb") && 
               (lowerMessage.contains("thrown") || 
                lowerMessage.contains("activated") ||
                lowerMessage.contains("on world") ||
                lowerMessage.contains("on server"));
    }
    
    /**
     * メッセージがWynncraftサーバーからのものかチェック
     */
    public static boolean isWynncraftMessage(Text message) {
        // Wynncraftサーバー特有のパターンをチェック
        String messageStr = message.getString();
        
        // 特定のプレフィックスやフォーマットをチェック
        return messageStr.contains("world") || 
               messageStr.contains("server") ||
               messageStr.contains("WC") ||
               messageStr.matches(".*\\b[A-Z]{2,3}\\d+\\b.*"); // WC1, EU2 等
    }
    
    /**
     * チャットメッセージの種類を判定
     */
    public static ChatMessageType detectMessageType(Text message) {
        String messageStr = message.getString();
        String lowerMessage = messageStr.toLowerCase();
        
        if (messageStr.startsWith("[Guild]")) return ChatMessageType.GUILD;
        if (messageStr.startsWith("[Party]")) return ChatMessageType.PARTY;
        if (messageStr.startsWith("[Team]")) return ChatMessageType.TEAM;
        if (messageStr.startsWith("[Staff]")) return ChatMessageType.STAFF;
        
        if (containsBombKeywords(messageStr)) return ChatMessageType.BOMB_BELL;
        
        if (lowerMessage.contains("has joined") || lowerMessage.contains("has left")) {
            return ChatMessageType.PLAYER_JOIN_LEAVE;
        }
        
        if (lowerMessage.contains("territory") || lowerMessage.contains("war")) {
            return ChatMessageType.TERRITORY;
        }
        
        return ChatMessageType.GENERAL;
    }
    
    /**
     * サーバー名の正規化
     */
    public static String normalizeServerName(String serverName) {
        if (serverName == null) return "";
        
        // "world 1" -> "WC1", "server EU2" -> "EU2" 等
        String normalized = serverName.trim().toUpperCase();
        
        // "WORLD" プレフィックスの除去
        normalized = normalized.replaceFirst("^WORLD\\s+", "");
        
        // "SERVER" プレフィックスの除去
        normalized = normalized.replaceFirst("^SERVER\\s+", "");
        
        // 数字のみの場合はWCプレフィックスを追加
        if (normalized.matches("\\d+")) {
            normalized = "WC" + normalized;
        }
        
        return normalized;
    }
    
    /**
     * チャットメッセージタイプの列挙
     */
    public enum ChatMessageType {
        GUILD,
        PARTY,
        TEAM,
        STAFF,
        BOMB_BELL,
        PLAYER_JOIN_LEAVE,
        TERRITORY,
        GENERAL
    }
}
```

#### SKJConfig.java（設定管理 - Fabric 1.21.4対応）
```java
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
```

#### BombEvent.java（統合イベントデータクラス）
```java
package com.skj.skjmod.data;

import com.skj.skjmod.bombbell.BombBellDetector.MessageSource;
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
```

#### build.gradle（Fabric 1.21.4 + Java 23対応）
```gradle
plugins {
    id 'fabric-loom' version '1.4-SNAPSHOT'
    id 'maven-publish'
}

version = project.mod_version
group = project.maven_group

base {
    archivesName = project.archives_base_name
}

repositories {
    mavenCentral()
    
    // Fabric公式リポジトリ
    maven {
        name = 'Fabric'
        url = 'https://maven.fabricmc.net/'
    }
    
    // ModMenu用（設定画面）
    maven {
        name = 'TerraformersMC'
        url = 'https://maven.terraformersmc.com/'
    }
    
    // Cloth Config用
    maven {
        name = 'Shedaniel'
        url = 'https://maven.shedaniel.me/'
    }
}

dependencies {
    // Minecraft と Fabric の基本依存関係（1.21.4対応）
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    
    // Fabric API（1.21.4対応）
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
    
    // HTTP通信用（Java 23対応）
    implementation 'com.google.code.gson:gson:2.10.1'
    include 'com.google.code.gson:gson:2.10.1'
    
    // ログ用
    implementation 'org.slf4j:slf4j-api:2.0.9'
    
    // 設定画面用（ModMenu連携）
    modImplementation "com.terraformersmc:modmenu:${project.modmenu_version}"
    
    // Cloth Config（設定画面）
    modImplementation("me.shedaniel.cloth:cloth-config-fabric:${project.cloth_config_version}") {
        exclude(group: "net.fabricmc.fabric-api")
    }
    
    // テスト用
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
    testImplementation 'org.mockito:mockito-core:5.6.0'
}

// Java 23対応
tasks.withType(JavaCompile).configureEach {
    it.options.release = 23
    it.options.compilerArgs.addAll([
        '--enable-preview',  // Preview機能を有効化（必要に応じて）
        '-Xlint:unchecked',
        '-Xlint:deprecation'
    ])
}

java {
    withSourcesJar()
    
    sourceCompatibility = JavaVersion.VERSION_23
    targetCompatibility = JavaVersion.VERSION_23
}

processResources {
    inputs.property "version", project.version
    inputs.property "minecraft_version", project.minecraft_version
    inputs.property "loader_version", project.loader_version
    
    filesMatching("fabric.mod.json") {
        expand "version": project.version,
               "minecraft_version": project.minecraft_version,
               "loader_version": project.loader_version,
               "java_version": "23"
    }
}

jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

// テスト設定
test {
    useJUnitPlatform()
    jvmArgs '--enable-preview'  // Preview機能を有効化（必要に応じて）
}

// リリース用のタスク
task buildRelease {
    dependsOn build
    doLast {
        println "SKJmod v${version} built successfully for Minecraft ${project.minecraft_version}"
    }
}
```

#### gradle.properties（最新版対応）
```properties
# Fabric Properties（1.21.4対応）
minecraft_version=1.21.4
yarn_mappings=1.21.4+build.1
loader_version=0.16.0

# Mod Properties
mod_version=1.0.0
maven_group=com.skj.skjmod
archives_base_name=skjmod

# Dependencies（最新版）
fabric_version=0.105.0+1.21.4
modmenu_version=11.0.1
cloth_config_version=15.0.140

# Build Properties
org.gradle.jvmargs=-Xmx2G
org.gradle.parallel=true
org.gradle.caching=true
```

#### fabric.mod.json（SKJmod用設定）
```json
{
  "schemaVersion": 1,
  "id": "skjmod",
  "version": "${version}",
  
  "name": "SKJmod",
  "description": "Real-time Wynncraft bomb bell detection with Discord integration via WynnTracker",
  "authors": [
    "SKJ"
  ],
  "contact": {
    "homepage": "https://github.com/yourusername/skjmod",
    "sources": "https://github.com/yourusername/skjmod",
    "issues": "https://github.com/yourusername/skjmod/issues"
  },
  
  "license": "MIT",
  "icon": "assets/skjmod/icon.png",
  
  "environment": "client",
  "entrypoints": {
    "client": [
      "com.skj.skjmod.SKJMod"
    ],
    "modmenu": [
      "com.skj.skjmod.config.ModMenuIntegration"
    ]
  },
  
  "mixins": [
    "skjmod.mixins.json"
  ],

  "depends": {
    "fabricloader": ">=${loader_version}",
    "fabric-api": "*",
    "minecraft": "${minecraft_version}",
    "java": ">=${java_version}"
  },
  "suggests": {
    "modmenu": "*",
    "cloth-config": "*"
  },
  "breaks": {
    "wynntils": "<4.0.0",
    "avomod": "*"
  },
  
  "custom": {
    "modmenu": {
      "badges": ["client"],
      "parent": "skjmod"
    },
    "loom:injected_interfaces": {
      "net/minecraft/class_408": ["com/skj/skjmod/mixins/ChatHudAccessor"]
    }
  }
}
```

#### VS Code用設定（.vscode/settings.json）
```json
{
    "java.configuration.updateBuildConfiguration": "automatic",
    "java.compile.nullAnalysis.mode": "automatic",
    "java.format.settings.url": "https://raw.githubusercontent.com/google/styleguide/gh-pages/eclipse-java-google-style.xml",
    "java.saveActions.organizeImports": true,
    "java.project.sourcePaths": [
        "src/main/java",
        "src/client/java"
    ],
    "java.project.resourceFilters": [
        "**/.metadata",
        "**/archetype-resources",
        "**/META-INF/maven"
    ],
    "files.exclude": {
        "**/.gradle": true,
        "**/build": true,
        "**/.fabric": true,
        "**/run": true
    },
    "editor.tabSize": 4,
    "editor.insertSpaces": true,
    "editor.detectIndentation": false,
    "java.debug.settings.hotCodeReplace": "auto",
    "java.test.defaultConfig": "default"
}
```

#### VS Code用タスク（.vscode/tasks.json）
```json
{
    "version": "2.0.0",
    "tasks": [
        {
            "label": "Build SKJmod",
            "type": "shell",
            "command": "./gradlew",
            "args": ["build"],
            "group": {
                "kind": "build",
                "isDefault": true
            },
            "presentation": {
                "echo": true,
                "reveal": "always",
                "focus": false,
                "panel": "shared"
            },
            "problemMatcher": ["$gradle"]
        },
        {
            "label": "Run Minecraft Client",
            "type": "shell",
            "command": "./gradlew",
            "args": ["runClient"],
            "group": "test",
            "presentation": {
                "echo": true,
                "reveal": "always",
                "focus": false,
                "panel": "shared"
            },
            "problemMatcher": ["$gradle"]
        },
        {
            "label": "Clean Project",
            "type": "shell",
            "command": "./gradlew",
            "args": ["clean"],
            "group": "build",
            "presentation": {
                "echo": true,
                "reveal": "always",
                "focus": false,
                "panel": "shared"
            },
            "problemMatcher": ["$gradle"]
        },
        {
            "label": "Generate Sources",
            "type": "shell",
            "command": "./gradlew",
            "args": ["genSources"],
            "group": "build",
            "presentation": {
                "echo": true,
                "reveal": "always",
                "focus": false,
                "panel": "shared"
            },
            "problemMatcher": ["$gradle"]
        }
    ]
}
```

### WynnTracker Discord Bot側の対応（Node.js例）

#### API エンドポイント追加
```javascript
// WynnTrackerボット側のexpress.js実装例
const express = require('express');
const { EmbedBuilder } = require('discord.js');

// SKJmod専用エンドポイント
app.post('/api/skjmod/bombbell', authenticateToken, async (req, res) => {
    try {
        const { 
            bombType, 
            bombDisplayName, 
            world, 
            timestamp, 
            playerName, 
            metadata,
            source,
            detectionPattern 
        } = req.body;
        
        // バリデーション
        if (!bombType || !world || !timestamp) {
            return res.status(400).json({ 
                error: 'Missing required fields',
                required: ['bombType', 'world', 'timestamp']
            });
        }
        
        // SKJmod専用の通知作成
        const embed = new EmbedBuilder()
            .setTitle(`${metadata.emoji} ${bombDisplayName} Bomb`)
            .setDescription(`
                **Server:** ${world}
                **Duration:** ${metadata.duration} minutes
                **Source:** ${source}
                **Detection:** ${detectionPattern}
                ${playerName ? `**Player:** ${playerName}` : ''}
            `)
            .setColor(getBombColor(bombType))
            .setTimestamp(new Date(timestamp))
            .setFooter({ 
                text: 'SKJmod • Wynncraft BombBell Tracker',
                iconURL: 'https://cdn.wynncraft.com/img/ico/favicon-16x16.png'
            });
        
        // 地域別チャンネルへの送信
        const region = metadata.serverRegion || 'Unknown';
        const channelId = getRegionalChannelId(region);
        
        const channel = client.channels.cache.get(channelId);
        if (channel) {
            await channel.send({ embeds: [embed] });
        }
        
        // 統計の更新
        updateBombStats(bombType, world, source);
        
        res.json({ 
            success: true, 
            message: 'SKJmod bomb notification processed',
            channelId: channelId,
            region: region
        });
        
    } catch (error) {
        console.error('SKJmod BombBell API error:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

function getBombColor(bombType) {
    const colors = {
        'COMBAT_XP': 0xFF4444,
        'PROFESSION_XP': 0x44FF44,
        'PROFESSION_SPEED': 0xFFFF44,
        'DUNGEON': 0x4444FF,
        'LOOT': 0xFF44FF,
        'LOOT_CHEST': 0x44FFFF
    };
    return colors[bombType] || 0x888888;
}

function getRegionalChannelId(region) {
    const channels = {
        'North America': process.env.NA_BOMBBELL_CHANNEL_ID,
        'Europe': process.env.EU_BOMBBELL_CHANNEL_ID,
        'Asia': process.env.AS_BOMBBELL_CHANNEL_ID,
        'Unknown': process.env.DEFAULT_BOMBBELL_CHANNEL_ID
    };
    return channels[region] || channels['Unknown'];
}
```

### 開発・テスト戦略

#### 単体テスト例
```java
// JUnit 5 + Mockito を使用
@Test
public void testBombBellPatternDetection() {
    // Given
    SKJConfig config = new SKJConfig();
    WynnTrackerClient mockClient = mock(WynnTrackerClient.class);
    BombBellDetector detector = new BombBellDetector(mockClient, config);
    
    // When
    String testMessage = "TestPlayer has thrown a Combat Experience Bomb on WC1";
    Text mockText = Text.literal(testMessage);
    detector.onGameMessage(mockText, false);
    
    // Then
    verify(mockClient, times(1)).sendBombNotification(any(BombEvent.class));
}
```

#### VS Code用デバッグ設定（.vscode/launch.json）
```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "Debug Minecraft Client",
            "request": "launch",
            "mainClass": "net.fabricmc.devlaunchinjector.Main",
            "vmArgs": [
                "-Dfabric.dli.config=.gradle/loom-cache/launch.cfg",
                "-Dfabric.dli.env=client",
                "--enable-preview"
            ],
            "args": [],
            "cwd": "${workspaceFolder}",
            "console": "integratedTerminal"
        },
        {
            "type": "java",
            "name": "Debug Tests",
            "request": "launch",
            "mainClass": "org.junit.platform.console.ConsoleLauncher",
            "vmArgs": [
                "--enable-preview"
            ],
            "args": [
                "--scan-classpath"
            ]
        }
    ]
}
```

この完全な実装ガイドで、SKJmodはWynntilsとAvoMod2の両方の実装を参考にしながら、最新のFabric 1.21.4とJava 23環境で動作する包括的なボムベル検出Modとして作成できます。特にVS Code開発環境とWynnTracker Discord Bot連携に最適化されています。