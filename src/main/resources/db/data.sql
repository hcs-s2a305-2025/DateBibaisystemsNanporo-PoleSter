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

INSERT INTO user_m(mail, name, password, role, member_rank, gender  , birthday, cancel_count, alive, point, point_card_complete) 
VALUES
-- パスワード password BCrypt使用

-- 顧客
(
    'isidahalu@example.com',
    '石田陽',
    '$2a$10$xRTXvpMWly0oGiu65WZlm.3YL95LGVV2ASFjDhe6WF4.Qji1huIPa',
    '顧客',
    '一般',
    '男',
    '1990-01-01',
    0,
    FALSE,
    0,
    0
)
