/* テーブルのデータ削除 */
DELETE FROM transaction_detail_t;
DELETE FROM transaction_t;
DELETE FROM order_detail_t;
DELETE FROM order_t;
DELETE FROM notice_t;
DELETE FROM close_t;
DELETE FROM custom_m;
DELETE FROM set_goods_m;
DELETE FROM goods_m;
DELETE FROM user_m;


/* --------------------------------------------------
1. ユーザマスタ (user_m)
-------------------------------------------------- */
INSERT INTO user_m(mail, name, password, role, member_rank, gender, birthday, cancel_count, alive, point, point_card_complete, icon) 
VALUES
-- パスワード password BCrypt使用

-- 顧客
(
    'isidaharu@example.com',
    '石田陽',
    '$2a$10$xRTXvpMWly0oGiu65WZlm.3YL95LGVV2ASFjDhe6WF4.Qji1huIPa',
    '顧客',
    'ゴールド',
    '男',
    '1990-01-01',
    0,
    FALSE,
    10,
    5,
    'sibainu1.png'
),
(
    'murokishoon@example.com',
    '室木渚音',
    '$2a$10$xRTXvpMWly0oGiu65WZlm.3YL95LGVV2ASFjDhe6WF4.Qji1huIPa',
    '顧客',
    '一般',
    '男',
    '1990-01-02',
    0,
    FALSE,
    0,
    0,
    'sibainu2.png'
),
-- 店長と店員
(
    'hiroshimaatsushi@example.com',
    '廣島暖士',
    '$2a$10$xRTXvpMWly0oGiu65WZlm.3YL95LGVV2ASFjDhe6WF4.Qji1huIPa',
    '店長',
    '一般',
    '男',
    '1990-01-03',
    0,
    FALSE,
    0,
    0,
    'sibainu3.png'
),
(
    'koserayuuki@example.com',
    '小瀬良優希',
    '$2a$10$xRTXvpMWly0oGiu65WZlm.3YL95LGVV2ASFjDhe6WF4.Qji1huIPa',
    '店員',
    '一般',
    '女',
    '1990-01-04',
    0,
    FALSE,
    0,
    0,
    'sibainu4.png'
),
(
    '店頭注文',
    '店頭注文用ユーザー',
    '$2a$10$xRTXvpMWly0oGiu65WZlm.3YL95LGVV2ASFjDhe6WF4.Qji1huIPa',
    '顧客',
    '一般',
    '男',
    '1990-01-05',
    0,
    FALSE,
    0,
    0,
    'agetate!zangitti.png'
),
-- 受入テスト用ユーザー（顧客、店員、店長（管理者）一つずつ）
-- 顧客（受入用）
(
    '20243003-ishidaharu@hcs.ac.jp',
    '石田陽',
    '$2a$10$xRTXvpMWly0oGiu65WZlm.3YL95LGVV2ASFjDhe6WF4.Qji1huIPa',
    '顧客',
    'ゴールド',
    '男',
    '2005-04-07',
    0,
    FALSE,
    0,
    5,
    'sibainu1.png'
),
-- 店員（受入用）
(
    '20242010-murokishoon@hcs.ac.jp',
    '室木渚音',
    '$2a$10$xRTXvpMWly0oGiu65WZlm.3YL95LGVV2ASFjDhe6WF4.Qji1huIPa',
    '店員',
    '一般',
    '男',
    '2005-07-27',
    0,
    FALSE,
    0,
    0,
    'sibainu2.png'
),
-- 店長（受入用）
(
    '20243057-koserayuuki@hcs.ac.jp',
    '小瀬良優希',
    '$2a$10$xRTXvpMWly0oGiu65WZlm.3YL95LGVV2ASFjDhe6WF4.Qji1huIPa',
    '店長',
    '一般',
    '女',
    '2005-08-09',
    0,
    FALSE,
    0,
    0,
    'sibainu3.png'
);


/* --------------------------------------------------
2. 商品マスタ (goods_m)
- 通常商品 (B:お弁当類 / S:単品・サイド)
- 裏商品 (U:裏商品)
-------------------------------------------------- */
INSERT INTO goods_m(goods_id, goods_name, photo, price, calorie, allergy, zangi_count, sold_out, detail, watch_rank)
VALUES

