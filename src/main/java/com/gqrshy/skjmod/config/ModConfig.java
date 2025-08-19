package com.gqrshy.skjmod.config;

import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    private String discordWebhookUrl = "";
    private boolean enableNotification = true;
    private boolean enableDebugLog = false;
    private List<String> enabledBombTypes = new ArrayList<>();
    
    public ModConfig() {
        // Initialize default enabled bomb types
        enabledBombTypes.add("COMBAT_XP");
        enabledBombTypes.add("PROFESSION_XP");
        enabledBombTypes.add("PROFESSION_SPEED");
        enabledBombTypes.add("DUNGEON");
        enabledBombTypes.add("LOOT");
        enabledBombTypes.add("LOOT_CHEST");
    }
    
    public String getDiscordWebhookUrl() {
        return discordWebhookUrl;
    }
    
    public void setDiscordWebhookUrl(String discordWebhookUrl) {
        this.discordWebhookUrl = discordWebhookUrl;
    }
    
    public boolean isEnableNotification() {
        return enableNotification;
    }
    
    public void setEnableNotification(boolean enableNotification) {
        this.enableNotification = enableNotification;
    }
    
    public boolean isEnableDebugLog() {
        return enableDebugLog;
    }
    
    public void setEnableDebugLog(boolean enableDebugLog) {
        this.enableDebugLog = enableDebugLog;
    }
    
    public List<String> getEnabledBombTypes() {
        return enabledBombTypes;
    }
    
    public void setEnabledBombTypes(List<String> enabledBombTypes) {
        this.enabledBombTypes = enabledBombTypes;
    }
    
    public boolean isBombTypeEnabled(String bombType) {
        return enabledBombTypes.contains(bombType);
    }
}