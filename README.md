# Reading Log

PostgreSQLの `book_test_user` スキーマを使う、Tomcat 10.1向けJava Webアプリです。

## Build

```powershell
mvn clean package
```

生成物は `target/reading-log.war` です。これをTomcatの `webapps` に配置してください。

## Tomcat起動（PowerShell）

リポジトリのルートで以下を実行します。このスクリプトは WAR を毎回ビルド・配備してから Tomcat を起動するため、古い WAR が配備されたままになる問題を防げます。

```powershell
.\run-tomcat.ps1
```

PowerShell の実行ポリシーでブロックされた場合だけ、次のコマンドでこの起動に限って実行します。

```powershell
powershell -ExecutionPolicy Bypass -File .\run-tomcat.ps1
```

終了するには、Tomcat を実行しているターミナルで `Ctrl+C` を押します。

## Tomcat起動（Windowsコマンドプロンプト）

```bat
set "JAVA_HOME=C:\Program Files\Amazon Corretto\jdk21.0.12_9"
set "CATALINA_HOME=%CD%\.tools\apache-tomcat-10.1.59"
set "CATALINA_BASE=%CATALINA_HOME%"
set "DB_URL=jdbc:postgresql://127.0.0.1:5432/postgres"
set "DB_USER=book_test_user"
set "DB_PASSWORD=book_test_password_2026"

call "%CATALINA_HOME%\bin\catalina.bat" run
```

`mvn package` 後には、必ず `target\reading-log.war` を `%CATALINA_HOME%\webapps\reading-log.war` に上書きコピーしてから起動してください。WAR に PostgreSQL JDBC ドライバーが含まれていないと、画面は表示されても API が `No suitable driver` エラーになります。

起動後は `http://localhost:8080/reading-log/` を開いてください。

## DB接続設定

既定値は以下です。必要に応じてTomcat起動前に環境変数で上書きできます。

```text
DB_URL=jdbc:postgresql://127.0.0.1:5432/postgres
DB_USER=book_test_user
DB_PASSWORD=book_test_password_2026
```

## 操作

- 書籍一覧と読書記録はタブで切替
- 開いているタブの「＋ 登録」から登録
- 読書記録の「削除」は確認後に削除して再読込
