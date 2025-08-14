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