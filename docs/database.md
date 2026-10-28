# Database Design

## 1. 概要

タスク管理Webアプリケーションで使用するPostgreSQLデータベースの設計を定義する。

データベース名は `task_manager` とする。

タスク管理機能では以下の4テーブルを使用する。

| テーブル        | 役割                     |
| ----------- | ---------------------- |
| `users`     | アプリケーションを利用するユーザーを管理する |
| `tasks`     | ユーザーが登録するタスクを管理する      |
| `tags`      | タスクに付与するタグを管理する        |
| `task_tags` | タスクとタグの多対多の関連を管理する     |

Slack OAuth連携を利用するため、別DDL `src/main/resources/db/slack_integration.sql` で`slack_installations`と`slack_connections`を追加する。既存の4テーブルは変更しない。

---

## 2. ER構成

```text
users
  │
  │ 1
  │
  │ N
tasks
  │
  │ N
  │
  │ N
task_tags
  │
  │ N
  │
  │ 1
tags
```

関係は以下のとおり。

* `users` 1 : N `tasks`
* `tasks` N : N `tags`
* `tasks` 1 : N `task_tags`
* `tags` 1 : N `task_tags`

Slack連携は以下の関係を持つ。

* `users` 1 : 0..1 `slack_connections`
* `slack_installations` 1 : N `slack_connections`

1人のユーザーが複数のタスクを登録できる。

1つのタスクには複数のタグを付与できる。

1つのタグを複数のタスクに付与できる。

---

# 3. users

## 3.1 役割

アプリケーションを利用するユーザーを管理する。

タスクの所有者を `tasks.user_id` から参照する。

## 3.2 カラム

| カラム             | PostgreSQL型    | NULL | 制約     | 説明                |
| --------------- | -------------- | ---: | ------ | ----------------- |
| `user_id`       | `BIGSERIAL`    |   NO | PK     | ユーザーID            |
| `username`      | `VARCHAR(50)`  |   NO | UNIQUE | ログイン・表示に使用するユーザー名 |
| `email`         | `VARCHAR(255)` |   NO | UNIQUE | メールアドレス           |
| `password_hash` | `VARCHAR(255)` |   NO | -      | ハッシュ化したパスワード      |
| `display_name`  | `VARCHAR(100)` |   NO | -      | 画面上に表示する名前        |
| `created_at`    | `TIMESTAMP`    |   NO | -      | 作成日時              |
| `updated_at`    | `TIMESTAMP`    |   NO | -      | 更新日時              |

## 3.3 制約

* `user_id` を主キーとする。
* `username` は一意でなければならない。
* `email` は一意でなければならない。
* パスワードの平文は保存しない。
* `password_hash` にはハッシュ化済みの値のみ保存する。

---

# 4. tasks

## 4.1 役割

ユーザーが登録するタスクを管理する。

各タスクは必ず1人のユーザーに所属する。

## 4.2 カラム

| カラム           | PostgreSQL型    | NULL | 制約 | 説明             |
| ------------- | -------------- | ---: | -- | -------------- |
| `task_id`     | `BIGSERIAL`    |   NO | PK | タスクID          |
| `user_id`     | `BIGINT`       |   NO | FK | タスクを所有するユーザーID |
| `title`       | `VARCHAR(200)` |   NO | -  | タスク名           |
| `description` | `TEXT`         |  YES | -  | タスクの詳細         |
| `status`      | `VARCHAR(20)`  |   NO | -  | タスクの状態         |
| `priority`    | `VARCHAR(20)`  |   NO | -  | 優先度            |
| `due_date`    | `DATE`         |  YES | -  | 期限             |
| `created_at`  | `TIMESTAMP`    |   NO | -  | 作成日時           |
| `updated_at`  | `TIMESTAMP`    |   NO | -  | 更新日時           |

## 4.3 status

`status` には以下の値を使用する。

| 値             | 意味  |
| ------------- | --- |
| `TODO`        | 未着手 |
| `IN_PROGRESS` | 進行中 |
| `DONE`        | 完了  |

初期バージョンでは上記3種類とする。

将来的に状態を追加する場合は、アプリケーション側の仕様と合わせて変更する。

## 4.4 priority

`priority` には以下の値を使用する。

| 値        | 意味 |
| -------- | -- |
| `LOW`    | 低  |
| `MEDIUM` | 中  |
| `HIGH`   | 高  |

初期値は `MEDIUM` とする。

## 4.5 制約

* `task_id` を主キーとする。
* `user_id` は `users.user_id` を参照する。
* `title` は必須とする。
* `status` は必須とし、デフォルト値を `TODO` とする。
* `priority` は必須とし、デフォルト値を `MEDIUM` とする。
* `due_date` は任意とする。
* ユーザー削除時に、そのユーザーのタスクをどう扱うかはアプリケーション仕様で定義する。
* 初期バージョンでは、物理削除を行う場合に参照整合性を壊さないよう注意する。

---

# 5. tags

## 5.1 役割

タスクに付与するタグを管理する。

タグはユーザー単位で管理する。

## 5.2 カラム

