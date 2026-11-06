# dolly closet

ドールのプロフィールと、商品デザイン・色違いの所持アイテム・ブランド・カテゴリを管理するSpring Boot Webアプリです。Java 21、Thymeleaf、JdbcTemplate、PostgreSQLを使用します（JPAはスキーマ自動変更に使いません）。

## 必要な環境

- Java 21
- PostgreSQL（このプロジェクトのローカル初期値は `127.0.0.1:5432`、データベース `postgres`）
- DBには以下の大文字・引用符付きテーブルが事前に作成されていること
  - `"Dolls"`
  - `"Brands"`
  - `"Categories"`
  - `"Clothing_Designs"`
  - `"Clothing_Items"`
  - `"Clothing_Item_Dolls"`
  - `"Saved_Color_Palettes"`

旧スキーマを使用中の場合は、起動前に後述の移行SQLを適用してください。アプリはスキーマを前提にし、起動時にテーブル作成・変更・削除や初期データ投入は行いません。`spring.jpa.hibernate.ddl-auto=none` と `spring.sql.init.mode=never` を設定しています。

## 商品デザイン分離のDB移行

`database/migrations/20261102_separate_clothing_designs_and_owned_items.sql` は、旧 `"Clothing_Items"` を商品デザイン表 `"Clothing_Designs"` に移し、元の衣装1行ごとに同じIDの所持アイテム行を作ります。既存の商品名・ブランド・カテゴリ・セット名はデザインに、サイズ・備考・作成／更新日時とドール紐付けは所持アイテムに引き継ぎます。データ行の削除・初期化は行いません。移行後は、コピー済みの旧 `"Size"`・`"Notes"` カラムをデザイン表から削除し、新しい所持アイテム表に集約します。

移行SQLはトランザクションで実行され、所持アイテム件数とドール紐付け件数が移行前後で一致しない場合はロールバックします。再実行時は移行済みの `"Clothing_Designs"` を検出してスキップします。適用前にアプリを停止し、対象DBが開発用であることを確認してバックアップを取得してください。実行ユーザーには対象テーブルのALTER/CREATE/DML権限が必要です。

```powershell
pg_dump -Fc -h 127.0.0.1 -U admin -d postgres -f .\before-clothing-design-migration.dump
psql -v ON_ERROR_STOP=1 -h 127.0.0.1 -U admin -d postgres -f .\database\migrations\20261102_separate_clothing_designs_and_owned_items.sql
```

`psql` が作業環境にない場合は、pgAdminのクエリーツール等で同じSQLファイルを対象DBへ一度実行してください。最後の結果に `Clothing_Designs`、`Clothing_Items`、`Clothing_Item_Dolls` の件数が表示されます。適用後にアプリを起動します。

### 保存済み配色テーブルの追加

ColorMagic連携で保存した配色を使うには、商品デザイン分離マイグレーション適用後、バックアップを確認し、次の追加マイグレーションを一度実行します。これは新しい `"Saved_Color_Palettes"` テーブルと検索用インデックスを追加します。既存の衣装データは変更しません。

```powershell
pg_dump -Fc -h 127.0.0.1 -U admin -d postgres -f .\before-saved-palettes-migration.dump
psql -v ON_ERROR_STOP=1 -h 127.0.0.1 -p 5432 -U admin -d postgres -f .\database\migrations\20261102_add_saved_color_palettes.sql
```

### ColorMagic配色検索

お洋服（所持アイテム）の詳細画面で、日本語の色名や雰囲気（例：「ミント」「青」「レトロ」）を入力して検索できます。日本語はJavaバックエンドからDeepL API Freeで英語に翻訳してから、ColorMagic APIへ送信します。DeepLは最大5秒でタイムアウトし、429/5xxは短い待機を挟んで最大3回まで再試行します。英語キーワードは翻訳せずColorMagicへ送ります。

DeepL API Freeの認証キーを取得し、アプリ起動前にPowerShellで環境変数 `DEEPL_AUTH_KEY` を設定してください。キーはソースコードやチャットへ貼らないでください。

```powershell
$secureKey = Read-Host "DeepL API Free key" -AsSecureString
$keyPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKey)
try {
    $env:DEEPL_AUTH_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($keyPointer)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($keyPointer)
}
```

この設定をした同じPowerShellターミナルからアプリを起動します。ターミナルを閉じると環境変数は失われます。キーが未設定でも英語検索は利用できますが、日本語検索時はキー未設定の案内が表示されます。翻訳された検索語は画面に表示されます。