--お弁当類
(
    'B001', 
    '名物！ザンギ弁当（3個入り）', 
    '/img/ザンギ弁当.jpg', 
    580, 
    1000, 
    '小麦,卵',
    3,
    FALSE,
    '当店一番人気のお弁当です。',
    '一般'
),
(
    'B002',
    '名物！ザンギ弁当（4個入り）',
    '/img/ザンギ弁当.jpg',
    680,
    1200,
    '小麦,卵',
    4,
    FALSE,
    'ボリューム満点のお弁当です。',
    '一般'
),
(
    'B003',
    '名物！ザンギ弁当（5個入り）',
    '/img/ザンギ弁当.jpg',
    780,
    1400,
    '小麦,卵',
    5,
    FALSE,
    '大満足のお弁当です。',
    '一般'
),
(
    'B004',
    '三元豚のトンカツ弁当',
    '/img/とんかつ弁当.jpg',
    750,
    900,
    '小麦,卵',
    0,
    FALSE,
    'ジューシーなトンカツを使用したお弁当です。',
    '一般'
),
(
    'B005',
    '特性タレ仕込みの生姜焼き弁当',
    '/img/生姜焼き弁当.jpg',
    720,
    750,
    '小麦,大豆',
    0,
    FALSE,
    'ジューシーな豚肉と特性タレがマッチします。',
    '一般'
),
(
    'B006',
    'こだわり出汁のチキン南蛮弁当',
    '/img/チキン南蛮.jpg',
    780,
    1000,
    '小麦,卵,大豆',
    0,
    FALSE,
    '特性タルタルソースでお召し上がりください。',
    '一般'
),

--単品・サイドメニュー
(
    'S001',
    '単品ザンギ（1個）',
    '/img/ザンギ単品.jpg',
    120,
    200,
    '小麦,卵',
    1,
    FALSE,
    'ジューシーなから揚げです。',
    '一般'
),
(
    'S002',
    '単品トンカツ（1個）',
    '/img/とんかつ弁当.jpg',
    550,
    500,
    '小麦,卵',
    0,
    FALSE,
    'サクサク衣のトンカツです。',
    '一般'
),
(
    'S003',
    '本日の日替わりお味噌汁',
    '/img/ミニカレールー.jpg',
    100,
    50,
    '大豆',
    0,
    FALSE,
    '日替わりでお味噌汁をご提供します。',
    '一般'
),
(
    'S004',
    'ミニカレールー',
    '/img/ミニカレールー.jpg',
    200,
    150,
    '小麦,牛肉,豚肉',
    0,
    FALSE,
    '味変にぴったりなミニカレーです。',
    '一般'
),
(
    'S005',
    '自家製ポテトサラダ',
    '/img/ポテトサラダ.jpg',
    150,
    150,
    '卵,乳',
    0,
    FALSE,
    'ホクホクのおいしいポテトサラダです。',
    '一般'
),
(
    'S006',
    'おろし大根シャキシャキサラダ',
    '/img/大根サラダ.jpg',
    150,
    50,
    '',
    0,
    FALSE,
    'さっぱりとした和風サラダです。',
    '一般'
),
(
    'S007',
    'マカロニたまごサラダ',
    '/img/マカロニサラダ.jpg',
    150,
    180,
    '小麦,卵',
    0,
    FALSE,
    'たまごの風味が広がるマカロニサラダです。',
    '一般'
),
(
    'S008',
    '緑茶(500ml)',
    '/img/ミニカレールー.jpg',
    130,
    0,
    'なし',
    0,
    FALSE,
    'ペットボトルのお茶です。',
    '一般'
),

