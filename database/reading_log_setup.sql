BEGIN;

CREATE TABLE IF NOT EXISTS public.books (
    isbn varchar(13) PRIMARY KEY,
    title varchar(255) NOT NULL,
    volume integer,
    author varchar(255) NOT NULL,
    translator varchar(255),
    page_count integer NOT NULL CHECK (page_count > 0),
    reading_status varchar(20) NOT NULL DEFAULT 'unread'
        CHECK (reading_status IN ('unread', 'reading', 'completed')),
    CHECK (isbn ~ '^[0-9]{10}([0-9]{3})?$'),
    CHECK (volume IS NULL OR volume > 0)
);

CREATE TABLE IF NOT EXISTS public.reading_records (
    reading_record_id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reading_date date NOT NULL,
    isbn varchar(13) NOT NULL REFERENCES public.books (isbn)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    pages_read integer NOT NULL CHECK (pages_read >= 0),
    comment text,
    is_completed boolean NOT NULL DEFAULT false,
    UNIQUE (reading_date, isbn)
);

COMMENT ON TABLE public.books IS '読書記録の対象となる書籍';
COMMENT ON COLUMN public.books.reading_status IS 'unread: 未読、reading: 読書中、completed: 読破済み';
COMMENT ON TABLE public.reading_records IS '日ごとの読書記録';

INSERT INTO public.books
    (isbn, title, volume, author, translator, page_count, reading_status)
VALUES
    ('9784101010014', 'こころ', NULL, '夏目漱石', NULL, 384, 'completed'),
    ('9784101050089', '人間失格', NULL, '太宰治', NULL, 208, 'completed'),
    ('9784101001012', '羅生門・鼻', NULL, '芥川龍之介', NULL, 304, 'reading'),
    ('9784101025032', '銀河鉄道の夜', NULL, '宮沢賢治', NULL, 352, 'unread'),
    ('9784041099124', '吾輩は猫である', 1, '夏目漱石', NULL, 368, 'reading'),
    ('9784041099131', '吾輩は猫である', 2, '夏目漱石', NULL, 352, 'unread'),
    ('9784101092058', '雪国', NULL, '川端康成', NULL, 208, 'unread'),
    ('9784101121185', '金閣寺', NULL, '三島由紀夫', NULL, 384, 'unread'),
    ('9784101030098', '山月記・李陵', NULL, '中島敦', NULL, 240, 'unread'),
    ('9784101061016', '檸檬', NULL, '梶井基次郎', NULL, 224, 'unread'),
    ('9784151200533', 'アルジャーノンに花束を', NULL, 'ダニエル・キイス', '小尾芙佐', 464, 'reading'),
    ('9784151200526', '老人と海', NULL, 'アーネスト・ヘミングウェイ', '高見浩', 160, 'completed'),
    ('9784102122044', '星の王子さま', NULL, 'サン＝テグジュペリ', '河野万里子', 160, 'completed'),
    ('9784152096817', '一九八四年', NULL, 'ジョージ・オーウェル', '高橋和久', 512, 'unread'),
    ('9784102025017', '華氏451度', NULL, 'レイ・ブラッドベリ', '伊藤典夫', 320, 'unread'),
    ('9784151200304', 'アンドロイドは電気羊の夢を見るか？', NULL, 'フィリップ・K・ディック', '浅倉久志', 352, 'unread'),
    ('9784150112905', '夏への扉', NULL, 'ロバート・A・ハインライン', '福島正実', 384, 'unread'),
    ('9784150310196', '虐殺器官', NULL, '伊藤計劃', NULL, 432, 'unread'),
    ('9784150311650', 'ハーモニー', NULL, '伊藤計劃', NULL, 384, 'unread'),
    ('9784102071045', '罪と罰', 1, 'フョードル・ドストエフスキー', '工藤精一郎', 544, 'unread')
ON CONFLICT (isbn) DO UPDATE SET
    title = EXCLUDED.title,
    volume = EXCLUDED.volume,
    author = EXCLUDED.author,
    translator = EXCLUDED.translator,
    page_count = EXCLUDED.page_count,
    reading_status = EXCLUDED.reading_status;

INSERT INTO public.reading_records
    (reading_date, isbn, pages_read, comment, is_completed)
VALUES
    (DATE '2026-08-20', '9784101010014', 120, '先生と私の距離感が印象的だった。', false),
    (DATE '2026-08-21', '9784101010014', 264, '後半を一気に読み、手紙の重さが心に残った。', true),
    (DATE '2026-08-22', '9784151200533', 80, '文章が読みやすく、変化の兆しが興味深い。', false),
    (DATE '2026-08-23', '9784101001012', 96, '短編ごとに異なる緊張感がある。', false),
    (DATE '2026-08-24', '9784102122044', 160, '短い物語だが、大切なものについて考えさせられた。', true)
ON CONFLICT (reading_date, isbn) DO UPDATE SET
    pages_read = EXCLUDED.pages_read,
    comment = EXCLUDED.comment,
    is_completed = EXCLUDED.is_completed;

COMMIT;
