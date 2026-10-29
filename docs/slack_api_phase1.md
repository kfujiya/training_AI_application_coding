# 1. Phase 1 Slack通知の現状

現在はSlack OAuth連携済みユーザーのSlack DMへ、Slack Web APIで通知する方式です。Incoming Webhook方式は撤去済みで、`SLACK_WEBHOOK_URL`は使用しません。

```yaml
slack:
  oauth:
    client-id: ${SLACK_CLIENT_ID:}
    client-secret: ${SLACK_CLIENT_SECRET:}
    redirect-uri: ${SLACK_REDIRECT_URI:}
    encryption-key: ${SLACK_TOKEN_ENCRYPTION_KEY:}
```

設定箇所：`src/main/resources/application.yml`

通知処理は以下です。

```text
TaskService.create/update
    ↓ DB保存
TransactionSynchronization.afterCommit()
    ↓
SlackNotificationService
    ↓
HttpSlackWebApiClient
    ↓ conversations.open / chat.postMessage
Slack DM
```

関連クラス：

- `src/main/java/com/example/task_manager/service/TaskService.java`
- `src/main/java/com/example/task_manager/service/SlackNotificationService.java`
- `src/main/java/com/example/task_manager/service/HttpSlackWebApiClient.java`

未連携、Token復号失敗、通信失敗、Slack APIエラーの場合は通知をスキップし、タスク処理には影響させない設計です。

## 現在の構成

- `slack_installations`でWorkspace単位の暗号化済みBot Tokenを管理する
- `slack_connections`でアプリユーザーとSlack User、DMチャンネルを管理する
- OAuth stateはセッションに保存し、10分期限・一度限りで検証する
- 通知先はログイン中ユーザーではなく、対象Taskの所有者から解決する

タスク管理の4テーブルに加え、Slack連携用DDLで2テーブルを使用します。

# 2. 関係するファイル・クラス・メソッド

## ユーザー・認証

- entity.User
- repository.UserRepository.findByEmail
- service.UserService.register
- security.DatabaseUserDetailsService.loadUserByUsername
- config.SecurityConfig.securityFilterChain
- controller.AuthController

User.java (src/main/java/com/example/task_manager/entity/User.java)

## タスク所有者管理

- Task.userId
- TaskRepository.findAllByUserId
- TaskRepository.findByTaskIdAndUserId
- TaskService.currentUser
- TaskService.create
- TaskService.update
- TaskService.delete

Task.java (src/main/java/com/example/task_manager/entity/Task.java)

## Slack通知

- TaskService.notifyAfterCommit
- SlackNotificationService.notifyTaskCreated
- SlackNotificationService.notifyTaskUpdated
- SlackNotificationService.notifyTaskCompleted
- SlackWebApiClient.openDirectMessage
- SlackWebApiClient.postMessage

テストは以下です。

- SlackNotificationServiceTest
- TaskServiceSlackNotificationTest

# 3. 推奨するOAuth・DM通知設計

## 推奨方式

Slackの標準OAuth v2による「Slack Appのインストール」方式を使用します。

Slack公式仕様では、OAuthは以下の3段階です。

1. 認可URLへリダイレクト
2. Slackユーザーが承認
3. callbackで認可コードをoauth.v2.accessへ交換

Slack OAuth v2公式仕様

既存アプリのログイン方式は維持し、Slack OAuthは「ログイン」ではなく「Slackアカウント連携」として扱います。Sign in with Slackは使用しません。

## OAuth開始

認証済みユーザーだけが以下のURLへアクセスできるようにします。

GET /settings/slack/connect

処理内容：

1. ログイン中のアプリユーザーを取得
2. 128bit以上のランダムなstateを生成
3. stateとアプリユーザーIDをHTTPセッションへ保存
4. 有効期限を設定
5. Slackの認可URLへリダイレクト

stateはSlack公式にも記載されているCSRF対策です。callbackで一致しない場合は処理を中断します。

## OAuth callback

GET /settings/slack/callback

処理内容：

1. stateをセッション保存値と比較
2. stateを一度使用したら破棄
3. Slackから返されたerrorを確認
4. 認可コードをoauth.v2.accessへ送信
5. okとレスポンス内容を検証
6. Workspace ID、Slack User ID、Bot Tokenを取得
7. 重複連携を確認
8. 暗号化してDB保存
9. 連携完了画面へリダイレクト

SlackのOAuthレスポンスには、team.id、authed_user.id、Bot Tokenなどが含まれます。OAuthレスポンス仕様

## 連携解除

POST /settings/slack/disconnect

