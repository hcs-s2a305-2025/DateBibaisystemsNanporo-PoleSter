package jp.co.dbs.nanporo.polestar.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import jp.co.dbs.nanporo.polestar.MessageConfig;
import jp.co.dbs.nanporo.polestar.data.MailData;

@ExtendWith(MockitoExtension.class)
public class MailAppComponentTest {

	@Mock
	private JavaMailSender mailSender;

	@InjectMocks
	private MailAppComponent component;

	@Test
	@DisplayName("MailDataの内容でメールを送信し空文字を返す")
	void testSend() {
		MailData mail = new MailData();
		mail.setTo("to@example.com");
		mail.setFrom("from@example.com");
		mail.setSubject("件名");
		mail.setBody("本文");

		String result = component.send(mail);

		assertThat(result).isEmpty();
		SimpleMailMessage sentMessage = captureSentMessage();
		assertThat(sentMessage.getTo()).containsExactly("to@example.com");
		assertThat(sentMessage.getFrom()).isEqualTo("from@example.com");
		assertThat(sentMessage.getSubject()).isEqualTo("件名");
		assertThat(sentMessage.getText()).isEqualTo("本文");
	}

	@Test
	@DisplayName("メール送信に失敗した場合はエラー定数付きの例外を返す")
	void testSendWhenMailSenderFails() {
		MailData mail = new MailData();
		RuntimeException cause = new RuntimeException("SMTP error");
		doThrow(cause).when(mailSender).send(any(SimpleMailMessage.class));

		assertThatThrownBy(() -> component.send(mail))
				.isInstanceOf(RuntimeException.class)
				.hasMessage(MessageConfig.MAIL_SEND_FAILED_MESSAGE + "SMTP error")
				.hasCause(cause);
	}

	@Test
	@DisplayName("宛先・件名・本文をメール送信に渡す")
	void testSendMail() {
		component.sendMail("to@example.com", "件名", "本文");

		SimpleMailMessage sentMessage = captureSentMessage();
		assertThat(sentMessage.getTo()).containsExactly("to@example.com");
		assertThat(sentMessage.getSubject()).isEqualTo("件名");
		assertThat(sentMessage.getText()).isEqualTo("本文");
		assertThat(sentMessage.getFrom()).isNull();
	}

	@Test
	@DisplayName("メール送信に失敗してもsendMailは例外を送出しない")
	void testSendMailWhenMailSenderFails() {
		doThrow(new RuntimeException("SMTP error"))
				.when(mailSender).send(any(SimpleMailMessage.class));

		assertThatCode(() -> component.sendMail("to@example.com", "件名", "本文"))
				.doesNotThrowAnyException();
	}

	private SimpleMailMessage captureSentMessage() {
		ArgumentCaptor<SimpleMailMessage> messageCaptor =
				ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(messageCaptor.capture());
		return messageCaptor.getValue();
	}
}