| カラム          | PostgreSQL型   | NULL | 制約 | 説明            |
| ------------ | ------------- | ---: | -- | ------------- |
| `tag_id`     | `BIGSERIAL`   |   NO | PK | タグID          |
| `user_id`    | `BIGINT`      |   NO | FK | タグを所有するユーザーID |
| `name`       | `VARCHAR(50)` |   NO | -  | タグ名           |
| `created_at` | `TIMESTAMP`   |   NO | -  | 作成日時          |

## 5.3 制約

* `tag_id` を主キーとする。
* `user_id` は `users.user_id` を参照する。
* 同一ユーザーが同じ名前のタグを複数作成できないようにする。
* `user_id` と `name` の組み合わせを一意とする。

したがって、以下のような制約を設定する。

```text
UNIQUE (user_id, name)
```

異なるユーザーが同じ名前のタグを使用することは許可する。

例えば、

```text
user_id = 1, name = "仕事"
user_id = 2, name = "仕事"
```

は許可する。

---

# 6. task_tags

## 6.1 役割

`tasks` と `tags` の多対多の関係を管理する中間テーブル。

1つのタスクに複数のタグを付与でき、1つのタグを複数のタスクに付与できるようにする。

## 6.2 カラム

| カラム       | PostgreSQL型 | NULL | 制約      | 説明    |
| --------- | ----------- | ---: | ------- | ----- |
| `task_id` | `BIGINT`    |   NO | PK / FK | タスクID |
| `tag_id`  | `BIGINT`    |   NO | PK / FK | タグID  |

## 6.3 制約

複合主キーとして、

```text
PRIMARY KEY (task_id, tag_id)
```

を設定する。

また、

```text
task_id → tasks.task_id
tag_id  → tags.tag_id
```

の外部キーを設定する。

同じタスクに同じタグを複数回付与することはできない。

---

# 7. 外部キー

外部キーは以下のとおり。

```text
tasks.user_id
    → users.user_id

tags.user_id
    → users.user_id

task_tags.task_id
    → tasks.task_id

task_tags.tag_id
    → tags.tag_id
```

---

# 8. インデックス

主キーおよびUNIQUE制約によって必要なインデックスは自動的に作成される。

加えて、タスク一覧や検索で使用するカラムについて以下のインデックスを検討する。

## 8.1 tasks

```text
tasks.user_id
tasks.status
tasks.due_date
```

ユーザーごとのタスク一覧、ステータスによる絞り込み、期限による検索を想定する。

ただし、初期バージョンでは必要以上にインデックスを作成せず、実際の検索処理とデータ量を確認したうえで追加する。

## 8.2 tags

`UNIQUE (user_id, name)` によるインデックスを利用する。

---

# 9. 日時の扱い

作成日時・更新日時には `TIMESTAMP` を使用する。

対象カラム：

```text
users.created_at
users.updated_at

tasks.created_at
tasks.updated_at

tags.created_at
```

アプリケーションから日時を設定する。

タイムゾーンを明示的に保持する必要が生じた場合は、将来的に `TIMESTAMP WITH TIME ZONE` への変更を検討する。

---

# 10. 削除方針

初期バージョンでは、論理削除用のカラムは設けない。

ユーザー、タスク、タグの削除を実装する場合は、関連データの整合性を考慮する。

特に以下の関連に注意する。

```text
users
  ↓
tasks
  ↓
task_tags
  ↓
tags
```

タスクを削除する場合、そのタスクに紐付く `task_tags` も削除する必要がある。

タグを削除する場合も、そのタグに紐付く `task_tags` を削除する必要がある。

具体的な `ON DELETE` の設定は、SQL作成時に決定する。

---

# 11. Slack連携テーブル

Slack OAuth連携情報は、既存の4テーブルとは分離した以下の2テーブルで管理する。

* `slack_installations`：Workspace単位のBot User、暗号化済みBot Token、scope
* `slack_connections`：アプリユーザーとSlack User、Workspace、DMチャンネル、連携状態

`slack_connections.user_id`は`users.user_id`を参照し、1アプリユーザーにつき1件とする。`slack_installations.workspace_id`は一意、`slack_connections (workspace_id, slack_user_id)`も一意とし、同一Slackアカウントの重複連携を防止する。ユーザー削除時はConnectionを削除するが、共有Installationは`ON DELETE RESTRICT`により自動削除しない。

Bot Tokenは`bot_access_token_encrypted`へ暗号化して保存する。DDLは`src/main/resources/db/slack_integration.sql`を管理者が既存DBへ適用する。アプリケーションは`ddl-auto: validate`で起動し、実行時にスキーマを変更しない。

---

# 12. テーブル一覧

現在のアプリケーションでは以下の6テーブルを使用する。

```text
users
tasks
tags
task_tags
slack_installations
slack_connections
```

---

# 13. DB設計上の方針

* PostgreSQLを使用する。
* 主キーには `BIGSERIAL` を使用する。
* 外部キーには参照先と同じ `BIGINT` を使用する。
* パスワードは平文で保存しない。
* タスクの状態と優先度は文字列で管理する。
* タグはユーザー単位で管理する。
* タスクとタグは中間テーブルによって多対多で関連付ける。
* タスク管理の基幹テーブルは`users`、`tasks`、`tags`、`task_tags`とする。
* Slack連携は`slack_installations`、`slack_connections`で管理する。
* DB設計とアプリケーション実装を分離し、アプリケーション側で勝手にDB構造を変更しない。
