-- Task Manager demo sample data
-- PostgreSQL / user_id = 8
--
-- This script is intentionally limited to tasks, tags, and task_tags.
-- It does not create users or change the database schema.

BEGIN;

-- Do not create a user implicitly. Abort the whole transaction when user 8 is absent.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM users WHERE user_id = 8) THEN
        RAISE EXCEPTION 'Demo data was not inserted: users.user_id = 8 does not exist';
    END IF;
END
$$;

-- Tag names are the idempotency key for this demo data.
INSERT INTO tags (user_id, name)
SELECT 8, demo.name
FROM (VALUES
    ('仕事'),
    ('開発'),
    ('レビュー'),
    ('顧客対応'),
    ('資料作成'),
    ('学習'),
    ('プライベート')
) AS demo(name)
WHERE NOT EXISTS (
    SELECT 1 FROM tags t
    WHERE t.user_id = 8 AND t.name = demo.name
);

-- Task titles are the idempotency key for this demo data.
-- Dates use CURRENT_DATE so the dashboard can demonstrate overdue, upcoming,
-- and no-deadline states whenever the script is run.
INSERT INTO tasks (user_id, title, description, status, priority, due_date)
SELECT 8, demo.title, demo.description, demo.status, demo.priority, demo.due_date
FROM (VALUES
    ('週次ミーティングの資料を準備', '次回の定例会議に向けて進捗と課題を整理する。', 'TODO',        'HIGH',   CURRENT_DATE + 1),
    ('顧客からの問い合わせに返信', '問い合わせ内容を確認し、必要な資料を添えて返信する。', 'IN_PROGRESS', 'HIGH',   CURRENT_DATE - 2),
    ('リリースノートを作成', '今回のリリースに含まれる変更点を利用者向けにまとめる。', 'TODO',        'MEDIUM', CURRENT_DATE + 7),
    ('ログイン画面の動作確認', '主要な入力パターンでログインとログアウトを確認する。', 'DONE',        'MEDIUM', CURRENT_DATE - 5),
    ('検索機能のコードレビュー', '検索条件と権限チェックの実装を確認し、コメントを整理する。', 'IN_PROGRESS', 'HIGH',   CURRENT_DATE + 3),
    ('開発環境の依存ライブラリを更新', '更新候補を確認し、互換性を検証してから適用する。', 'TODO',        'LOW',    CURRENT_DATE + 14),
    ('ユーザーインタビューの議事録を整理', 'インタビューで得た要望と課題をカテゴリ別にまとめる。', 'DONE',        'LOW',    CURRENT_DATE - 10),
    ('データベースのバックアップ手順を確認', '復旧手順を含めて現在の運用手順に不足がないか確認する。', 'IN_PROGRESS', 'MEDIUM', NULL),
    ('新しいAPI仕様を学習', '次の開発で利用するAPIの認証とエラー処理を試す。', 'TODO',        'LOW',    NULL),
    ('画面レイアウトの改善案を検討', 'スマートフォン表示での使いやすさを確認し、改善案をまとめる。', 'DONE',        'MEDIUM', CURRENT_DATE - 1),
    ('チームのタスク分担を見直す', '今週の作業量を確認し、担当と期限を調整する。', 'IN_PROGRESS', 'HIGH',   CURRENT_DATE + 30),
    ('週末の買い物リストを作成', '日用品と食材の不足分を確認してリストにする。', 'TODO',        'LOW',    CURRENT_DATE + 3)
) AS demo(title, description, status, priority, due_date)
WHERE NOT EXISTS (
    SELECT 1 FROM tasks t
    WHERE t.user_id = 8 AND t.title = demo.title
);

-- Reusing tags across tasks demonstrates the many-to-many relationship.
-- ON CONFLICT protects the script when it is run again.
INSERT INTO task_tags (task_id, tag_id)
SELECT t.task_id, g.tag_id
FROM (VALUES
    ('週次ミーティングの資料を準備', '仕事'),
    ('週次ミーティングの資料を準備', '資料作成'),
    ('顧客からの問い合わせに返信', '仕事'),
    ('顧客からの問い合わせに返信', '顧客対応'),
    ('リリースノートを作成', '開発'),
    ('リリースノートを作成', '資料作成'),
    ('ログイン画面の動作確認', '開発'),
    ('ログイン画面の動作確認', 'レビュー'),
    ('検索機能のコードレビュー', '開発'),
    ('検索機能のコードレビュー', 'レビュー'),
    ('開発環境の依存ライブラリを更新', '開発'),
    ('ユーザーインタビューの議事録を整理', '仕事'),
    ('ユーザーインタビューの議事録を整理', '顧客対応'),
    ('新しいAPI仕様を学習', '開発'),
    ('新しいAPI仕様を学習', '学習'),
    ('画面レイアウトの改善案を検討', '開発'),
    ('画面レイアウトの改善案を検討', 'レビュー'),
    ('チームのタスク分担を見直す', '仕事'),
    ('週末の買い物リストを作成', 'プライベート')
) AS demo(title, tag_name)
JOIN tasks t ON t.user_id = 8 AND t.title = demo.title
JOIN tags g ON g.user_id = 8 AND g.name = demo.tag_name
ON CONFLICT (task_id, tag_id) DO NOTHING;

COMMIT;

-- Expected inserted/demo scope when user 8 had no matching demo records:
-- tasks: 12 (TODO 5 / IN_PROGRESS 4 / DONE 3)
-- priorities: LOW 4 / MEDIUM 4 / HIGH 4
-- tags: 7
-- task_tags: 19
-- One task intentionally has no tag: "データベースのバックアップ手順を確認".
