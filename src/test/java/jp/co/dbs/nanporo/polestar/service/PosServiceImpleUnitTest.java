package jp.co.dbs.nanporo.polestar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.co.dbs.nanporo.polestar.entity.CustomEntity;
import jp.co.dbs.nanporo.polestar.entity.GoodsEntity;
import jp.co.dbs.nanporo.polestar.entity.OrderDetailEntity;
import jp.co.dbs.nanporo.polestar.entity.OrderEntity;
import jp.co.dbs.nanporo.polestar.entity.SetGoodsEntity;
import jp.co.dbs.nanporo.polestar.entity.TransactionDetailEntity;
import jp.co.dbs.nanporo.polestar.entity.TransactionEntity;
import jp.co.dbs.nanporo.polestar.repository.OrderDetailRepository;
import jp.co.dbs.nanporo.polestar.repository.OrderTRepository;
import jp.co.dbs.nanporo.polestar.repository.StoreRepository;
import jp.co.dbs.nanporo.polestar.repository.TransactionDetailRepository;
import jp.co.dbs.nanporo.polestar.repository.TransactionRepository;
import jp.co.dbs.nanporo.polestar.request.MobileOrderRequest;
import jp.co.dbs.nanporo.polestar.request.PaymentRequest;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class PosServiceImpleUnitTest {

	@Mock
	private OrderTRepository orderTRepository;

	@Mock
	private OrderDetailRepository orderDetailRepository;

	@Mock
	private StoreRepository storeRepository;

	@Mock
	private TransactionRepository transactionRepository;

	@Mock
	private TransactionDetailRepository transactionDetailRepository;

	@InjectMocks
	private PosServiceImple service;

	@Test
	@DisplayName("POS実装がPOSサービス契約を実装する")
	void implementsPosService() {
		assertThat(service).isInstanceOf(PosService.class);
	}

	@Test
	@DisplayName("当日の予約注文と商品・セット・カスタム価格を取得する")
	void getTodayMobileOrder() {
		OrderEntity order = order(12, "M0012", "member@example.com");
		when(orderTRepository.findTodayOrderByNumber(eq("M0012"), any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(Optional.of(order));
		when(orderDetailRepository.findByOrderId(12)).thenReturn(List.of(
				detail("B001", 2, 3, 50), detail("B404", 0, null, null)));
		GoodsEntity goods = new GoodsEntity();
		goods.setGoodsName("弁当");
		goods.setPrice(800);
		when(storeRepository.getGoodsEntityById("B001")).thenReturn(Optional.of(goods));
		when(storeRepository.getGoodsEntityById("B404")).thenReturn(Optional.empty());
		SetGoodsEntity set = new SetGoodsEntity();
		set.setSetGoodsName("セット");
		set.setPrice(100);
		when(storeRepository.getSetGoodsEntityById(2)).thenReturn(Optional.of(set));
		CustomEntity custom = new CustomEntity();
		custom.setGoodsName("ソース");
		custom.setPrice(50);
		when(storeRepository.getCustomEntityById(50)).thenReturn(Optional.of(custom));

		MobileOrderRequest request = new MobileOrderRequest();
		request.setOrderNo(" M0012 ");
		var response = service.getTodayMobileOrder(request);

		assertThat(response.isSuccess()).isTrue();
		assertThat(response.getOrderId()).isEqualTo(12);
		assertThat(response.getItems()).hasSize(2);
		assertThat(response.getItems().get(0).getName()).isEqualTo("弁当");
		assertThat(response.getItems().get(0).getUnitPrice()).isEqualTo(950);
		assertThat(response.getItems().get(0).getQuantity()).isEqualTo(3);
		assertThat(response.getItems().get(0).getUnitTotal()).isEqualTo(2850);
		assertThat(response.getItems().get(0).getToppings()).extracting("name")
				.containsExactly("セット", "ソース");
		assertThat(response.getItems().get(1).getName()).isEqualTo("商品ID:B404");
		assertThat(response.getItems().get(1).getQuantity()).isEqualTo(1);
		assertThat(response.getItems().get(1).getUnitPrice()).isZero();
	}

	@Test
	@DisplayName("空番号・店頭番号・当日予約不在をそれぞれ拒否する")
	void getTodayMobileOrderFailures() {
		MobileOrderRequest request = new MobileOrderRequest();	
		assertThatThrownBy(() -> service.getTodayMobileOrder(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("注文番号が指定されていません。");

		request.setOrderNo("0012");
		assertThatThrownBy(() -> service.getTodayMobileOrder(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("入力された番号（0012）は予約注文番号ではありません。");

		request.setOrderNo("M404");
		when(orderTRepository.findTodayOrderByNumber(eq("M404"), any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(Optional.empty());
		assertThatThrownBy(() -> service.getTodayMobileOrder(request))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("当日の予約注文が見つかりません: M404");
	}

	@Test
	@DisplayName("セット・カスタム名や価格がnullまたは空でも安全に合計する")
	void getTodayMobileOrderWithMissingOptions() {
		when(orderTRepository.findTodayOrderByNumber(eq("M0020"), any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(Optional.of(order(20, "M0020", "guest@example.com")));
		OrderDetailEntity detail = detail("B020", 7, 8, 9);
		detail.setCount(null);
		when(orderDetailRepository.findByOrderId(20)).thenReturn(List.of(detail));
		GoodsEntity goods = new GoodsEntity();
		goods.setGoodsName("商品");
		goods.setPrice(null);
		when(storeRepository.getGoodsEntityById("B020")).thenReturn(Optional.of(goods));
		SetGoodsEntity set = new SetGoodsEntity();
		set.setSetGoodsName("");
		set.setPrice(null);
		when(storeRepository.getSetGoodsEntityById(7)).thenReturn(Optional.of(set));
		CustomEntity custom = new CustomEntity();
		custom.setGoodsName(null);
		custom.setPrice(null);
		when(storeRepository.getCustomEntityById(9)).thenReturn(Optional.of(custom));

		MobileOrderRequest request = new MobileOrderRequest();
		request.setMobileOrderNo("M0020");
		var response = service.getTodayMobileOrder(request);

		assertThat(response.getItems()).hasSize(1);
		assertThat(response.getItems().get(0).getUnitPrice()).isZero();
		assertThat(response.getItems().get(0).getQuantity()).isEqualTo(1);
		assertThat(response.getItems().get(0).getToppings()).isEmpty();
	}

	@Test
	@DisplayName("予約会計では注文を受取済みにして取引と明細を保存する")
	void processMobilePayment() {
		OrderEntity order = order(31, "M0031", "order@example.com");
		when(orderTRepository.findTodayOrderByNumber(eq("M0031"), any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(Optional.of(order));
		when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> {
			TransactionEntity transaction = invocation.getArgument(0);
			transaction.setTransactionId(88);
			return transaction;
		});
		PaymentRequest request = payment(" M0031 ", "qr@example.com", -100,
				List.of(paymentItem("弁当", 2, 1800)));

		var response = service.processPayment(request);

		assertThat(response.isSuccess()).isTrue();
		assertThat(response.getTransactionId()).isEqualTo(88);
		assertThat(order.getStatus()).isEqualTo("受取済");
		verify(orderTRepository).save(order);
		ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
		verify(transactionRepository).save(transactionCaptor.capture());
		assertThat(transactionCaptor.getValue().getOrderId()).isEqualTo(31);
		assertThat(transactionCaptor.getValue().getMail()).isEqualTo("qr@example.com");
		assertThat(transactionCaptor.getValue().getUseCoupon()).isEqualTo("値引");
		ArgumentCaptor<TransactionDetailEntity> detailCaptor = ArgumentCaptor.forClass(TransactionDetailEntity.class);
		verify(transactionDetailRepository).save(detailCaptor.capture());
		assertThat(detailCaptor.getValue().getTransactionId()).isEqualTo(88);
		assertThat(detailCaptor.getValue().getReservationCount()).isEqualTo(1);
		assertThat(detailCaptor.getValue().getCount()).isEqualTo(2);
		assertThat(detailCaptor.getValue().getPrice()).isEqualTo(1800);
	}

	@Test
	@DisplayName("店頭会計では注文を検索せず、QR ID未指定時はゲストとして記録する")
	void processStorePaymentWithoutItems() {
		when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> {
			TransactionEntity transaction = invocation.getArgument(0);
			transaction.setTransactionId(89);
			return transaction;
		});
		PaymentRequest request = payment(null, null, 0, null);

		var response = service.processPayment(request);

		assertThat(response.getTransactionId()).isEqualTo(89);
		ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
		verify(transactionRepository).save(transactionCaptor.capture());
		assertThat(transactionCaptor.getValue().getOrderId()).isZero();
		assertThat(transactionCaptor.getValue().getMail()).isEqualTo("guest@example.com");
		assertThat(transactionCaptor.getValue().getUseCoupon()).isNull();
		verify(orderTRepository, never()).findTodayOrderByNumber(any(), any(), any());
		verify(orderTRepository, never()).save(any(OrderEntity.class));
		verify(transactionDetailRepository, never()).save(any(TransactionDetailEntity.class));
	}

	@Test
	@DisplayName("予約注文が見つからない会計もゲスト取引として保存し複数明細を採番する")
	void processPaymentWhenReservationDoesNotExist() {
		when(orderTRepository.findTodayOrderByNumber(eq("M0999"), any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(Optional.empty());
		when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> {
			TransactionEntity transaction = invocation.getArgument(0);
			transaction.setTransactionId(90);
			return transaction;
		});

		service.processPayment(payment("M0999", "member@example.com", 10,
				List.of(paymentItem("A", 1, 100), paymentItem("B", 3, 300))));

		ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
		verify(transactionRepository).save(transactionCaptor.capture());
		assertThat(transactionCaptor.getValue().getOrderId()).isZero();
		assertThat(transactionCaptor.getValue().getUseCoupon()).isNull();
		ArgumentCaptor<TransactionDetailEntity> detailCaptor = ArgumentCaptor.forClass(TransactionDetailEntity.class);
		verify(transactionDetailRepository, org.mockito.Mockito.times(2)).save(detailCaptor.capture());
		assertThat(detailCaptor.getAllValues()).extracting(TransactionDetailEntity::getReservationCount)
				.containsExactly(1, 2);
	}

	private OrderEntity order(Integer id, String orderNumber, String mail) {
		OrderEntity order = new OrderEntity();
		order.setOrderId(id);
		order.setOrderNumber(orderNumber);
		order.setMail(mail);
		order.setStatus("受付");
		return order;
	}

	private OrderDetailEntity detail(String goodsId, Integer setGoodsId, Integer count, Integer customId) {
		OrderDetailEntity detail = new OrderDetailEntity();
		detail.setGoodsId(goodsId);
		detail.setSetGoodsId(setGoodsId);
		detail.setCount(count);
		detail.setCustomId(customId);
		return detail;
	}

	private PaymentRequest payment(String mobileOrderNo, String qrId, Integer discount,
			List<PaymentRequest.PaymentItemRequest> items) {
		PaymentRequest request = new PaymentRequest();
		request.setMobileOrderNo(mobileOrderNo);
		request.setQrId(qrId);
		request.setDiscount(discount);
		request.setTotal(1000);
		request.setReceived(1500);
		request.setChange(500);
		request.setItems(items);
		return request;
	}

	private PaymentRequest.PaymentItemRequest paymentItem(String name, Integer quantity, Integer total) {
		PaymentRequest.PaymentItemRequest item = new PaymentRequest.PaymentItemRequest();
		item.setName(name);
		item.setQuantity(quantity);
		item.setTotal(total);
		return item;
	}
}