--裏商品
(
    'U001',
    '特上海鮮丼',
    '/img/海鮮丼.jpg',
    1800,
    1150,
    'エビ,カニ,小麦,大豆',
    0,
    FALSE,
    '厳選素材をふんだんに乗せた至高の海鮮丼です。',
    'ブロンズ'
),
(
    'U002',
    '特上ステーキ丼',
    '/img/ステーキ丼.jpg',
    2200,
    1400,
    '小麦,牛肉,大豆',
    0,
    FALSE,
    '極上黒毛和牛を使用した贅沢ステーキ丼です。',
    'シルバー'
),
(
    'U003',
    '特上ひつまぶし',
    '/img/ひつまぶし.jpg',
    2500,
    1300,
    '小麦,大豆',
    0,
    FALSE,
    'ふっくら香ばしく焼き上げたウナギのひつまぶしです。',
    'ゴールド'
);


/* --------------------------------------------------
3. セット商品マスタ (set_goods_m)
-------------------------------------------------- */
INSERT INTO set_goods_m(set_goods_id, goods_name, price, calorie, allergy, sold_out) VALUES
(
    11,
    '満腹セット（味噌汁＋ポテトサラダ）',
    200,
    200,
    '大豆,卵,乳',
    FALSE
),
(
    12,
    '満腹セット（味噌汁＋大根サラダ）',
    200,
    100,
    '大豆',
    FALSE
),
(
    13,
    '満腹セット（味噌汁＋マカロニたまご）',
    200,
    230,
    '大豆,小麦,卵',
    FALSE
),
(
    20,
    '定番コンビセット（味噌汁＋緑茶）',
    200,
    50,
    '大豆',
    FALSE
);


/* --------------------------------------------------
4. カスタムマスタ (custom_m)
-------------------------------------------------- */
-------------------------------------------------- */
INSERT INTO custom_m(custom_id, goods_name, price, calorie, allergy, sold_out) VALUES
(
    0,
    'なし',
    0,
    0,
    '',
    FALSE
),
(
    10,
    '小盛り（150g）',
    -50,
    -170,
    '',
    FALSE
),
(
    20,
    '普通（250g）',
    0,
    0,
    '',
    FALSE
),
(
    30,
    '大盛り（350g）',
    50,
    170,
    '',
    FALSE
),
(
    40,
    '特盛り（450g）',
    100,
    330,
    '',
    FALSE
),
(
    50,
    'おろしポン酢ソース',
    50,
    30,
    '小麦,大豆',
    FALSE
),
(
    51,
    'おろしポン酢ソースだく',
    100,
    60,
    '小麦,大豆',
    FALSE
),
(
    52,
    'おろしポン酢ソースだくだく',
    150,
    90,
    '小麦,大豆',
    FALSE
),
(
    60,
    '自家製タルタルソース',
    80,
    100,
    '卵,乳',
    FALSE
),
(
    61,
    '自家製タルタルソースだく',
    160,
    200,
    '卵,乳',
    FALSE
),
(
    62,
    '自家製タルタルソースだくだく',
    240,
    300,
    '卵,乳',
    FALSE
),
(
    70,
    '油淋鶏風ネギタレ',
    80,
    60,
    '小麦,ごま,大豆',
    FALSE
),
(
    71,
    '油淋鶏風ネギタレだく',
    160,
    120,
    '小麦,ごま,大豆',
    FALSE
),
(
    72,
    '油淋鶏風ネギタレだくだく',
    240,
    180,
    '小麦,ごま,大豆',
    FALSE
),
(
    80,
    '皆辣麻婆ソース',
    100,
    90,
    '小麦,ごま,大豆,豚肉',
    FALSE
),
(
    81,
    '皆辣麻婆ソースだく',
    200,
    180,
    '小麦,ごま,大豆,豚肉',
    FALSE
),
(
    82,
    '皆辣麻婆ソースだくだく',
    400,
    270,
    '小麦,ごま,大豆,豚肉',
    FALSE
);


/* --------------------------------------------------
5. 休業日トラン (close_t)
-------------------------------------------------- */
INSERT INTO close_t(close_day, close_type)
VALUES
(
    '2026-09-20',
    '定休日'
),
(
    '2026-09-16',
    '臨時休業'
);