ColorMagicの検索結果は保存操作をするまでデータベースへ登録されません。保存後は当該所持アイテムに紐づいて表示され、ColorMagicへ再接続せず閲覧できます。同じColorMagicパレットIDは同じ所持アイテムに重複保存できず、他の色違い所持アイテムには個別に保存できます。API障害時は配色セクション内に案内を表示し、詳細ページの他の機能は利用できます。

## データベース接続設定

接続先や認証情報は環境変数で設定します。既定のURL・ユーザー名は現在のローカル開発環境向けです。パスワードの既定値はありません。

PowerShellのターミナルで必要な値を設定してから起動してください。

```powershell
$env:DB_URL = "jdbc:postgresql://127.0.0.1:5432/postgres"
$env:DB_USERNAME = "admin"
$securePassword = Read-Host "PostgreSQL password" -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
}
```

`DB_PASSWORD` はソースコードや設定ファイルに記載したり、Gitへコミットしたりしないでください。アプリ停止後は `$env:DB_PASSWORD = $null` でターミナルから消去できます。`DB_URL` と `DB_USERNAME` は省略可能です。省略した場合、それぞれ `jdbc:postgresql://127.0.0.1:5432/postgres` と `admin` を使用します。DBには必要なテーブル権限を持つ専用ユーザーを使用し、通常運用ではスーパーユーザーをアプリに設定しないでください。

## 起動方法

プロジェクトのルートで、環境変数を設定した同じPowerShellターミナルから実行します。

```powershell
.\mvnw.cmd spring-boot:run
```

ブラウザーで <http://localhost:8080> を開きます。停止するにはターミナルで `Ctrl+C` を押します。

## 主な使い方

- トップ画面でドールのカードを見たり、プロフィールを登録・編集・削除したりできます。
- `/designs` で商品名・ブランド・カテゴリ・セット名など共通情報を登録し、詳細から色違いの所持アイテムを追加できます。同一デザインから複数の独立した所持アイテムを登録できます。
- 所持アイテムごとにカラー名、任意の `#RRGGBB` カラーコード、メーカー表記サイズ、対応ドール、個別メモを記録できます。色コード入力時に色見本が表示されます。カラーコードと着用ドールは空欄でも保存できます。
- 所持アイテム登録・編集では、カラー名を入力して「色コード候補を探す」を押すと、ColorMagicの配色から候補色を表示します。色見本を選ぶとHEXコード欄へ入力されます。これは近い色の候補で、カラー名と完全一致するコードとは限りません。
- 所持アイテム詳細の「配色を考える」から日本語キーワードをDeepLで英語へ翻訳してColorMagicの配色を検索できます。英語検索、色見本・HEXコードのコピー、所持アイテムごとの保存と削除にも対応しています。日本語検索には環境変数 `DEEPL_AUTH_KEY` が必要です。
- お洋服一覧では商品名、ブランド、カテゴリ、サイズ、セット名、ドール、カラー名を組み合わせて検索できます。色違いは所持アイテム単位で表示します。
- ブランド・購入元、カテゴリは商品デザイン登録時に候補選択または自由入力で追加できます。専用カテゴリ管理ページはありません。ブランド一覧は削除のみで、デザインから参照中のブランドは削除できません。
- セット名は商品デザインに保持され、同じブランド・セット名の商品デザインとその所持アイテムを表示します。セット名を設定しても、ドールとの紐付けは自動作成されません。
- 所持アイテムの削除はそのアイテムのドール紐付けのみを削除します。ドール削除もそのドールに対応する紐付けのみを削除します。所持アイテムが残っている商品デザインは削除できません。

## テストとビルド

DB接続なしでフォームのバリデーション、商品デザインから複数色の所持品・個別ドール紐付け、およびDeepL翻訳クライアントのテストを実行できます。

```powershell
.\mvnw.cmd '-Dtest=CatalogFormsTests,CatalogServiceTests,ColorMagicClientTests,JapaneseKeywordTranslatorTests' test
```

全テストを実行する場合は、先にDB環境変数を設定してください。`DollClothingCatalogApplicationTests` はSpringコンテキストを起動し、設定済みPostgreSQLへ接続します。

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

## 主な画面

| URL | 内容 |
|---|---|
| `/` | ドールカードと登録件数 |
| `/dolls` | ドール一覧 |
| `/designs` | 商品デザインと色違い所持品 |
| `/clothes` | 所持アイテムの検索・一覧 |
| `/sets` | ブランド・セット名ごとの一覧 |
| `/brands` | ブランド・購入元の管理 |
