# Task Manager

Spring Boot、Thymeleaf、Spring Security、PostgreSQLで構成したタスク管理Webアプリケーションです。

## 主な機能

- ユーザー登録、ログイン、ログアウト
- タスクの登録、一覧、詳細、編集、削除
- ステータス、優先度、期限、説明の管理
- タグの登録・編集・削除、タスクへの付与・解除
- タイトル、ステータス、優先度、期限、タグによる絞り込み
- ダッシュボードの集計表示と優先タスク表示
- Slack OAuth連携とSlack Web APIによるDM通知

タスクとタグはユーザー単位で管理され、サーバー側で所有者を確認します。

## 技術構成

- Java 21
- Spring Boot 4.1.1
- Spring Security
- Spring Data JPA / Hibernate
- Thymeleaf / Bootstrap
- PostgreSQL
- Maven

## 前提環境

- JDK 21
- Maven 3.9以降またはMaven Wrapper
- PostgreSQL

デフォルトのDB接続先は以下です。必要に応じて`src/main/resources/application.yml`を環境に合わせて設定してください。

```text
Host: localhost
Port: 5433
Database: task_manager
User: postgres
Password: postgres
```

## データベース準備

1. `task_manager`データベースを作成します。
2. `src/main/resources/db/schema.sql`を実行します。
3. Slack連携を利用する場合は、`src/main/resources/db/slack_integration.sql`を実行します。

アプリケーションは`spring.jpa.hibernate.ddl-auto=validate`で起動するため、起動時にテーブルを作成・変更しません。

## 環境変数

Slack連携を利用する場合は、以下を設定します。

```text
SLACK_CLIENT_ID=<Slack AppのClient ID>
SLACK_CLIENT_SECRET=<Slack AppのClient Secret>
SLACK_REDIRECT_URI=http://localhost:8080/settings/slack/callback
SLACK_TOKEN_ENCRYPTION_KEY=<Token暗号化用の秘密鍵>
```

`SLACK_TOKEN_ENCRYPTION_KEY`は実値をソースコードやGit管理対象ファイルへ記載しないでください。Slack AppのOAuth Redirect URLには、`SLACK_REDIRECT_URI`と同じURLを登録します。Bot Token Scopesには`chat:write`と`im:write`が必要です。

Slack未連携またはSlack設定未完了の場合も、タスク管理機能は利用できます。Slack通知の失敗はタスクの登録・更新結果に影響しません。

## 起動方法

プロジェクトルートで実行します。

```bash
mvn spring-boot:run
```

起動後、ブラウザで以下へアクセスします。

```text
http://localhost:8080
```

## テスト

```bash
mvn test
```

Slack APIの実通信は通常のテストでは行わず、モックを使用します。

## デモ用サンプルデータ

`docs/demo_sample_data.sql`に、`user_id = 8`向けのサンプルタスク、タグ、関連データを用意しています。

```bash
psql -d task_manager -f docs/demo_sample_data.sql
```

対象ユーザーが存在しない場合は投入を中止します。同じSQLを再実行しても、同名のデモタスク・タグ・関連付けは重複登録されません。

## Slack通知の概要

タスクの登録・更新・完了後、DBコミット成功時にタスク所有者のSlack DMへ通知します。

- 登録：タスク名、ステータス、優先度、期限、タグ
- 更新：変更された項目のみを変更前後で表示
- 完了：`【タスク完了！】`の専用通知
- 期限未設定：`未設定`
- タグ未設定：`なし`

Incoming Webhook方式は使用せず、Slack OAuth連携とSlack Web APIを利用します。

## ドキュメント

- [要件定義](docs/requirements.md)
- [アーキテクチャ](docs/architecture.md)
- [データベース設計](docs/database.md)
- [Slack API連携設計・実装状況](docs/slack_api_phase1.md)
- [プロダクト概要・Codex実施内容](docs/product_overview.md)
