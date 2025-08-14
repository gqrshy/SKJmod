# SKJmod - Wynncraft BombBell Tracker

SKJmodは、Wynncraftのボムベルイベントをリアルタイムで検出し、Discord WynnTrackerボットに送信するMinecraft Modです。

## 特徴

- **リアルタイム検出**: AvoMod2とWynntilsの両方の検出パターンを統合
- **Discord連携**: WynnTrackerボット専用のAPI連携
- **複数の検出方式**: サーバーメッセージとギルド/パーティリレーに対応
- **高度なフィルタリング**: 重複検知、サーバーフィルター、爆弾タイプフィルター
- **設定可能**: JSON設定ファイルで細かい調整が可能

## 対応する爆弾タイプ

- Combat Experience (Combat XP) - 20分
- Profession Experience (Profession XP) - 20分  
- Profession Speed - 10分
- Dungeon - 10分
- Loot - 20分
- Loot Chest - 20分

## インストール

1. Fabric Loader 0.16.0以上をインストール
2. Fabric API 0.105.0+1.21.4をインストール
3. SKJmodのjarファイルをmodsフォルダに配置

## 設定

初回起動後、`config/skjmod.json`ファイルが作成されます。

### 基本設定

```json
{
  "enabled": true,
  "wynnTrackerApiUrl": "https://your-wynntracker-api.com/api/bombbell",
  "wynnTrackerApiToken": "your-api-token",
  "discordWebhookUrl": "https://discord.com/api/webhooks/your-webhook",
  "bombBellConfig": {
    "enabled": true,
    "discordNotificationEnabled": true,
    "guildRelayEnabled": true,
    "filterDuplicates": true
  }
}
```

## 開発環境

- Minecraft 1.21.4
- Fabric Loader 0.16.0
- Java 23
- VS Code推奨

## ビルド方法

```bash
./gradlew build
```

## 開発・デバッグ

```bash
./gradlew runClient
```

## ライセンス

MIT License

## 参考プロジェクト

- [Wynntils](https://github.com/Wynntils/Wynntils) - ボムベル検出パターンの参考
- [AvoMod2](https://github.com/Avocarrot/avomod2) - 正規表現パターンの参考