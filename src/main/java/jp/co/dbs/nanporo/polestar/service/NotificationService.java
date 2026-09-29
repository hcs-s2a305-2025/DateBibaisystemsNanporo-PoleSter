package jp.co.dbs.nanporo.polestar.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jp.co.dbs.nanporo.polestar.component.MailAppComponent;
import jp.co.dbs.nanporo.polestar.data.MailData;
import jp.co.dbs.nanporo.polestar.repository.OrderRepository;
import jp.co.dbs.nanporo.polestar.repository.UserRepository;

/**
 * 通知処理を担当するサービスクラス。
 *
 * ・注文が「完成」になった際の個別通知
 * ・ダッシュボードからの一斉通知
 *
 * を担当します。
 */
@Service
public class NotificationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MailAppComponent mailComponent;

    /**
     * メール送信元アドレス。
     *
     * application.properties に
     * mail.from=xxxxx
     * を設定した場合は、その値を使用します。
     *
     * 未設定の場合は既存コードで使用していたアドレスを使用します。
     */
    @Value("${spring.mail.username}")
    private String fromAddress;

    /**
     * 注文が「完成」になった際に、
     * 対象ユーザーへ通知を登録し、メールを送信します。
     *
     * @param orderId 注文ID
     */
    public void sendOrderCompleteNotification(int orderId) {

        // 注文情報を取得
        Map<String, Object> order = orderRepository.getOrderById(orderId);

        if (order == null) {
            throw new IllegalArgumentException("対象の注文が見つかりません。");
        }

        // 注文情報から必要な値を取得
        String mail = (String) order.get("mail");
        String orderNumber = (String) order.get("order_number");
        Object getTime = order.get("get_time");

        if (mail == null || mail.isBlank()) {
            throw new IllegalStateException(
                    "注文にメールアドレスが設定されていません。");
        }

        /*
         * ユーザー名を取得
         *
         * 通知本文だけならメールアドレスだけでも処理できますが、
         * メールでは「○○様」と表示するため取得します。
         */
        String userName = "お客様";

        try {
            Map<String, Object> user = userRepository.findByMail(mail);

            if (user != null && user.get("name") != null) {
                userName = String.valueOf(user.get("name"));
            }
        } catch (Exception e) {
            // ユーザー名取得に失敗しても通知自体は続行
            System.err.println("ユーザー名の取得に失敗しました: " + e.getMessage());
        }

        // notice_t に保存する通知内容
        String content =
                "注文番号「" + orderNumber + "」のお弁当の受取準備が整いました。";

        /*
         * notice_tへ通知登録
         *
         * notice_t.content は VARCHAR(100) のため、
         * 100文字を超えないようにチェックします。
         */
        if (content.length() > 100) {
            content = content.substring(0, 100);
        }

        insertNotice(mail, content);

        // メール本文
        String body =
                userName + " 様\n\n"
                + "ご注文のお弁当の受取準備が整いました。\n\n"
                + "注文番号：" + orderNumber + "\n"
                + "受取予定日時：" + formatDateTime(getTime) + "\n\n"
                + "ご来店をお待ちしております。\n"
                + "ORDER ZANGI";

        // メール送信
        sendMail(
                mail,
                "【ORDER ZANGI】お弁当の受取準備が整いました",
                body
        );
    }

    /**
     * ダッシュボードから全顧客へ一斉通知を送信します。
     *
     * 1回の送信で、
     * ・notice_tへ顧客ごとの通知を登録
     * ・顧客ごとにメールを送信
     *
     * します。
     *
     * @param content 通知内容
     */
    public void sendBroadcastNotification(String content) {

        // 未入力チェック
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("通知内容を入力してください。");
        }

        content = content.trim();

        // notice_t.content VARCHAR(100) に合わせる
        if (content.length() > 100) {
            throw new IllegalArgumentException(
                    "通知内容は100文字以内で入力してください。");
        }

        // 顧客メールアドレスを取得
        List<String> customerEmails =
                userRepository.findCustomerEmails();

        if (customerEmails == null || customerEmails.isEmpty()) {
            throw new IllegalStateException(
                    "通知対象の顧客が存在しません。");
        }

        /*
         * 通知IDを取得。
         *
         * 既存のUserRepositoryが
         * MAX(notice_id) + 1方式を使用しているため、
         * ここでもその方式に合わせます。
         */
        int nextId = userRepository.getMaxNoticeId() + 1;

        LocalDateTime now = LocalDateTime.now();

        /*
         * まずDBへ全顧客分の通知を登録
         */
        for (String email : customerEmails) {

            if (email == null || email.isBlank()) {
                continue;
            }

            userRepository.insertNoticeWithId(
                    nextId,
                    email,
                    now,
                    content
            );

            nextId++;
        }

        /*
         * DB登録後、全顧客へメールを送信
         */
        for (String email : customerEmails) {

            if (email == null || email.isBlank()) {
                continue;
            }

            sendMail(
                    email,
                    "【ORDER ZANGI】お知らせ",
                    content
            );
        }
    }

    /**
     * notice_tへ通知を登録します。
     */
    private void insertNotice(String mail, String content) {

        int nextId = userRepository.getMaxNoticeId() + 1;

        userRepository.insertNoticeWithId(
                nextId,
                mail,
                LocalDateTime.now(),
                content
        );
    }

    /**
     * メールデータを作成して送信します。
     */
    private void sendMail(
            String to,
            String subject,
            String body) {

        MailData mail = new MailData();

        mail.setTo(to);
        mail.setFrom(fromAddress);
        mail.setSubject(subject);
        mail.setBody(body);

        mailComponent.send(mail);

        System.out.println(
                "メール送信完了: " + to
        );
    }

    /**
     * Timestampなどをメール表示用の日時文字列へ変換します。
     */
    private String formatDateTime(Object value) {

        if (value == null) {
            return "未設定";
        }

        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
        }

        return String.valueOf(value);
    }
}