- Spring SecurityのCSRF保護を維持
- ログイン中ユーザーの連携だけ削除
- Slack Tokenをログへ出力しない
- 他ユーザーも利用しているWorkspace用Installationは削除・失効させない
- そのWorkspaceに紐づく連携がなくなった場合だけ、Bot Tokenの失効を検討

Slackにはauth.revokeがあり、個別Tokenの失効が可能です。ただし、共有しているWorkspace InstallationのTokenを連携解除時に即時失効すると、他ユーザーの通知も停止するため注意が必要です。auth.revoke公式仕様

# 4. DB設計案と既存DBへの影響

既存4テーブルは変更せず、新規テーブルを追加する案を推奨します。

## slack_installations

Workspace単位のSlack Appインストール情報を管理します。

| カラム | 内容 |
|---|---|
| slack_installation_id | PK |
| workspace_id | Slack Workspace ID |
| enterprise_id | Enterprise Grid利用時のID、NULL可 |
| bot_user_id | Slack AppのBot User ID |
| bot_access_token_encrypted | 暗号化済みBot Token |
| refresh_token_encrypted | Token Rotation利用時のRefresh Token |
| token_expires_at | 有効期限、NULL可 |
| scopes | Slackから付与されたscope |
| created_at | 作成日時 |
| updated_at | 更新日時 |

制約案：

UNIQUE (workspace_id)

単一Workspace内でBot Tokenを重複保存しないためです。

## slack_connections

アプリユーザーとSlackユーザーの連携を管理します。

| カラム | 内容 |
|---|---|
| slack_connection_id | PK |
| user_id | users.user_idへのFK |
| slack_installation_id | InstallationへのFK |
| workspace_id | Slack Workspace ID |
| slack_user_id | Slack User ID |
| dm_channel_id | 取得済みDMチャンネルID、NULL可 |
| status | ACTIVE / REVOKED等 |
| created_at | 作成日時 |
| updated_at | 更新日時 |

制約案：

UNIQUE (user_id)
UNIQUE (workspace_id, slack_user_id)

これにより、以下を保証できます。

- アプリユーザー1人につきSlack連携1件
- 同じWorkspace内の同じSlackアカウントを複数アプリユーザーへ連携不可
- Workspaceが異なる場合のSlack User ID重複は許容

## ユーザー削除時

既存仕様ではユーザー削除機能自体が明確に実装されていません。

将来ユーザー削除を実装する場合は、以下を推奨します。

users削除
    ↓ ON DELETE CASCADE
slack_connections削除
    ↓ 参照がなくなった場合
slack_installationsのToken失効・削除

既存のtasks、tags、task_tagsの削除方針は、既存DB設計と合わせて別途統一する必要があります。

## DBへの影響

新規テーブル、Entity、Repositoryの追加が必要です。

既存の以下は変更しません。

- users
- tasks
- tags
- task_tags
- 既存Task Entityの永続化項目
- 既存タスク所有者チェック

Bot Tokenは平文保存せず、アプリケーション側で暗号化して保存します。暗号化キーは環境変数またはSecrets Managerで管理します。

# 5. Slack App側で必要な設定

## Slack App

1. Slack APIで新規Slack Appを作成
2. OAuth & PermissionsでRedirect URLを登録
3. OAuth v2を利用
4. Token Rotationの有効化を検討
5. Bot Token Scopesに`chat:write`と`im:write`を設定
6. Incoming Webhooksは使用しない

Redirect URLはHTTPSで設定します。Slack公式仕様上、認可時とToken交換時にredirect_uriを指定する場合は同一値である必要があります。OAuth Redirect URL仕様

## 必要なscope

Bot Token方式では、最小限以下を推奨します。

chat:write
im:write

- im:write
  - conversations.openで1対1 DMを開くため
- chat:write
  - chat.postMessageでDMへ投稿するため

conversations.openはBot Tokenの場合、DM作成にim:writeが必要です。conversations.open公式仕様

chat.postMessageはBot Tokenのchat:writeで利用できます。chat.postMessage公式仕様

users:readは、OAuthレスポンスのauthed_user.idを利用する設計なら必須ではありません。

# 6. 環境変数・秘密情報の管理方針

設定候補：

SLACK_CLIENT_ID
SLACK_CLIENT_SECRET
SLACK_REDIRECT_URI
SLACK_TOKEN_ENCRYPTION_KEY

用途：

- SLACK_CLIENT_ID
  - OAuth認可URLとToken交換に使用
- SLACK_CLIENT_SECRET
  - 認可コード交換に使用
- SLACK_REDIRECT_URI
  - OAuth callback URL
- SLACK_TOKEN_ENCRYPTION_KEY
  - DB保存前のToken暗号化・復号に使用

