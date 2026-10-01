package jp.co.dbs.nanporo.polestar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jp.co.dbs.nanporo.polestar.component.MailAppComponent;
import jp.co.dbs.nanporo.polestar.data.MailData;
import jp.co.dbs.nanporo.polestar.repository.OrderRepository;
import jp.co.dbs.nanporo.polestar.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private MailAppComponent mailComponent;

	@InjectMocks
	private NotificationService service;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(service, "fromAddress", "no-reply@example.com");
	}

	@Test
	@DisplayName("注文完成時に通知を登録し日時を整形してメール送信する")
	void testSendOrderCompleteNotification() {
		int orderId = 12;
		String mail = "customer@example.com";
		Timestamp getTime = Timestamp.valueOf(LocalDateTime.of(2026, 2, 3, 4, 5));
		when(orderRepository.getOrderById(orderId))
				.thenReturn(order(mail, "M0012", getTime));
		when(userRepository.findByMail(mail)).thenReturn(Map.of("name", "田中"));
		when(userRepository.getMaxNoticeId()).thenReturn(20);

		service.sendOrderCompleteNotification(orderId);

		verify(userRepository).insertNoticeWithId(
				eq(21), eq(mail), any(LocalDateTime.class),
				eq("注文番号「M0012」のお弁当の受取準備が整いました。"));
		String sentBody = captureOrderCompleteMailBody(mail);
		assertThat(sentBody).contains("田中 様", "注文番号：M0012", "受取予定日時：2026/02/03 04:05");
	}

	@Test
	@DisplayName("注文が見つからない場合は例外を返す")
	void testSendOrderCompleteNotificationOrderNotFound() {
		when(orderRepository.getOrderById(404)).thenReturn(null);

		assertThatThrownBy(() -> service.sendOrderCompleteNotification(404))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("対象の注文が見つかりません。");
		verifyNoInteractions(userRepository, mailComponent);
	}

	@Test
	@DisplayName("注文にメールアドレスがない場合は例外を返す")
	void testSendOrderCompleteNotificationWithoutMail() {
		when(orderRepository.getOrderById(1)).thenReturn(order(null, "M0001", null));

		assertThatThrownBy(() -> service.sendOrderCompleteNotification(1))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("注文にメールアドレスが設定されていません。");
		verifyNoInteractions(userRepository, mailComponent);
	}

	@Test
	@DisplayName("注文のメールアドレスが空白の場合は例外を返す")
	void testSendOrderCompleteNotificationWithBlankMail() {
		when(orderRepository.getOrderById(1)).thenReturn(order("  ", "M0001", null));

		assertThatThrownBy(() -> service.sendOrderCompleteNotification(1))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("注文にメールアドレスが設定されていません。");
		verifyNoInteractions(userRepository, mailComponent);
	}

	@Test
	@DisplayName("ユーザー情報がない場合は既定名と未設定日時でメール送信する")
	void testSendOrderCompleteNotificationWithoutUserAndTime() {
		String mail = "customer@example.com";
		when(orderRepository.getOrderById(2)).thenReturn(order(mail, "M0002", null));
		when(userRepository.findByMail(mail)).thenReturn(null);
		when(userRepository.getMaxNoticeId()).thenReturn(0);

		service.sendOrderCompleteNotification(2);

		String sentBody = captureOrderCompleteMailBody(mail);
		assertThat(sentBody).contains("お客様 様", "受取予定日時：未設定");
	}

	@Test
	@DisplayName("ユーザー名がない場合は既定名を使い文字列日時をそのまま表示する")
	void testSendOrderCompleteNotificationWithoutUserName() {
		String mail = "customer@example.com";
		when(orderRepository.getOrderById(3)).thenReturn(order(mail, "M0003", "予定日時"));
		when(userRepository.findByMail(mail)).thenReturn(Map.of());
		when(userRepository.getMaxNoticeId()).thenReturn(0);

		service.sendOrderCompleteNotification(3);

		String sentBody = captureOrderCompleteMailBody(mail);
		assertThat(sentBody).contains("お客様 様", "受取予定日時：予定日時");
	}

	@Test
	@DisplayName("ユーザー名取得に失敗しても既定名で通知とメールを送信する")
	void testSendOrderCompleteNotificationWhenUserLookupFails() {
		String mail = "customer@example.com";
		when(orderRepository.getOrderById(4)).thenReturn(order(mail, "M0004", null));
		when(userRepository.findByMail(mail)).thenThrow(new RuntimeException("DB error"));
		when(userRepository.getMaxNoticeId()).thenReturn(0);

		service.sendOrderCompleteNotification(4);

		assertThat(captureOrderCompleteMailBody(mail)).contains("お客様 様");
		verify(userRepository).insertNoticeWithId(eq(1), eq(mail), any(LocalDateTime.class), any(String.class));
	}

	@Test
	@DisplayName("通知本文が100文字を超える場合は100文字に切り詰めて登録する")
	void testSendOrderCompleteNotificationTruncatesContent() {
		String mail = "customer@example.com";
		String orderNumber = "M".repeat(120);
		when(orderRepository.getOrderById(5)).thenReturn(order(mail, orderNumber, null));
		when(userRepository.findByMail(mail)).thenReturn(Map.of("name", "田中"));
		when(userRepository.getMaxNoticeId()).thenReturn(0);
		ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);

		service.sendOrderCompleteNotification(5);

		verify(userRepository).insertNoticeWithId(
				eq(1), eq(mail), any(LocalDateTime.class), contentCaptor.capture());
		assertThat(contentCaptor.getValue()).hasSize(100);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", "  ", "\t"})
	@DisplayName("一斉通知の内容が未入力なら例外を返す")
	void testSendBroadcastNotificationWithInvalidContent(String content) {
		assertThatThrownBy(() -> service.sendBroadcastNotification(content))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("通知内容を入力してください。");
		verifyNoInteractions(userRepository, mailComponent);
	}

	@Test
	@DisplayName("一斉通知の内容が100文字を超える場合は例外を返す")
	void testSendBroadcastNotificationWithTooLongContent() {
		String content = "あ".repeat(101);

		assertThatThrownBy(() -> service.sendBroadcastNotification(content))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("通知内容は100文字以内で入力してください。");
		verifyNoInteractions(userRepository, mailComponent);
	}

	@Test
	@DisplayName("通知対象の顧客リストがnullなら例外を返す")
	void testSendBroadcastNotificationWithoutCustomerList() {
		when(userRepository.findCustomerEmails()).thenReturn(null);

		assertThatThrownBy(() -> service.sendBroadcastNotification("お知らせ"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("通知対象の顧客が存在しません。");
		verifyNoInteractions(mailComponent);
	}

	@Test
	@DisplayName("通知対象の顧客リストが空なら例外を返す")
	void testSendBroadcastNotificationWithEmptyCustomerList() {
		when(userRepository.findCustomerEmails()).thenReturn(List.of());

		assertThatThrownBy(() -> service.sendBroadcastNotification("お知らせ"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("通知対象の顧客が存在しません。");
		verifyNoInteractions(mailComponent);
	}

	@Test
	@DisplayName("一斉通知を有効な顧客に登録してメール送信し空の宛先はスキップする")
	void testSendBroadcastNotification() {
		String content = "  営業時間のお知らせ  ";
		String firstMail = "first@example.com";
		String secondMail = "second@example.com";
		when(userRepository.findCustomerEmails())
				.thenReturn(Arrays.asList(firstMail, null, " ", secondMail));
		when(userRepository.getMaxNoticeId()).thenReturn(40);

		service.sendBroadcastNotification(content);

		ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
		verify(userRepository).insertNoticeWithId(eq(41), eq(firstMail), timeCaptor.capture(), eq("営業時間のお知らせ"));
		verify(userRepository).insertNoticeWithId(eq(42), eq(secondMail), any(LocalDateTime.class), eq("営業時間のお知らせ"));
		assertThat(timeCaptor.getValue()).isNotNull();
		ArgumentCaptor<MailData> mailCaptor = ArgumentCaptor.forClass(MailData.class);
		verify(mailComponent, org.mockito.Mockito.times(2)).send(mailCaptor.capture());
		assertThat(mailCaptor.getAllValues()).extracting(MailData::getTo)
				.containsExactly(firstMail, secondMail);
		assertThat(mailCaptor.getAllValues()).allSatisfy(mail -> {
			assertThat(mail.getFrom()).isEqualTo("no-reply@example.com");
			assertThat(mail.getSubject()).isEqualTo("【ORDER ZANGI】お知らせ");
			assertThat(mail.getBody()).isEqualTo("営業時間のお知らせ");
		});
	}

	private Map<String, Object> order(String mail, String orderNumber, Object getTime) {
		Map<String, Object> order = new HashMap<>();
		order.put("mail", mail);
		order.put("order_number", orderNumber);
		order.put("get_time", getTime);
		return order;
	}

	private String captureOrderCompleteMailBody(String mail) {
		ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
		verify(mailComponent).sendMail(
				eq(mail), eq("【ORDER ZANGI】お弁当の受取準備が整いました"), bodyCaptor.capture());
		return bodyCaptor.getValue();
	}
}
