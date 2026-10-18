# ペーパー手帳風 Todo アプリ

Java 17 / Spring Boot / PostgreSQL で動く、日次・長期付箋のTodoアプリです。

## PostgreSQL接続

デフォルトで `jdbc:postgresql://localhost:5432/todo_db` に接続します。初期値の接続ユーザーは `postgres`、パスワードは `postgres` です。環境に合わせて `DATABASE_URL`、`DB_USER`、`DB_PASSWORD` を設定してください。

```sql
CREATE DATABASE todo_db;
```

PostgreSQLが起動している状態でアプリを開始します。初回起動時にJPAがテーブルを作成します。

```sh
mvn spring-boot:run
```

ブラウザで http://localhost:8080 を開きます。

## 仕様

- 毎日0時（Asia/Tokyo）に日次付箋を作成します。アクセス時にも当日分を補完します。
- 日次付箋はデータベースの一意制約でも1日1枚に制限します。
- 長期付箋は何枚でも作成できます。
- 完了タスクは下に並び、日次付箋の進捗を更新します。
- 日次付箋を締めると作成日・最終進捗・未完了タスク概要を保存し、その付箋を読み取り専用にします。