管理方針：

- ソースコード、application.yml、Git管理対象ファイルへ実値を書かない
- ログへToken、Client Secret、認可コードを出力しない
- APIエラー時もレスポンス全体をそのままログ出力しない
- DBのTokenカラムは暗号化済み文字列のみ保存
- 本番では環境変数よりSecrets Manager等を優先
- Token復号はSlack API呼び出し直前だけに限定
- Slack API通信はHTTPSのみ

Slack公式もTokenを秘密情報として安全に保存することを要求しています。Slack Token公式仕様

`SLACK_WEBHOOK_URL`は現在の実装では使用しません。秘密情報は環境変数またはSecrets Managerで管理します。

# 7. 実装手順と変更対象ファイル

## 推奨する追加ファイル

entity/
  SlackInstallation.java
  SlackConnection.java

repository/
  SlackInstallationRepository.java
  SlackConnectionRepository.java

service/
  SlackOAuthService.java
  HttpSlackWebApiClient.java
  SlackTokenEncryptionService.java

client/
  SlackOAuthClient.java
  SlackWebApiClient.java

controller/
  SlackIntegrationController.java

dto/
  SlackOAuthAccessResponse.java
  SlackApiResponse.java

## 既存ファイルの変更

- TaskService
  - 通知対象ユーザーのuser_idを通知サービスへ渡す
  - ログイン中ユーザーではなく、保存したTaskの所有者を基準にする
- SlackNotificationService
  - Slack Web APIによるDM通知を使用
  - SlackConnectionRepositoryからTask所有者の連携情報を取得
  - DMチャンネルを解決してWeb APIで投稿
- application.yml
  - OAuth設定を環境変数参照で追加
- SecurityConfig
  - Slack連携画面は認証必須のまま
  - callbackはOAuthフロー上必要な公開エンドポイントとして設計し、state検証で保護
- fragments.htmlまたは新規設定画面
  - Slack連携状態
  - Slack連携ボタン
  - 連携解除ボタン

## DM送信フロー

TaskService.afterCommit()
    ↓ Task.userId
SlackNotificationService
    ↓ SlackConnectionRepository.findByUserId()
SlackConnection
    ↓ encrypted Bot Token
SlackWebApiClient
    ↓ conversations.open(users=slack_user_id)
dm_channel_idを保存
    ↓
chat.postMessage(channel=dm_channel_id, text=...)

Slack公式仕様では、conversations.openに対象ユーザーIDを指定してDMを開き、そのレスポンスのDMチャンネルIDをchat.postMessageへ渡します。conversations.open公式仕様

通知先はAuthentication.getName()ではなく、保存されたTaskのuserIdから決定します。

これにより、管理者処理や将来の非同期処理でも、ログイン中ユーザーとタスク所有者を取り違えません。

## API応答の検証

Slack Web APIはHTTP 200でもJSONのok: falseを返す可能性があります。そのため、以下を両方確認します。

- HTTPステータス
- JSONのok
- error
- 必要に応じてRetry-After

現在の実装はHTTPステータスとJSONの`ok`を確認します。

## 通知失敗時

- 例外をTaskServiceへ伝播させない
- タスクのDB保存成功を維持
- TokenやPayloadをログに出さない
- invalid_auth、token_revokedなどは連携状態をREVOKEDへ変更
- 未連携・連携解除済みは通知をスキップ
- channel_not_found / invalid_channelでDM IDが無効な場合は、保存済みDM IDを破棄して再度conversations.openを実行
- Phase 2対象外のため、複雑なリトライキューや通知履歴テーブルは追加しない

# 8. テストケースと移行リスク

## OAuth

- 未ログインユーザーが連携開始URLへアクセスできない
- state一致時だけcallbackを処理する
- state不一致で連携されない
- state再利用で連携されない
- OAuth拒否時にDBが変更されない
- 認可コード交換失敗時にDBが変更されない
- Tokenをログへ出さない
- Workspace IDとSlack User IDを正しく保存する
- 同一アプリユーザーの再連携時に既存連携を更新できる
- 同じworkspace_id + slack_user_idを別アプリユーザーへ連携できない

## 連携解除

- 他ユーザーの連携を解除できない
- 連携解除後に通知されない
- 共有Workspaceの他ユーザー通知が停止しない
- 共有InstallationのToken失効・削除は連携解除時に行わない

## 通知

- タスク登録後、コミット成功時にDM通知される
- タスク更新後にDM通知される
- DONE変更時に完了通知される
- ロールバック時に通知されない
- 未連携ユーザーのTask操作が成功する
- APIのok:falseでTask操作が失敗しない
- HTTP timeoutでTask操作が失敗しない
- invalid_authで連携状態がREVOKEDになる
- DMチャンネルIDを再利用できる
- DMチャンネルID無効時に再取得できる
- タスク所有者のSlackへ通知され、操作実行者のSlackへ誤送信されない

