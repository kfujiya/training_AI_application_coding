# QUIZ ARENA

Open Trivia DBを利用するクイズゲームの開発プロジェクトです。現在はPhase 2（クイズ取得・回答・結果表示）まで実装しています。

## 使用技術

- Frontend: React / TypeScript / Vite
- Backend: Java 21 / Spring Boot 3.4 / Gradle Wrapper
- Database: PostgreSQL（専用DB `quiz_arena_db`）
- Migration: Flyway

## ディレクトリ構成

```text
frontend/   React + Vite
backend/    Spring Boot + Gradle
```

## 起動方法

### バックエンド（DBなしで起動）

Java 21を用意したうえで、`backend` から実行します。`local`プロファイルではDB接続を行わないため、PostgreSQL未導入でも起動できます。

```powershell
cd backend
.\gradlew.bat bootRun
```

バックエンドは `http://127.0.0.1:8081` で起動します。ヘルスチェックは `GET /api/health` です。

### フロントエンド

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

フロントエンドは `http://127.0.0.1:5173` で起動し、`/api`へのアクセスをバックエンドへプロキシします。

## PostgreSQL接続（任意）

既存環境を変更せず、クイズゲーム専用のDBとユーザーを用意してください。接続情報は環境変数で指定します。

```powershell
$env:SPRING_PROFILES_ACTIVE = 'db'
$env:DB_URL = 'jdbc:postgresql://127.0.0.1:5432/quiz_arena_db'
$env:DB_USERNAME = 'quiz_arena_app'
$env:DB_PASSWORD = 'your-password'
.\gradlew.bat bootRun
```

`db`プロファイルではFlywayが`src/main/resources/db/migration`のマイグレーションを実行します。認証情報はソースコードへ記載しないでください。

## テスト

```powershell
cd backend
.\gradlew.bat test
cd ..\frontend
npm.cmd run build
```

## Phase 2の操作方法

1. フロントエンドを起動してホーム画面で「ゲームを開始する」を選択します。
2. 問題数（5・10・20問）、カテゴリ、難易度を設定します。
3. 選択肢を押して回答し、「次の問題へ」で進みます。
4. 全問回答後、バックエンドで計算された正解数・不正解数・正答率を確認します。

## Phase 2 API

| Method | Path | 内容 |
|---|---|---|
| GET | `/api/categories` | Open Trivia DBのカテゴリ一覧（1時間キャッシュ） |
| POST | `/api/quizzes` | 問題をまとめて取得し、クイズセッションを開始 |
| POST | `/api/quizzes/{quizId}/answers` | 回答を判定し、次の問題または結果を返却 |

クイズ開始リクエスト例：

```json
{ "amount": 5, "category": 9, "difficulty": "easy" }
```

`category`と`difficulty`は`null`にできます。開始レスポンスと次の問題情報には正解情報を含めず、回答APIのレスポンスでのみ正解を返します。
外部APIへのリクエストはバックエンド側で管理し、Open Trivia DBのリクエスト間隔制限（5秒）を守ります。

## Phase 1の確認用API

```text
GET http://127.0.0.1:8081/api/health
```

成功時は`status: UP`を含むJSONを返します。

## Open Trivia DB

クイズ問題はOpen Trivia DBから取得する予定です。問題データを利用する際は、同サービスのライセンスと帰属表示（CC BY-SA 4.0）に従います。
