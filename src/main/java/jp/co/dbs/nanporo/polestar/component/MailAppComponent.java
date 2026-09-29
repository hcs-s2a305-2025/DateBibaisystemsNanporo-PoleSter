package jp.co.dbs.nanporo.polestar.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import jp.co.dbs.nanporo.polestar.MessageConfig;
import jp.co.dbs.nanporo.polestar.data.MailData;

/**
 * Gmail SMTPを利用してメールを送信するコンポーネントクラス。
 */
@Component
public class MailAppComponent {

    /**
     * Spring Bootが設定したメール送信オブジェクト
     */
    @Autowired
    private JavaMailSender mailSender;

    /**
     * 指定されたメールデータをもとにGmailからメールを送信します。
     *
     * @param mail 送信先、送信元、件名、本文を保持するMailData
     * @return 送信成功時は空文字
     * @throws RuntimeException メール送信に失敗した場合
     */
    public String send(MailData mail) {

        try {
            SimpleMailMessage message = new SimpleMailMessage();

            message.setTo(mail.getTo());
            message.setFrom(mail.getFrom());
            message.setSubject(mail.getSubject());
            message.setText(mail.getBody());

            mailSender.send(message);

            return "";

        } catch (Exception e) {
            throw new RuntimeException(
                    MessageConfig.MAIL_SEND_FAILED_MESSAGE + e.getMessage(),
                    e
            );
        }
    }
}