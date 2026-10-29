# 天気情報管理アプリ

Open-Meteoから地点検索・7日間の時間別予報を取得し、専用PostgreSQLへ保存して閲覧するSpring Bootアプリです。

## 接続先の安全条件

アプリは `127.0.0.1:5434/weather_db` 以外のPostgreSQLへ接続しないよう起動時に検証します。`DB_PASSWORD`が未設定の場合も起動を拒否します。5432/5433へのフォールバックはありません。

PostgreSQLの初期化・DB作成はこのリポジトリのビルドやテストでは行いません。専用インスタンスを準備した後、`.env.example`を`.env`等へコピーし、秘密情報を設定してください。

## 起動

1. PostgreSQL専用インスタンスを5434、DB名`weather_db`、ユーザー`weather_app`で別途準備する。
2. `cp .env.example .env`相当でローカル設定を作り、`DB_PASSWORD`を設定する。
3. 環境変数を読み込んで `./gradlew bootRun` を実行する。Gradle Wrapperがない環境では、Gradle 8.xで `gradle bootRun` を実行する。
4. `http://localhost:8080`を開く。

既存の`build/libs/weather-information-management-0.0.1-SNAPSHOT.jar`は上書きしません。新しいJARは`weather-information-management-source.jar`として出力します。

## DB設計

`locations`は検索結果として登録した地点を管理し、`weather_records`は取得日時`fetched_at`と予報対象日時`forecast_at`を分離して時間別データを保存します。`(location_id, forecast_at)`の一意制約とUPSERTで重複を防止し、地点削除は履歴がある場合に拒否します。

## API

- `GET /api/locations/search?name=東京`
- `GET/POST/DELETE /api/locations[/{id}]`
- `POST /api/locations/{id}/weather`
- `GET /api/weather/history?locationId={id}&from=YYYY-MM-DD&to=YYYY-MM-DD`