## 移行リスク

1. Bot Token漏洩
   - DB暗号化、ログマスキング、Secrets管理で軽減
2. OAuth state不備
   - セッション保存、ワンタイム利用、有効期限で軽減
3. Workspace IDとSlack User IDの取り違え
   - 複合一意制約とレスポンス検証で防止
4. 共有WorkspaceのToken失効
   - InstallationとConnectionを分離し、参照中は失効させない
5. Slack APIのHTTP 200 / ok:false
   - HTTPとJSON両方の検証で対応
6. Slack APIのレート制限
   - Phase 1では通知キューを追加せず、失敗をログ記録してタスク処理を優先
7. 旧Webhookとの二重通知
   - 移行中は切替フラグまたは段階的切替を用い、新旧を同時に有効化しない
8. 未連携ユーザーへの影響
   - 連携情報がない場合は通知だけスキップし、Taskのトランザクションを成功させる

## 実装・移行状況

1. Slack連携テーブル、Entity、Repositoryを追加済み
2. OAuth開始・callback・state検証・連携解除を実装済み
3. Slack Web APIによるDM通知へ移行済み
4. Incoming Webhook実装、`SLACK_WEBHOOK_URL`、専用テストを削除済み
5. 通知失敗時の安全性と所有者宛先をテスト済み
## 実装状況（第1段階）

以下を実装済みです。

- `slack_installations` / `slack_connections` の追加DDL、Entity、Repository
- `/settings/slack` の連携状態表示、OAuth開始、callback、連携解除
- セッション保存・10分期限・一度限り消費のOAuth state検証
- Bot TokenのAES-GCM暗号化保存（`SLACK_TOKEN_ENCRYPTION_KEY`を使用）
- OAuth scopesは `chat:write,im:write`
- Slack Web API DM通知を実装し、Incoming Webhook通知は使用しない

DDLは `src/main/resources/db/slack_integration.sql` にあります。現行プロジェクトはFlyway等を導入しておらず、`ddl-auto: validate`運用のため、初回のみ管理者が既存の `task_manager` DBへこのDDLを適用してください。DDLは既存4テーブルを変更しません。

Token Rotationは今回実装していません。Refresh Tokenや有効期限を部分的に扱わず、Slack App側で発行されたBot Tokenを暗号化して保存する方式です。将来Rotationを採用する場合は、refresh token、期限、更新失敗時の再連携まで一貫して追加してください。

### 必要な環境変数

```text
SLACK_CLIENT_ID
SLACK_CLIENT_SECRET
SLACK_REDIRECT_URI=https://<host>/settings/slack/callback
SLACK_TOKEN_ENCRYPTION_KEY=<十分な長さの秘密値>
```

### Slack App側の設定

Slack API管理画面でOAuth Redirect URLに `SLACK_REDIRECT_URI` と同じURLを登録し、Bot Token Scopesに `chat:write` と `im:write` を追加してください。開発環境と本番環境でRedirect URL・Client Secret・暗号化キーを分離し、秘密値は環境変数またはSecret Managerで管理します。アプリの再インストールや権限変更が必要な場合は、Slack管理画面から実施してください。
## Slack Web API DM通知の実装結果

タスク登録・更新・完了の通知を、Incoming WebhookからSlack Web APIのDM通知へ切り替えました。通知先はログイン中ユーザーではなく、`Task.userId`に対応する`SlackConnection`です。

- `conversations.open`で所有者のSlack User ID向けDMを取得し、`dm_channel_id`へ保存
- 保存済みDM IDを再利用し、`channel_not_found`または`invalid_channel`の場合だけ再取得
- `chat.postMessage`でタスク名、表示名変換済みのステータス・優先度、期限、タグを送信
- 更新時はタスク名、説明、ステータス、優先度、期限、タグの変更項目だけを通知
- `DONE`への遷移は完了専用通知とし、変更のない更新は通知しない
- Slack APIのHTTPステータスとJSON `ok`を検証
- 未連携、Token復号失敗、通信失敗、APIエラーは通知のみスキップ
- `invalid_auth` / `token_revoked`だけ対象Connectionを`REVOKED`へ更新
- TaskServiceのDBコミット後通知と既存トランザクション方針を維持

旧Incoming Webhook実装、`SLACK_WEBHOOK_URL`設定、Webhook専用テストは削除済みです。Token Rotationは引き続き未実装です。
