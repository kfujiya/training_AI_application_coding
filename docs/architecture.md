# Architecture

## 1. 概要

本アプリケーションは、Java 21 / Spring Bootを使用したWebアプリケーションとして構築する。

画面表示にはThymeleafを使用し、データベースにはPostgreSQLを使用する。

認証・認可にはSpring Securityを使用する。

アプリケーションはレイヤードアーキテクチャを基本とし、Controller、Service、Repositoryの責務を分離する。

---

# 2. 技術構成

| 分類         | 技術                          |
| ---------- | --------------------------- |
| 言語         | Java 21                     |
| ビルド        | Maven                       |
| Webフレームワーク | Spring Boot                 |
| Web MVC    | Spring MVC                  |
| 認証・認可      | Spring Security             |
| テンプレート     | Thymeleaf                   |
| ORM        | Spring Data JPA / Hibernate |
| データベース     | PostgreSQL                  |
| DB Driver  | PostgreSQL JDBC Driver      |
| 開発環境       | Visual Studio Code          |

---

# 3. 全体構成

基本的な処理の流れは以下とする。

```text
Browser
   ↓
Spring Security
   ↓
Controller
   ↓
Service
   ↓
Repository
   ↓
JPA / Hibernate
   ↓
PostgreSQL
```

画面表示にはThymeleafを使用する。

```text
Controller
   ↓
Thymeleaf
   ↓
HTML
   ↓
Browser
```

---

# 4. レイヤー構成

## 4.1 Controller

HTTPリクエストを受け付け、適切なServiceを呼び出す。

Controllerには業務ロジックや直接的なDBアクセス処理を記述しない。

主な責務：

* HTTPリクエストの受付
* 入力値の受け取り
* バリデーション結果の処理
* Serviceの呼び出し
* Viewへのデータ受け渡し
* 画面遷移

---

## 4.2 Service

アプリケーションの業務ロジックを担当する。

主な責務：

* タスクに関する業務処理
* タグに関する業務処理
* ユーザーに関する業務処理
* データ所有者の確認
* 複数Repositoryをまたぐ処理
* トランザクション境界の管理

Controllerから直接Repositoryを操作せず、原則としてServiceを経由する。

---

## 4.3 Repository

データベースへのアクセスを担当する。

Spring Data JPAのRepositoryを使用する。

主な責務：

* Entityの検索
* Entityの登録
* Entityの更新
* Entityの削除
* 必要な検索条件によるデータ取得

業務ロジックはRepositoryに記述しない。

---

# 5. Entity

PostgreSQLのテーブルに対応するEntityを作成する。

初期バージョンでは以下のEntityを作成する。

```text
User
Task
Tag
TaskTag
```

Entityとテーブルの対応は以下とする。

| Entity    | Table       |
| --------- | ----------- |
| `User`    | `users`     |
| `Task`    | `tasks`     |
| `Tag`     | `tags`      |
| `TaskTag` | `task_tags` |

EntityにはDB上のデータを表現するための情報を持たせる。

ControllerからEntityを直接画面表示用データとして使用することは避ける。

---

# 6. DTO / Form

画面から受け取る入力値や画面表示用データについては、必要に応じてDTOまたはFormクラスを使用する。

特に以下についてはEntityを直接HTTPリクエストにバインドしない。

* ユーザー登録
* ログイン
* タスク登録
* タスク編集
* タグ登録
* タグ編集

これにより、HTTPリクエストからEntityの意図しない項目が変更されることを防ぐ。

---

# 7. Validation

入力値の検証にはJakarta Bean Validationを使用する。

例えば、

* `@NotBlank`
* `@Size`
* `@Email`
* `@NotNull`

などを使用する。

Controllerで入力を受け取り、Serviceで業務上必要な追加チェックを行う。

DBの制約だけに依存せず、ユーザーにとって分かりやすいエラーメッセージを画面に表示する。

---

# 8. Spring Security

Spring Securityを使用して認証・認可を実装する。

## 8.1 認証

初期バージョンではメールアドレスとパスワードによるフォームログインを使用する。

認証情報は `users` テーブルから取得する。

パスワードはPasswordEncoderを使用してハッシュ化する。

平文パスワードをDBに保存しない。

---

## 8.2 認可

タスクおよびタグはユーザー単位で管理する。

ログインしているだけではなく、対象データの所有者であることをService側でも確認する。

例えばタスク取得時は、

```text
現在ログインしているユーザー
        ↓
user_idを取得
        ↓
そのuser_idに属するTaskだけ取得
```

という方式を基本とする。

URLパラメータの `task_id` を変更するだけで他ユーザーのタスクを取得できるような実装は禁止する。

