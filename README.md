# SKJ Mod

WynnCraftサーバーでChampion rankプレイヤーが使用する「Bombbell」機能を検知し、Discord Webhookを通じて通知するMinecraft Fabric 1.21.4 Modです。

## 機能

### 主要機能
- **チャットログ監視**: Minecraftクライアント内のチャットログを常時監視
- **Bombbell情報解析**: プレイヤー名、Bomb名、サーバー名、投げられた日時を自動抽出
- **Discord通知**: Webhook経由でDiscordに通知送信

### 対応Bomb種類
- Combat Experience Bomb (戦闘経験値)
- Profession Experience Bomb (職業経験値)
- Profession Speed Bomb (職業スピード)
- Dungeon Bomb (ダンジョン)
- Loot Bomb (ルート)
- Loot Chest Bomb (ルートチェスト)

## インストール

### 必要環境
- Minecraft 1.21.4
- Fabric Loader 0.16.9以上
- Fabric API 0.110.5+1.21.4以上
- Java 21以上

### インストール手順
1. Fabric Loaderをインストール
2. Fabric APIをmodsフォルダに配置
3. ビルドされたmodファイル(`build/libs/skjmod-1.0.0.jar`)をmodsフォルダに配置

## 設定

### 設定ファイル
設定ファイルは`.minecraft/config/skjmod.json`に自動作成されます。

```json
{
  "discordWebhookUrl": "https://discord.com/api/webhooks/...",
  "enableNotification": true,
  "enableDebugLog": false,
  "enabledBombTypes": [
    "COMBAT_XP",
    "PROFESSION_XP", 
    "PROFESSION_SPEED",
    "DUNGEON",
    "LOOT",
    "LOOT_CHEST"
  ]
}
```

### 設定項目
- `discordWebhookUrl`: Discord Webhook URL（必須）
- `enableNotification`: 通知の有効/無効
- `enableDebugLog`: デバッグログの有効/無効
- `enabledBombTypes`: 通知対象のBomb種別

### Discord Webhook URL取得方法
1. Discordサーバーの設定画面を開く
2. 「連携サービス」→「ウェブフック」を選択
3. 「新しいウェブフック」を作成
4. URLをコピーして設定ファイルに貼り付け

## Discord通知フォーマット

```
💣 Combat Experience Bombが AS1 に投げられました！
プレイヤー: PlayerName
投げられた日時: 2025-08-19 15:30:45
```

各Bomb種別に応じて色分けされたEmbedメッセージで送信されます。

## 開発

### ビルド方法
```bash
./gradlew build
```

### 開発環境セットアップ
```bash
./gradlew runClient
```

## トラブルシューティング

### 通知が送信されない
1. Discord Webhook URLが正しく設定されているか確認
2. `enableNotification`が`true`になっているか確認
3. 対象のBomb種別が`enabledBombTypes`に含まれているか確認

### デバッグログを有効にする
設定ファイルで`enableDebugLog`を`true`に設定すると、詳細なログが出力されます。

## ライセンス

MIT License

## 開発者

**gqrshy** - [GitHub](https://github.com/gqrshy)

## 貢献

バグ報告や機能提案は、GitHubのIssueでお願いします。

## 注意事項

- このModはクライアントサイドModです
- WynnCraftサーバーのチャットメッセージフォーマットに依存しています
- Champion rankプレイヤーのBombbellのみが検知対象です