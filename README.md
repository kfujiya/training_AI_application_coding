# 図書館の蔵書・貸出状況 WEB アプリ

Java Servlet と PostgreSQL を使った、Tomcat 8.5 向けの WAR アプリケーションです。トップ画面に登録済み書籍を1冊1行で表示します。

## 表示内容

- タイトル: `books.title`
- 貸出中のユーザ: `loan_records.returned_at IS NULL` の貸出記録に紐づくユーザ氏名
- 予約人数: `reservations.loaned_at IS NULL` の予約件数

貸出中の記録がない書籍は「貸出なし」と表示します。複数の貸出中記録がある場合は、該当ユーザ名を「、」区切りで表示します。

## ビルド

JDK と Maven を用意し、リポジトリのルートで実行してください。

```powershell
.\.tools\apache-maven-3.10.2\bin\mvn.cmd clean package
```

生成物は `target\library-status.war` です。

## Tomcat 8.5 で起動

WAR を Tomcat の `webapps` フォルダに `library-status.war` の名前で配置します。PostgreSQL の接続設定を環境変数に指定してから Tomcat を起動してください。

```powershell
$env:LIBRARY_DB_URL = 'jdbc:postgresql://localhost:5432/postgres'
$env:LIBRARY_DB_USER = 'nakamura'
$env:LIBRARY_DB_PASSWORD = '<PostgreSQL のパスワード>'
& "$env:CATALINA_HOME\bin\catalina.bat" run
```

ブラウザで `http://localhost:8080/library-status/` を開きます。Tomcat を Windows サービスとして起動する場合は、同じ3つの環境変数をサービス起動前に Windows のシステム環境変数として設定してください。

## データベース

`books`、`users`、`loan_records`、`reservations` テーブルを利用します。接続先は環境変数で指定し、認証情報はソースコードに含めません。