---

# 9. ログインユーザー情報

ログイン中のユーザー情報はSpring Securityの認証情報から取得する。

ControllerやServiceでユーザーIDをHTTPリクエストパラメータから受け取って所有者として扱わない。

例えば、

```text
GET /tasks/10
```

の `10` はタスクIDであり、所有者を指定する情報ではない。

所有者は現在認証されているユーザーから決定する。

---

# 10. Transaction

DB更新処理にはSpringのトランザクション管理を使用する。

複数のDB操作を一つの処理として扱う必要がある場合は、Service層をトランザクション境界とする。

例えばタスク削除時に関連する `task_tags` を削除する処理などは、必要に応じて同一トランザクションで処理する。

Repository側で個別にトランザクションを管理するのではなく、原則としてService側で管理する。

---

# 11. Thymeleaf

画面表示にはThymeleafを使用する。

テンプレートは、

```text
src/main/resources/templates/
```

以下に配置する。

静的リソースは、

```text
src/main/resources/static/
```

以下に配置する。

想定する構成例：

```text
src/main/resources/
├── templates/
│   ├── login.html
│   ├── register.html
│   ├── tasks/
│   │   ├── list.html
│   │   ├── form.html
│   │   └── detail.html
│   └── tags/
│       └── list.html
└── static/
    ├── css/
    └── js/
```

具体的な画面構成は実装時に調整する。

---

# 12. URL設計

初期バージョンでは以下のようなURL構成を基本とする。

| URL                  | HTTP Method | 内容       | 認証 |
| -------------------- | ----------- | -------- | -- |
| `/login`             | GET         | ログイン画面   | 不要 |
| `/login`             | POST        | ログイン処理   | 不要 |
| `/register`          | GET         | ユーザー登録画面 | 不要 |
| `/register`          | POST        | ユーザー登録   | 不要 |
| `/logout`            | POST        | ログアウト    | 必須 |
| `/tasks`             | GET         | タスク一覧    | 必須 |
| `/tasks/new`         | GET         | タスク登録画面  | 必須 |
| `/tasks`             | POST        | タスク登録    | 必須 |
| `/tasks/{id}/edit`   | GET         | タスク編集画面  | 必須 |
| `/tasks/{id}`        | POST        | タスク更新    | 必須 |
| `/tasks/{id}/delete` | POST        | タスク削除    | 必須 |
| `/tags`              | GET         | タグ一覧     | 必須 |
| `/tags`              | POST        | タグ登録     | 必須 |

実際のURLは実装時に一貫性を保つことを優先して調整する。

---

# 13. DBアクセス

DBアクセスにはSpring Data JPAを使用する。

原則としてControllerから直接EntityManagerやRepositoryを呼び出さない。

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
JPA
```

の流れを維持する。

---

# 14. データ所有者チェック

タスクやタグを操作する場合、対象データの所有者を必ず確認する。

悪い例：

```text
taskRepository.findById(taskId)
```

で取得したタスクを、そのまま現在のユーザーが操作できるものとして扱う。

推奨例：

```text
現在のユーザーID
      +
対象タスクID
      ↓
Repository
      ↓
所有者が一致するタスクのみ取得
```

これにより、他ユーザーのデータへのアクセスを防止する。

---

# 15. エラーハンドリング

アプリケーション内部の例外をユーザーへそのまま表示しない。

画面上ではユーザーが理解できるエラーメッセージを表示する。

想定するエラー：

* 入力値エラー
* 認証エラー
* 認可エラー
* 対象データ不存在
* DBエラー
* 予期しないシステムエラー

必要に応じてSpring MVCの例外ハンドリング機能を使用する。

---

# 16. ログ

アプリケーションログにはSLF4J / Spring Bootの標準ログ機能を使用する。

パスワードなどの機密情報をログへ出力しない。

DB接続パスワードやセッション情報などの機密情報もログへ出力しない。

---

# 17. 設定管理

Spring Bootの設定は `application.yml` を基本とする。

例：

```text
src/main/resources/application.yml
```

DB接続情報などの機密情報をソースコードへ直接記述しない。

ローカル開発環境では環境変数などから取得できる構成を採用する。

---

# 18. データベースマイグレーション

DB構造はアプリケーションのEntity生成に任せず、明示的に管理する。

初期段階ではPostgreSQL上に手動でテーブルを作成してもよい。

将来的にはFlywayなどのデータベースマイグレーションツールの導入を検討する。

Hibernateの自動DDL生成に依存しない。

本番環境では自動的なテーブル削除や再作成を行わない。

---

# 19. REST API

初期バージョンではThymeleafによるWeb画面を主とする。

ただし、将来的なAPI利用を考慮してService層とWeb層を分離する。

将来的には、

```text
Browser
   ↓
