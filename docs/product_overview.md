# Task Manager 概要

## プロダクト概要

ユーザーがログインして自分のタスクを管理するWebアプリケーションです。

タスクの登録・編集・削除・検索・絞り込みに加え、ステータス、優先度、期限、説明、タグを管理できます。タスクとタグはユーザー単位で管理され、他ユーザーのデータは参照・操作できません。

技術構成は以下です。

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA / Hibernate
- Thymeleaf
- Bootstrap
- PostgreSQL
- Slack OAuth 2.0 / Slack Web API

## Codexで実施した内容

- データベース・Entity・Repositoryを実装
  - `users`、`tasks`、`tags`、`task_tags`
  - Slack連携用の`slack_installations`、`slack_connections`
- ユーザー認証を実装
  - ユーザー登録
  - ログイン・ログアウト
  - Spring Securityによる認証・認可
  - パスワードのハッシュ化
- タスク管理機能を実装
  - タスクの登録・一覧・詳細・編集・削除
  - ステータス、優先度、期限、説明の管理
  - サーバー側でのタスク所有者チェック
- タグ機能を実装
  - タグの登録・編集・削除
  - タスクへのタグ付与・解除
  - タグによるタスク絞り込み
  - タグの所有者チェック
- ダッシュボードと画面UIを改善
  - 未完了数、期限超過数、本日期限数、完了率の表示
  - 優先タスクの表示
  - Bootstrapによる統一的な画面構成
  - PC・タブレット・スマートフォン向けのレスポンシブ対応
  - ユーザーの`display_name`表示
  - ステータス・優先度の表示名とバッジ色の統一
- タスク編集画面の不具合を修正
  - 既存の期限日を編集フォームへ初期表示
  - 期限を変更しない更新時に期限を保持
- Slack OAuth連携を実装
  - Slack連携開始、callback、連携解除
  - OAuth stateの生成、10分間の有効期限、一度限りの検証
  - Workspace・Slack Userとの連携管理
  - Bot Tokenの暗号化保存
- Slack通知を実装・移行
  - Slack Web APIによるDM通知
  - タスク登録・更新・完了通知
  - タスク所有者を基準にした通知先決定
  - DMチャンネルの取得・再利用
  - 通知失敗時もタスク処理を成功させる安全なエラー処理
  - 更新前後の差分通知
  - 完了状態への変更に対する専用通知
  - ステータス・優先度の表示名変換
- デモ用サンプルデータを作成
  - `docs/demo_sample_data.sql`
  - `user_id = 8`向けのタスク、タグ、関連データ
  - 重複登録防止、ユーザー存在確認、トランザクション対応
- ドキュメントを更新
  - 要件、アーキテクチャ、DB設計を現行実装に合わせて整理
  - Slack OAuth・DM通知の設定と設計を記載

## 確認済み事項

- Mavenテスト成功
- Spring Bootテストコンテキストの起動成功
- PostgreSQLへの接続確認
- Slack通知の登録・更新・完了・失敗時動作をモックで確認
- OAuth state検証のテストを確認
- DB既存テーブルを維持し、Slack連携用DDLは別管理

## 現在の対象外・未実装事項

- Slack Token Rotation
- Slackからのタスク登録・更新
- 期限前・期限超過の定期通知
- REST APIの外部公開
- メール通知
