# 読書記録Webアプリ

Tomcat 8.5とPostgreSQLを使用するServlet/JSPアプリです。

## ビルド

```powershell
.\build.ps1
```

生成された `reading-log.war` をTomcatの `webapps` へ配置します。
DB接続の初期値は `localhost:5432/postgres`、ユーザー `postgres`、パスワード `password` です。
変更する場合はTomcat起動前に `READING_LOG_DB_URL`、`READING_LOG_DB_USER`、
`READING_LOG_DB_PASSWORD` の環境変数を設定してください。