REST Controller
   ↓
Service
   ↓
Repository
```

というAPI構成を追加できるようにする。

REST APIの追加時も、Controllerに業務ロジックを実装せずServiceを利用する。

---

# 20. Slack API連携

Slack OAuth連携とSlack Web API通知を、タスク管理処理から分離した専用Service・Clientで実装する。

Slack API連携処理は既存のタスク管理処理と分離する。

構成：

```text
TaskService
    ↓（DBコミット後）
SlackNotificationService
    ↓
SlackWebApiClient
    ↓ conversations.open / chat.postMessage
Slack DM
```

OAuthの開始・callback・連携解除は`SlackIntegrationController`と`SlackOAuthService`が担当する。OAuth stateは`SlackOAuthStateService`で生成し、セッションに10分間保存してcallbackで一度だけ検証する。

Bot Tokenは`SlackTokenEncryptionService`で暗号化して保存し、API呼び出し直前に復号する。OAuth情報と暗号化キーは環境変数から取得する。

---

# 21. パッケージ構成

パッケージは責務ごとに分割する。

例：

```text
com.example.task_manager
├── config
├── controller
├── service
├── repository
├── entity
├── dto
├── form
├── security
└── exception
```

各パッケージの役割：

| パッケージ        | 役割                |
| ------------ | ----------------- |
| `config`     | Spring等の設定        |
| `controller` | HTTPリクエスト処理       |
| `service`    | 業務ロジック            |
| `repository` | DBアクセス            |
| `entity`     | DB Entity         |
| `dto`        | データ転送用オブジェクト      |
| `form`       | 画面入力用オブジェクト       |
| `security`   | Spring Security関連 |
| `exception`  | 独自例外・例外処理         |

必要のないクラスやパッケージを無理に作成しない。

---

# 22. テスト

初期バージョンでは以下のテストを実装する。

## Unit Test

Serviceなどの業務ロジックを中心にテストする。

## Repository Test

DBアクセス処理について必要なテストを行う。

## Integration Test

Spring BootのApplicationContext、Controller、DBなどを組み合わせたテストを必要に応じて実施する。

特に以下を確認する。

* ログインできる
* 未認証ユーザーが保護された画面へアクセスできない
* 自分のタスクを取得できる
* 他ユーザーのタスクを取得できない
* 自分のタスクを編集できる
* 他ユーザーのタスクを編集できない
* 自分のタスクを削除できる
* 他ユーザーのタスクを削除できない

---

# 23. 実装フェーズ

開発は以下のフェーズに分割する。

## Phase 1: 基盤

* Spring Boot起動確認
* PostgreSQL接続
* 基本設定
* パッケージ構成

## Phase 2: DB / Entity

* Entity作成
* Repository作成
* DBテーブル作成
* DBとの基本的なCRUD確認

## Phase 3: ユーザー / 認証

* User Entity
* ユーザー登録
* PasswordEncoder
* Spring Security設定
* ログイン
* ログアウト

## Phase 4: タスク

* Task Entity
* Task Repository
* タスク登録
* タスク一覧
* タスク編集
* タスク削除
* 所有者チェック

## Phase 5: タグ

* Tag Entity
* TaskTag Entity
* タグCRUD
* タスクへのタグ付与
* タグによる絞り込み

## Phase 6: UI / Validation

### UI

Thymeleafによる各画面のUIを整備し、タスク管理アプリケーションとして統一感のある外観にする。

Bootstrapを導入し、基本的なレイアウト、フォーム、ボタン、ナビゲーション、カード、バッジ、アラート等のUIにBootstrapのコンポーネントを利用する。

対象画面：

* ログイン
* ユーザー登録
* タスク一覧
* タスク登録
* タスク詳細
* タスク編集
* タグ管理

以下を満たすこと。

* 共通のヘッダー・ナビゲーションを設ける
* 各画面で統一されたレイアウト・UIを使用する
* タスクのステータス、優先度、期限、タグを視認しやすく表示する
* タスク一覧は単純なテーブルだけにせず、カード等も活用して視認性を高める
* 入力フォームを整理し、操作しやすくする
* 成功・エラー等のメッセージを視認しやすく表示する
* PCおよび一般的なスマートフォン画面で利用しやすいレスポンシブなレイアウトとする
* Bootstrapで対応できる部分はBootstrapを優先し、独自CSSは必要最小限とする

### Validation

ユーザー登録、タスク登録・編集、タグ登録・編集などの入力値を検証する。

入力エラーが発生した場合は、ユーザーが原因を理解できるメッセージを画面上に表示する。

### エラーメッセージ

以下のようなエラーを適切に画面表示する。

* 必須項目未入力
* 最大文字数超過
* 不正な形式
* ログイン失敗
* 存在しないデータへのアクセス
* 他ユーザーのデータへのアクセス
* その他の業務上想定されるエラー

### 検索・絞り込み

タスク一覧で以下の条件による検索・絞り込みを可能にする。

* タスクタイトル
* ステータス
* 優先度
* タグ
* 期限

検索・絞り込みはログインユーザー自身のタスクのみを対象とする。

## Phase 7: テスト

* Unit Test
* Repository Test
* Integration Test
* 認証・認可テスト

## Phase 8: 将来拡張

* REST API
* メール通知
* 定期通知・期限通知

Slack OAuth連携とタスク通知は実装済みであり、Phase 8の対象外とする。

---

# 24. 実装上の原則

以下の原則を維持する。

1. Controllerに業務ロジックを記述しない。
2. Controllerから直接DBアクセスしない。
3. Serviceに業務ロジックを集約する。
4. RepositoryはDBアクセスに責務を限定する。
5. EntityをHTTPリクエストへ直接バインドしない。
6. ユーザー所有データについて、必ずサーバー側で所有者を確認する。
7. パスワードを平文で保存しない。
8. DB構造をHibernateの自動生成に依存しない。
9. 機密情報をソースコードやログへ出力しない。
10. REST APIやSlack連携を含む外部連携を考慮し、Service層とWeb層を分離する。
11. 要件にない機能を独断で追加しない。
12. 既存の設計を変更する場合は、変更理由を明示する。

---

# 26. Slack通知設計

`TaskService`はタスク保存処理を担当し、`SlackNotificationService`は通知内容の生成とSlack API呼び出しを担当する。タスク所有者の`user_id`から`SlackConnection`と`SlackInstallation`を解決するため、ログイン中ユーザーへ誤送信しない。

タスクの登録・更新処理は既存の`@Transactional`境界を維持し、`TransactionSynchronizationManager`のコミット後コールバックで通知する。通知処理の例外は通知サービス内で処理し、タスク処理へ伝播させない。

HTTP通信はJava 21標準の`java.net.http.HttpClient`を使用する。`conversations.open`でDMチャンネルを取得し、`dm_channel_id`へ保存して再利用する。保存済みチャンネルが無効な場合は再取得する。HTTPステータスとレスポンスJSONの`ok`を両方確認する。

通知の種類は以下とする。

* タスク登録
* タスク更新
* タスク完了（`DONE`への状態変更）

通信失敗、Token復号失敗、未連携ユーザー、Slack APIエラーは通知のみをスキップする。`invalid_auth`と`token_revoked`の場合のみ対象連携を`REVOKED`へ更新する。Token、Client Secret、認可コード、暗号化キー、APIリクエスト内容はログへ出力しない。

## Slack設定

以下の環境変数を使用する。

* `SLACK_CLIENT_ID`
* `SLACK_CLIENT_SECRET`
* `SLACK_REDIRECT_URI`
* `SLACK_TOKEN_ENCRYPTION_KEY`

Slack AppのOAuth Redirect URLには`SLACK_REDIRECT_URI`と同じURLを登録し、Bot Token Scopesには`chat:write`と`im:write`を設定する。Incoming Webhook方式および`SLACK_WEBHOOK_URL`は現在の通知処理では使用しない。


---

# 25. 参照ドキュメント

実装時は以下のドキュメントを仕様として扱う。

```text
docs/
├── requirements.md
├── architecture.md
└── database.md
```

* `requirements.md`：機能要件・非機能要件
* `architecture.md`：アプリケーション構成・実装方針
* `database.md`：DB構造・テーブル定義

実装上の判断が必要になった場合は、まずこれらのドキュメントとの整合性を確認する。
## Slack通知 Phase 1 第2段階

`TaskService`は既存のコミット後コールバックを維持し、`SlackNotificationService`へ通知を委譲する。通知サービスはタスク所有者の`SlackConnection`からWorkspaceの`SlackInstallation`を解決し、暗号化Bot Tokenを復号して`conversations.open`と`chat.postMessage`を呼び出す。DMチャンネルIDは`slack_connections.dm_channel_id`へ保存して再利用する。

Slack APIのHTTPステータスとJSONの`ok`を検証し、通信失敗・APIエラー・Token復号失敗はログ記録後に通知をスキップする。`invalid_auth`と`token_revoked`の場合だけ対象Connectionを`REVOKED`に更新し、その他のAPIエラーでは連携を無効化しない。認証情報やTokenはログへ出力しない。
