package com.gqrshy.skjmod;

import net.fabricmc.api.ClientModInitializer;
import com.gqrshy.skjmod.utils.SKJLogger;

public class SKJMod implements ClientModInitializer {
    public static final String MOD_ID = "skjmod";
    public static final String MOD_NAME = "SKJmod";
    public static final String VERSION = "1.0.0";
    
    @Override
    public void onInitializeClient() {
        SKJLogger.info("Initializing " + MOD_NAME + " v" + VERSION);
        
        // 基本初期化のみ - 後でイベントリスナーを追加予定
        SKJLogger.info("SKJmod initialized successfully!");
    }
}