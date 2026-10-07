package jp.co.dbs.nanporo.polestar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.co.dbs.nanporo.polestar.entity.CustomEntity;
import jp.co.dbs.nanporo.polestar.data.CasherHistoryDto;
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
import jp.co.dbs.nanporo.polestar.repository.UserRepository;
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

	@Mock
	private UserRepository userRepository;

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
		set.setGoodsName("セット");
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
		when(orderDetailRepository.findByOrderId(20)).thenReturn(List.of(
				detail, detail("B021", null, 0, null), detail("B022", null, 1, 999)));
		GoodsEntity goods = new GoodsEntity();
		goods.setGoodsName("商品");
		goods.setPrice(null);
		when(storeRepository.getGoodsEntityById("B020")).thenReturn(Optional.of(goods));
		SetGoodsEntity set = new SetGoodsEntity();
		set.setGoodsName("");
		set.setPrice(null);
		when(storeRepository.getSetGoodsEntityById(7)).thenReturn(Optional.of(set));
		CustomEntity custom = new CustomEntity();
		custom.setGoodsName(null);
		custom.setPrice(null);
		when(storeRepository.getCustomEntityById(9)).thenReturn(Optional.of(custom));
		when(storeRepository.getCustomEntityById(999)).thenReturn(Optional.empty());
		SetGoodsEntity unnamedSet = new SetGoodsEntity();
		unnamedSet.setGoodsName(null);
		unnamedSet.setPrice(10);
		when(storeRepository.getSetGoodsEntityById(8)).thenReturn(Optional.of(unnamedSet));
		CustomEntity unnamedCustom = new CustomEntity();
		unnamedCustom.setGoodsName("");
		unnamedCustom.setPrice(10);
		when(storeRepository.getCustomEntityById(10)).thenReturn(Optional.of(unnamedCustom));

		MobileOrderRequest request = new MobileOrderRequest();
		request.setMobileOrderNo("M0020");
		var response = service.getTodayMobileOrder(request);

		assertThat(response.getItems()).hasSize(3);
		assertThat(response.getItems().get(0).getUnitPrice()).isZero();
		assertThat(response.getItems().get(0).getQuantity()).isEqualTo(1);
		assertThat(response.getItems().get(0).getToppings()).isEmpty();
		assertThat(response.getItems().get(1).getName()).isEqualTo("商品ID:B021");
		assertThat(response.getItems().get(1).getQuantity()).isEqualTo(1);
		assertThat(response.getItems().get(2).getName()).isEqualTo("商品ID:B022");
		OrderDetailEntity emptyNames = detail("B023", 8, 1, 10);
		when(orderDetailRepository.findByOrderId(20)).thenReturn(List.of(
				detail, detail("B021", null, 0, null), detail("B022", null, 1, 999), emptyNames));
		assertThat(service.getTodayMobileOrder(request).getItems()).hasSize(4);
	}

	@Test
	@DisplayName("会計済みの予約注文は二重会計を拒否する")
	void getTodayMobileOrderAlreadyPaid() {
		OrderEntity order = order(24, "M0024", "member@example.com");
		when(orderTRepository.findTodayOrderByNumber(eq("M0024"), any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(Optional.of(order));
		when(transactionRepository.existsByOrderId(24)).thenReturn(true);

		MobileOrderRequest request = new MobileOrderRequest();
		request.setOrderNo("M0024");

		assertThatThrownBy(() -> service.getTodayMobileOrder(request))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("注文番号（M0024）は既に会計が完了しています。");
		verify(orderDetailRepository, never()).findByOrderId(24);
	}

	@Test
	@DisplayName("予約会計では注文を受取済みにして取引と明細を保存する")
	void processMobilePayment() {
		OrderEntity order = order(31, "M0031", "order@example.com");
		when(orderTRepository.findTodayOrderByNumber(eq("M0031"), any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(Optional.of(order));
		when(orderDetailRepository.findByOrderId(31)).thenReturn(List.of(detail("B001", 2, 2, 50)));
		GoodsEntity goods = new GoodsEntity();
		goods.setGoodsName("弁当");
		goods.setPrice(800);
		when(storeRepository.getGoodsEntityById("B001")).thenReturn(Optional.of(goods));
		SetGoodsEntity set = new SetGoodsEntity();
		set.setGoodsName("セット");
		set.setPrice(100);
		when(storeRepository.getSetGoodsEntityById(2)).thenReturn(Optional.of(set));
		CustomEntity custom = new CustomEntity();
		custom.setGoodsName("ソース");
		custom.setPrice(50);
		when(storeRepository.getCustomEntityById(50)).thenReturn(Optional.of(custom));
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
		assertThat(transactionCaptor.getValue().getMail()).isEqualTo("order@example.com");
		assertThat(transactionCaptor.getValue().getUseCoupon()).isEqualTo("値引");
		ArgumentCaptor<TransactionDetailEntity> detailCaptor = ArgumentCaptor.forClass(TransactionDetailEntity.class);
		verify(transactionDetailRepository).save(detailCaptor.capture());
		assertThat(detailCaptor.getValue().getTransactionId()).isEqualTo(88);
		assertThat(detailCaptor.getValue().getReservationCount()).isEqualTo(1);
		assertThat(detailCaptor.getValue().getCount()).isEqualTo(2);
		assertThat(detailCaptor.getValue().getGoodsName()).isEqualTo("弁当");
		assertThat(detailCaptor.getValue().getSetGoodsName()).isEqualTo("セット");
		assertThat(detailCaptor.getValue().getPrice()).isEqualTo(1900);
	}

	@Test
	@DisplayName("既に会計済みの予約注文の会計を拒否する")
	void processMobilePaymentAlreadyPaid() {
		OrderEntity order = order(32, "M0032", "order@example.com");
		when(orderTRepository.findTodayOrderByNumber(eq("M0032"), any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(Optional.of(order));
		when(transactionRepository.existsByOrderId(32)).thenReturn(true);

		assertThatThrownBy(() -> service.processPayment(payment("M0032", null, 0, List.of())))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("この予約注文は既に会計が完了しています。");
		verify(orderTRepository, never()).save(any(OrderEntity.class));
		verify(transactionRepository, never()).save(any(TransactionEntity.class));
	}

	@Test
	@DisplayName("店頭会計では当日の連番で注文を登録し、QR ID未指定時は店頭注文として記録する")
	void processStorePaymentWithoutItems() {
		when(orderTRepository.findMaxOrderNumberToday(any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(Optional.of("0042"), Optional.empty());
		when(orderTRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
			OrderEntity order = invocation.getArgument(0);
			order.setOrderId(41);
			return order;
		});
		when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> {
			TransactionEntity transaction = invocation.getArgument(0);
			transaction.setTransactionId(89);
			return transaction;
		});
		when(orderDetailRepository.findByOrderId(41)).thenReturn(List.of());
		PaymentRequest request = payment(null, null, 0, null);
		request.setUseCoupon("  ");

		var response = service.processPayment(request);

		assertThat(response.getTransactionId()).isEqualTo(89);
		assertThat(service.processPayment(payment(null, "   ", 0, null)).getTransactionId()).isEqualTo(89);
		ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
		verify(orderTRepository, times(2)).save(orderCaptor.capture());
		assertThat(orderCaptor.getAllValues()).extracting(OrderEntity::getOrderNumber)
				.containsExactly("0043", "0001");
		assertThat(orderCaptor.getValue().getMail()).isEqualTo("店頭注文");
		assertThat(orderCaptor.getValue().getStatus()).isEqualTo("調理中");
		ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
		verify(transactionRepository, times(2)).save(transactionCaptor.capture());
		assertThat(transactionCaptor.getAllValues()).extracting(TransactionEntity::getOrderId)
				.containsOnly(41);
		assertThat(transactionCaptor.getValue().getMail()).isEqualTo("店頭注文");
		assertThat(transactionCaptor.getValue().getUseCoupon()).isNull();
		verify(orderTRepository, never()).findTodayOrderByNumber(any(), any(), any());
		verify(transactionDetailRepository, never()).save(any(TransactionDetailEntity.class));
	}

	@Test
	@DisplayName("店頭会計で注文番号を再採番し、トッピングを明細へ分類する")
	void processStorePaymentCreatesToppingDetails() {
		when(orderTRepository.findMaxOrderNumberToday(any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(Optional.of("invalid"));
		when(orderTRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
			OrderEntity order = invocation.getArgument(0);
			order.setOrderId(42);
			return order;
		});
		when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> {
			TransactionEntity transaction = invocation.getArgument(0);
			transaction.setTransactionId(92);
			return transaction;
		});

		List<PaymentRequest.ToppingRequest> toppings = List.of(
				topping(null, null), topping("", null),
				topping("小盛り", null), topping("普通", null), topping("大盛り", null), topping("特盛り", null),
				topping("おろしポン酢", null), topping("おろしポン酢だく", null),
				topping("おろしポン酢だくだく", null),
				topping("タルタル", null), topping("タルタルだく", null), topping("タルタルだくだく", null),
				topping("ネギダレ", null), topping("ネギダレだく", null), topping("ネギタレだくだく", null),
				topping("麻婆", null), topping("麻婆だく", null), topping("麻婆だくだく", null),
				topping("ポテトサラダ", null), topping("大根サラダ", null),
				topping("マカロニ", null), topping("緑茶", null),
				topping("ザンギ追加", 2), topping("ザンギ追加", 3), topping("追加トッピング", null));
		PaymentRequest request = payment(null, "qr@example.com", 0, List.of(
				paymentItem("B001", 2, 1000, toppings),
				paymentItem("B004", null, 500, List.of(topping("麻婆だくだく", null))),
				paymentItem("B002", 1, 300, null),
				paymentItem("B003", 1, 200, List.of())));
		request.setUseCoupon("  学生割引  ");

		GoodsEntity mainGoods = new GoodsEntity();
		mainGoods.setGoodsName("商品");
		mainGoods.setPrice(100);
		when(storeRepository.getGoodsEntityById("B001")).thenReturn(Optional.of(mainGoods));
		GoodsEntity noPriceGoods = new GoodsEntity();
		noPriceGoods.setGoodsName("価格なし");
		when(storeRepository.getGoodsEntityById("B002")).thenReturn(Optional.of(noPriceGoods));
		when(storeRepository.getGoodsEntityById("B003")).thenReturn(Optional.empty());
		when(storeRepository.getGoodsEntityById("B004")).thenReturn(Optional.empty());
		when(storeRepository.getGoodsEntityById("B404")).thenReturn(Optional.empty());
		SetGoodsEntity setGoods = new SetGoodsEntity();
		setGoods.setGoodsName("緑茶");
		when(storeRepository.getSetGoodsEntityById(20)).thenReturn(Optional.of(setGoods));
		when(storeRepository.getSetGoodsEntityById(99)).thenReturn(Optional.empty());
		CustomEntity custom = new CustomEntity();
		custom.setGoodsName("麻婆");
		when(storeRepository.getCustomEntityById(40)).thenReturn(Optional.of(custom));
		when(storeRepository.getCustomEntityById(82)).thenReturn(Optional.empty());
		when(storeRepository.getCustomEntityById(99)).thenReturn(Optional.empty());

		List<OrderDetailEntity> savedOrderDetails = new ArrayList<>();
		doAnswer(invocation -> {
			OrderDetailEntity detail = invocation.getArgument(0);
			savedOrderDetails.add(detail);
			return detail;
		}).when(orderDetailRepository).save(any(OrderDetailEntity.class));
		OrderDetailEntity missingReferences = detail("B404", 99, null, 99);
		missingReferences.setPlusZangiCount(null);
		when(orderDetailRepository.findByOrderId(42)).thenAnswer(invocation -> {
			List<OrderDetailEntity> details = new ArrayList<>(savedOrderDetails);
			details.add(missingReferences);
			return details;
		});

		var response = service.processPayment(request);

		assertThat(response.getTransactionId()).isEqualTo(92);
		ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
		verify(orderTRepository).save(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getOrderNumber()).isEqualTo("0001");
		ArgumentCaptor<OrderDetailEntity> orderDetailCaptor = ArgumentCaptor.forClass(OrderDetailEntity.class);
		verify(orderDetailRepository, times(4)).save(orderDetailCaptor.capture());
		assertThat(orderDetailCaptor.getAllValues()).extracting(OrderDetailEntity::getOrderCount)
				.containsExactly(1, 2, 3, 4);
		assertThat(orderDetailCaptor.getAllValues().get(0).getSetGoodsId()).isEqualTo(20);
		assertThat(orderDetailCaptor.getAllValues().get(0).getPlusZangiCount()).isEqualTo(5);
		assertThat(orderDetailCaptor.getAllValues().get(0).getCustomId()).isEqualTo(40);
		assertThat(orderDetailCaptor.getAllValues().get(1).getCustomId()).isEqualTo(82);
		assertThat(orderDetailCaptor.getAllValues().get(1).getCount()).isEqualTo(1);
		assertThat(orderDetailCaptor.getAllValues().get(2).getCustomId()).isNull();
		assertThat(orderDetailCaptor.getAllValues().get(3).getCustomId()).isNull();

		ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
		verify(transactionRepository).save(transactionCaptor.capture());
		assertThat(transactionCaptor.getValue().getUseCoupon()).isEqualTo("学生割引");
		ArgumentCaptor<TransactionDetailEntity> transactionDetailCaptor =
				ArgumentCaptor.forClass(TransactionDetailEntity.class);
		verify(transactionDetailRepository, times(5)).save(transactionDetailCaptor.capture());
		assertThat(transactionDetailCaptor.getAllValues().get(0).getPrice()).isEqualTo(200);
		assertThat(transactionDetailCaptor.getAllValues().get(0).getSetGoodsName()).isEqualTo("緑茶");
		assertThat(transactionDetailCaptor.getAllValues().get(1).getGoodsName()).isEqualTo("商品ID:B004");
		assertThat(transactionDetailCaptor.getAllValues().get(4).getCount()).isEqualTo(1);
		assertThat(transactionDetailCaptor.getAllValues().get(4).getPlusZangiCount()).isZero();
	}

	@Test
	@DisplayName("予約注文が見つからない会計は注文IDなしの取引として保存する")
	void processPaymentWhenReservationDoesNotExist() {
		when(orderTRepository.findTodayOrderByNumber(eq("M0999"), any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(Optional.empty());
		when(orderDetailRepository.findByOrderId(0)).thenReturn(List.of());
		when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> {
			TransactionEntity transaction = invocation.getArgument(0);
			transaction.setTransactionId(90);
			return transaction;
		});

		service.processPayment(payment("M0999", "member@example.com", null, List.of()));

		ArgumentCaptor<TransactionEntity> transactionCaptor = ArgumentCaptor.forClass(TransactionEntity.class);
		verify(transactionRepository).save(transactionCaptor.capture());
		assertThat(transactionCaptor.getValue().getOrderId()).isZero();
		assertThat(transactionCaptor.getValue().getMail()).isEqualTo("member@example.com");
		assertThat(transactionCaptor.getValue().getUseCoupon()).isNull();
		verify(orderTRepository, never()).save(any(OrderEntity.class));
		verify(orderDetailRepository, never()).save(any(OrderDetailEntity.class));
		verify(transactionDetailRepository, never()).save(any(TransactionDetailEntity.class));
	}

	@Test
	@DisplayName("販売状態の更新は必須値を検証し、未登録商品を通知する")
	void updateGoodsSoldOut() {
		assertThatThrownBy(() -> service.updateGoodsSoldOut(null, true))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("商品IDまたは販売状態が指定されていません。");
		assertThatThrownBy(() -> service.updateGoodsSoldOut("B001", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("商品IDまたは販売状態が指定されていません。");

		when(storeRepository.updateSoldOut("B404", true)).thenReturn(0);
		assertThatThrownBy(() -> service.updateGoodsSoldOut("B404", true))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("指定された商品が見つかりません。ID: B404");

		when(storeRepository.updateSoldOut("B001", false)).thenReturn(1);
		service.updateGoodsSoldOut("B001", false);
		verify(storeRepository).updateSoldOut("B001", false);
	}

	@ParameterizedTest
	@CsvSource({
			"19, 4, ゴールド, 0, 5",
			"19, 2, シルバー, 0, 3",
			"19, 0, ブロンズ, 0, 1",
			"0, 0, 一般, 1, 0"
	})
	@DisplayName("会計でポイントカードを繰り越し、獲得数に応じて会員ランクを更新する")
	void processPaymentUpdatesMemberPoints(int currentPoint, int currentCards, String expectedRank,
			int expectedPoint, int expectedCards) {
		prepareMemberSale(currentPoint, currentCards, "一般");

		service.processPayment(payment(null, "{\"mail\":\"member@example.com\"}", 0,
				List.of(paymentItem("B001", 1, 100))));

		verify(userRepository).updateMemberPointAndRank("member@example.com", expectedPoint, expectedCards, expectedRank);
	}

	@Test
	@DisplayName("ポイント対象の会員が存在しない場合はポイント更新を行わない")
	void processPaymentSkipsMissingMember() {
		prepareMemberSale(0, 0, "一般");
		when(userRepository.findByMail("member@example.com"))
				.thenThrow(new org.springframework.dao.EmptyResultDataAccessException(1));

		service.processPayment(payment(null, "{\"mail\":\"member@example.com\"}", 0,
				List.of(paymentItem("B001", 1, 100))));

		verify(userRepository, never()).updateMemberPointAndRank(any(), any(Integer.class), any(Integer.class), any());
	}

	@Test
	@DisplayName("顧客情報が空ならポイント計算を行わない")
	void processPaymentSkipsEmptyMember() {
		when(userRepository.findByMail("empty@example.com")).thenReturn(Map.of());
		when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> {
			TransactionEntity transaction = invocation.getArgument(0);
			transaction.setTransactionId(96);
			return transaction;
		});

		service.processPayment(payment("M0998", "empty@example.com", 0, List.of()));

		verify(userRepository, never()).updateMemberPointAndRank(any(), any(Integer.class), any(Integer.class), any());
		verify(orderDetailRepository, times(1)).findByOrderId(0);
	}

	@Test
	@DisplayName("会員検索結果がnullの場合はポイント計算を行わない")
	void processPaymentSkipsNullMember() {
		prepareStoreSale();
		when(userRepository.findByMail("null@example.com")).thenReturn(null);

		service.processPayment(payment(null, "null@example.com", 0, List.of()));

		verify(userRepository, never()).updateMemberPointAndRank(any(), any(Integer.class), any(Integer.class), any());
	}

	@Test
	@DisplayName("ザンギの追加数と欠落値を考慮してポイントを計算する")
	void processPaymentCountsZangiWithMissingValues() {
		when(orderTRepository.findMaxOrderNumberToday(any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(Optional.empty());
		when(orderTRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
			OrderEntity order = invocation.getArgument(0);
			order.setOrderId(56);
			return order;
		});
		when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> {
			TransactionEntity transaction = invocation.getArgument(0);
			transaction.setTransactionId(97);
			return transaction;
		});
		GoodsEntity noZangiCount = new GoodsEntity();
		noZangiCount.setGoodsName("個数未設定");
		noZangiCount.setPrice(100);
		when(storeRepository.getGoodsEntityById("B001")).thenReturn(Optional.of(noZangiCount));
		when(storeRepository.getGoodsEntityById("B002")).thenReturn(Optional.empty());
		OrderDetailEntity additionalZangi = detail("B001", null, 0, null);
		additionalZangi.setPlusZangiCount(3);
		OrderDetailEntity missingGoods = detail("B002", null, null, null);
		when(orderDetailRepository.findByOrderId(56)).thenReturn(List.of(additionalZangi, missingGoods));
		Map<String, Object> member = new HashMap<>();
		member.put("point", null);
		member.put("point_card_complete", null);
		member.put("member_rank", "一般");
		when(userRepository.findByMail("member@example.com")).thenReturn(member);

		service.processPayment(payment(null, "{\"mail\":\"member@example.com\"}", 0, List.of()));

		verify(userRepository).updateMemberPointAndRank("member@example.com", 3, 0, "一般");
	}

	@Test
	@DisplayName("ザンギ加算が0なら既存会員のポイントを変更しない")
	void processPaymentDoesNotUpdatePointsWithoutZangi() {
		OrderEntity order = order(91, "M0091", "member@example.com");
		when(orderTRepository.findTodayOrderByNumber(eq("M0091"), any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(Optional.of(order));
		when(orderDetailRepository.findByOrderId(91)).thenReturn(List.of(detail("B404", null, 1, null)));
		when(storeRepository.getGoodsEntityById("B404")).thenReturn(Optional.empty());
		when(userRepository.findByMail("member@example.com")).thenReturn(Map.of(
				"point", 10, "point_card_complete", 2, "member_rank", "ブロンズ"));
		when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> {
			TransactionEntity transaction = invocation.getArgument(0);
			transaction.setTransactionId(98);
			return transaction;
		});

		service.processPayment(payment("M0091", null, 0, List.of()));

		verify(userRepository, never()).updateMemberPointAndRank(any(), any(Integer.class), any(Integer.class), any());
	}

	@Test
	@DisplayName("予約ユーザーのメールアドレスがnullまたは空白の場合はポイント照会を省略する")
	void processPaymentSkipsBlankOrderMail() {
		OrderEntity blankMailOrder = order(92, "M0092", "  ");
		OrderEntity nullMailOrder = order(93, "M0093", null);
		when(orderTRepository.findTodayOrderByNumber(any(), any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(Optional.of(blankMailOrder), Optional.of(nullMailOrder));
		when(orderDetailRepository.findByOrderId(92)).thenReturn(List.of());
		when(orderDetailRepository.findByOrderId(93)).thenReturn(List.of());
		when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> {
			TransactionEntity transaction = invocation.getArgument(0);
			transaction.setTransactionId(100);
			return transaction;
		});

		service.processPayment(payment("M0092", null, 0, List.of()));
		service.processPayment(payment("M0093", null, 0, List.of()));

		verify(userRepository, never()).findByMail(anyString());
	}

	@ParameterizedTest
	@ValueSource(strings = {"{\"mail\": }", "{\"user\":\"member@example.com\"}"})
	@DisplayName("不完全なQR JSONはメール形式に変換せずそのまま検索する")
	void processPaymentWithUnparseableQrJson(String qrId) {
		prepareStoreSale();

		service.processPayment(payment(null, qrId, 0, List.of()));

		verify(userRepository).findByMail(qrId);
	}

	@Test
	@DisplayName("会計履歴の明細・合計を生成し、null値とオプションの欠落を処理する")
	void getCasherHistory() {
		LocalDate date = LocalDate.now();
		TransactionEntity withValues = new TransactionEntity();
		withValues.setTransactionId(1);
		withValues.setTransactionDate(date.atTime(12, 34, 56));
		withValues.setSumMoney(1200);
		withValues.setReceivedMoney(1500);
		withValues.setChangeMoney(300);
		withValues.setUseCoupon("  クーポン  ");
		TransactionEntity withNullValues = new TransactionEntity();
		withNullValues.setTransactionId(2);
		withNullValues.setUseCoupon("  ");
		TransactionEntity withNullCoupon = new TransactionEntity();
		withNullCoupon.setTransactionId(3);

		TransactionDetailEntity detailWithOptions = new TransactionDetailEntity();
		detailWithOptions.setGoodsName("弁当");
		detailWithOptions.setCount(2);
		detailWithOptions.setSetGoodsName("サラダ");
		detailWithOptions.setCustomId(50);
		TransactionDetailEntity detailWithoutOptions = new TransactionDetailEntity();
		detailWithoutOptions.setGoodsName("お茶");
		detailWithoutOptions.setCount(1);
		detailWithoutOptions.setSetGoodsName("");
		detailWithoutOptions.setCustomId(51);
		TransactionDetailEntity detailWithNullValues = new TransactionDetailEntity();
		detailWithNullValues.setGoodsName("不明数");
		detailWithNullValues.setSetGoodsName(null);
		detailWithNullValues.setCustomId(null);
		TransactionDetailEntity detailWithNullCustomName = new TransactionDetailEntity();
		detailWithNullCustomName.setGoodsName("名前なし");
		detailWithNullCustomName.setCount(null);
		detailWithNullCustomName.setSetGoodsName("");
		detailWithNullCustomName.setCustomId(52);
		when(transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(
				any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(List.of(withValues, withNullValues, withNullCoupon));
		when(transactionDetailRepository.findByTransactionId(1))
				.thenReturn(List.of(detailWithOptions, detailWithoutOptions,
						detailWithNullValues, detailWithNullCustomName));
		when(transactionDetailRepository.findByTransactionId(2)).thenReturn(List.of());
		when(transactionDetailRepository.findByTransactionId(3)).thenReturn(List.of());
		CustomEntity custom = new CustomEntity();
		custom.setGoodsName("ソース");
		when(storeRepository.getCustomEntityById(50)).thenReturn(Optional.of(custom));
		when(storeRepository.getCustomEntityById(51)).thenReturn(Optional.empty());
		CustomEntity customWithoutName = new CustomEntity();
		when(storeRepository.getCustomEntityById(52)).thenReturn(Optional.of(customWithoutName));

		Map<String, Object> result = service.getCasherHistory(null);

		assertThat(result).containsEntry("selectedDate", date.toString())
				.containsEntry("totalSales", 1200)
				.containsEntry("totalCustomers", 3);
		List<?> history = (List<?>) result.get("historyList");
		assertThat(history).hasSize(3);
		CasherHistoryDto first = (CasherHistoryDto) history.get(0);
		assertThat(first.getTime()).isEqualTo("12:34:56");
		assertThat(first.getGoodsName()).isEqualTo("弁当 ×2, お茶, 不明数, 名前なし");
		assertThat(first.getDetail()).isEqualTo("セット:サラダ / ソース / 利用:  クーポン  ");
		assertThat(first.getSumMoney()).isEqualTo(1200);
		assertThat(first.getReceivedMoney()).isEqualTo(1500);
		assertThat(first.getChangeMoney()).isEqualTo(300);
		CasherHistoryDto second = (CasherHistoryDto) history.get(1);
		assertThat(second.getTime()).isEmpty();
		assertThat(second.getSumMoney()).isZero();
		assertThat(second.getReceivedMoney()).isZero();
		assertThat(second.getChangeMoney()).isZero();
		assertThat(second.getDetail()).isEmpty();
		assertThat(((CasherHistoryDto) history.get(2)).getDetail()).isEmpty();
		verify(transactionRepository).findByTransactionDateBetweenOrderByTransactionDateDesc(
				date.atStartOfDay(), date.atTime(java.time.LocalTime.MAX));
	}

	@Test
	@DisplayName("指定日の会計履歴が空の場合は0件の集計を返す")
	void getCasherHistoryForDateWithoutTransactions() {
		LocalDate date = LocalDate.of(2026, 10, 1);
		when(transactionRepository.findByTransactionDateBetweenOrderByTransactionDateDesc(
				any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of());

		Map<String, Object> result = service.getCasherHistory(date);

		assertThat(result).containsEntry("selectedDate", date.toString())
				.containsEntry("historyList", List.of())
				.containsEntry("totalSales", 0)
				.containsEntry("totalCustomers", 0);
	}

	@Test
	@DisplayName("会計金額の必須IDを検証し、null金額を0にして保存する")
	void updateTransactionMoney() {
		assertThatThrownBy(() -> service.updateTransactionMoney(null, 100, 100, 0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("取引IDが指定されていません。");
		assertThatThrownBy(() -> service.updateTransactionMoney(99, 100, 100, 0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("指定された取引が見つかりません。ID: 99");

		TransactionEntity transaction = new TransactionEntity();
		when(transactionRepository.findById(3)).thenReturn(Optional.of(transaction));
		service.updateTransactionMoney(3, null, null, null);

		assertThat(transaction.getSumMoney()).isZero();
		assertThat(transaction.getReceivedMoney()).isZero();
		assertThat(transaction.getChangeMoney()).isZero();
		verify(transactionRepository).save(transaction);
	}

	@Test
	@DisplayName("会計金額に指定された値を保存する")
	void updateTransactionMoneyWithValues() {
		TransactionEntity transaction = new TransactionEntity();
		when(transactionRepository.findById(4)).thenReturn(Optional.of(transaction));

		service.updateTransactionMoney(4, 1000, 1500, 500);

		assertThat(transaction.getSumMoney()).isEqualTo(1000);
		assertThat(transaction.getReceivedMoney()).isEqualTo(1500);
		assertThat(transaction.getChangeMoney()).isEqualTo(500);
		verify(transactionRepository).save(transaction);
	}

	private void prepareMemberSale(int currentPoint, int currentCards, String currentRank) {
		when(orderTRepository.findMaxOrderNumberToday(any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(Optional.empty());
		when(orderTRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
			OrderEntity order = invocation.getArgument(0);
			order.setOrderId(55);
			return order;
		});
		when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> {
			TransactionEntity transaction = invocation.getArgument(0);
			transaction.setTransactionId(95);
			return transaction;
		});
		GoodsEntity goods = new GoodsEntity();
		goods.setGoodsName("商品");
		goods.setPrice(100);
		goods.setZangiCount(1);
		when(storeRepository.getGoodsEntityById("B001")).thenReturn(Optional.of(goods));
		when(orderDetailRepository.findByOrderId(55)).thenReturn(List.of(detail("B001", null, 1, null)));
		Map<String, Object> member = new HashMap<>();
		member.put("point", currentPoint);
		member.put("point_card_complete", currentCards);
		member.put("member_rank", currentRank);
		when(userRepository.findByMail("member@example.com")).thenReturn(member);
	}

	private void prepareStoreSale() {
		when(orderTRepository.findMaxOrderNumberToday(any(LocalDateTime.class), any(LocalDateTime.class)))
				.thenReturn(Optional.empty());
		when(orderTRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
			OrderEntity order = invocation.getArgument(0);
			order.setOrderId(57);
			return order;
		});
		when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> {
			TransactionEntity transaction = invocation.getArgument(0);
			transaction.setTransactionId(99);
			return transaction;
		});
		when(orderDetailRepository.findByOrderId(57)).thenReturn(List.of());
	}

	private PaymentRequest.PaymentItemRequest paymentItem(String productId, Integer quantity, Integer total,
			List<PaymentRequest.ToppingRequest> toppings) {
		PaymentRequest.PaymentItemRequest item = paymentItem(productId, quantity, total);
		item.setProductId(productId);
		item.setToppings(toppings);
		return item;
	}

	private PaymentRequest.ToppingRequest topping(String name, Integer plusZangiCount) {
		PaymentRequest.ToppingRequest topping = new PaymentRequest.ToppingRequest();
		topping.setName(name);
		topping.setPlusZangiCount(plusZangiCount);
		return topping;
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