/* --------------------------------------------------
6. 通知トラン (notice_t)
- 通知コード
-------------------------------------------------- */
INSERT INTO notice_t(notice_id, mail, register_time, content)
VALUES
(
    1,
    'murokishoon@example.com',
    '2026-09-15 13:30:00', 
    'モバイル予約(M0001)の受取準備が整いました。'
),
(
    2,
    'isidaharu@example.com',
    '2026-09-16 10:00:00',
    'ゴールド会員限定裏メニューをご利用いただけます。'
);


/* --------------------------------------------------
7. 注文トラン (order_t)
- 店頭予約番号 (0001～)
- モバイル予約 (M0001～)
-------------------------------------------------- */
INSERT INTO order_t(order_id, order_number, get_time, mail, register_time, sum_money, memo, status)
VALUES
(
    1,
    'M0001',
    '2026-09-15 13:30:00',
    'murokishoon@example.com',
    '2026-09-15 11:00:00',
    830,
    'なし',
    '受取済'
),
(
    2,
    'M0001',
    '2026-10-07 11:12:00',
    'isidaharu@example.com',
    '2026-10-07 10:00:00',
    2780,
    '無地袋希望',
    '受付'
),
(
    3,
    'M0002',
    '2026-10-07 12:30:00',
    'murokishoon@example.com',
    '2026-10-07 11:00:00',
    860,
    'なし',
    '受付'
);


/* --------------------------------------------------
8. 注文明細トラン (order_detail_t)
-------------------------------------------------- */
INSERT INTO order_detail_t(order_id, order_count, goods_id, set_goods_id, count, plus_zangi_count, custom_id)
VALUES
-- 注文1: 元祖ザンギ弁当(3個) + 満腹セット(味噌汁＋ポテトサラダ) + ご飯大盛り
(
    1,
    1,
    'B001',
    11,
    1,
    3,
    30
),
-- 注文2: 特上海鮮丼 + 満腹セット(味噌汁＋大根サラダ) + 名物！ザンギ弁当（5個）
(
    2,
    1,
    'U001',
    12,
    1,
    0,
    NULL
),
(
    2,
    2,
    'B003',
    NULL,
    1,
    0,
    NULL
),
-- 注文3: 名物！ザンギ弁当（5個入り）+ 自家製タルタルソース
(
    3,
    1, 
    'B003',
    NULL,
    1,
    0,
    60
);


/* --------------------------------------------------
9. 取引トラン (transaction_t)
    取引履歴コード
-------------------------------------------------- */
INSERT INTO transaction_t(transaction_id, order_id, mail, transaction_date, use_coupon, received_money, change_money, sum_money) VALUES
(
    1,
    1,
    'murokishoon@example.com',
    '2026-09-15 12:05:00',
    '100円引き',
    1000,
    270,
    830
);

/* --------------------------------------------------
10. 取引明細トラン (transaction_detail_t)
-------------------------------------------------- */
INSERT INTO transaction_detail_t(transaction_id, reservation_count, goods_name, set_goods_name, count, plus_zangi_count, custom_id, price)
VALUES
(
    1,
    1,
    '名物！ザンギ弁当（3個入り）',
    '満腹セット（味噌汁＋ポテトサラダ）',
    1,
    0,
    30,
    830
);
/* --------------------------------------------------
11. 自動採番シーケンスの同期（修正版）
-------------------------------------------------- */
-- データが存在する場合は MAX値、データが1件もない場合は 1 からスタート（is_called = false）
SELECT setval(
    pg_get_serial_sequence('notice_t', 'notice_id'), 
    COALESCE((SELECT MAX(notice_id) FROM notice_t), 1), 
    (SELECT MAX(notice_id) FROM notice_t) IS NOT NULL
);

SELECT setval(
    pg_get_serial_sequence('order_t', 'order_id'), 
    COALESCE((SELECT MAX(order_id) FROM order_t), 1), 
    (SELECT MAX(order_id) FROM order_t) IS NOT NULL
);

SELECT setval(
    pg_get_serial_sequence('transaction_t', 'transaction_id'), 
    COALESCE((SELECT MAX(transaction_id) FROM transaction_t), 1), 
    (SELECT MAX(transaction_id) FROM transaction_t) IS NOT NULL
);