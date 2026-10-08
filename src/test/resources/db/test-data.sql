INSERT INTO user_m(mail, name, password, role, member_rank, gender, birthday, cancel_count, alive, point, point_card_complete, icon)
VALUES
    ('isidaharu@example.com', '石田陽', 'test-password', '顧客', 'ゴールド', '男', '1990-01-01', 0, false, 10, 5, 'sibainu1.png'),
    ('murokishoon@example.com', '室木渚音', 'test-password', '顧客', '一般', '男', '1990-01-02', 0, false, 0, 0, 'sibainu2.png'),
    ('hiroshimaatsushi@example.com', '廣島暖士', 'test-password', '店長', '一般', '男', '1990-01-03', 0, false, 0, 0, 'sibainu3.png'),
    ('koserayuuki@example.com', '小瀬良優希', 'test-password', '店員', '一般', '女', '1990-01-04', 0, false, 0, 0, 'sibainu4.png');

INSERT INTO notice_t(mail, register_time, content)
VALUES
    ('murokishoon@example.com', '2026-09-15 13:30:00', 'モバイル予約(M0001)の受取準備が整いました。'),
    ('isidaharu@example.com', '2026-09-16 10:00:00', 'ゴールド会員限定裏メニューをご利用いただけます。');
