package jp.co.dbs.nanporo.polestar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.co.dbs.nanporo.polestar.data.CartData;
import jp.co.dbs.nanporo.polestar.data.OrderData;
import jp.co.dbs.nanporo.polestar.data.OrderDetailData;
import jp.co.dbs.nanporo.polestar.repository.OrderRepository;
import jp.co.dbs.nanporo.polestar.request.OrderDetailRequest;
import jp.co.dbs.nanporo.polestar.request.OrderRegisterRequest;
import jp.co.dbs.nanporo.polestar.response.ActiveOrderResponse;
import jp.co.dbs.nanporo.polestar.response.OrderHistoryResponse;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class OrderServiceUnitTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private OrderService service;

	@Test
	@DisplayName("注文を登録して明細の既定値とサイドメニュー設定を保持する")
	void insertOrder() {
		when(orderRepository.insertOrder(any(OrderData.class))).thenReturn(12);
		when(orderRepository.insertOrderDetail(any(OrderDetailData.class))).thenReturn(1);
		OrderDetailRequest regular = detail("B001", null, 1);
		OrderDetailRequest side = detail("s001", 55, 2);
		OrderRegisterRequest request = orderRequest(List.of(regular, side));

		var response = service.insertOrder(request);

		assertThat(response.getOrderId()).isEqualTo(12);
		ArgumentCaptor<OrderData> orderCaptor = ArgumentCaptor.forClass(OrderData.class);
		verify(orderRepository).insertOrder(orderCaptor.capture());
		assertThat(orderCaptor.getValue().getMail()).isEqualTo("guest@example.com");
		ArgumentCaptor<OrderDetailData> detailCaptor = ArgumentCaptor.forClass(OrderDetailData.class);
		org.mockito.Mockito.verify(orderRepository, org.mockito.Mockito.times(2))
				.insertOrderDetail(detailCaptor.capture());
		assertThat(detailCaptor.getAllValues()).extracting(OrderDetailData::getCustomId)
				.containsExactly(20, 0);
		assertThat(detailCaptor.getAllValues()).extracting(OrderDetailData::getOrderCount)
				.containsExactly(1, 2);
	}

	@Test
	@DisplayName("注文登録失敗と明細登録失敗を例外にする")
	void insertOrderFailures() {
		when(orderRepository.insertOrder(any(OrderData.class))).thenReturn(0);
		assertThatThrownBy(() -> service.insertOrder(orderRequest(null)))
				.isInstanceOf(RuntimeException.class)
				.hasMessage("注文情報の登録に失敗しました。");

		when(orderRepository.insertOrder(any(OrderData.class))).thenReturn(4);
		when(orderRepository.insertOrderDetail(any(OrderDetailData.class))).thenReturn(0);
		assertThatThrownBy(() -> service.insertOrder(orderRequest(List.of(detail("B001", 20, 1))))
				).isInstanceOf(RuntimeException.class)
				.hasMessage("注文明細の登録に失敗しました。");
	}

	@Test
	@DisplayName("明細なしの注文を登録し、null商品IDと0カスタムIDも受け付ける")
	void insertOrderWithoutDetailsAndNullGoodsId() {
		when(orderRepository.insertOrder(any(OrderData.class))).thenReturn(21, 22, 23);
		when(orderRepository.insertOrderDetail(any(OrderDetailData.class))).thenReturn(1);

		assertThat(service.insertOrder(orderRequest(null)).getOrderId()).isEqualTo(21);
		assertThat(service.insertOrder(orderRequest(List.of())).getOrderId()).isEqualTo(22);
		service.insertOrder(orderRequest(List.of(detail(null, 0, 1))));

		ArgumentCaptor<OrderDetailData> detailCaptor = ArgumentCaptor.forClass(OrderDetailData.class);
		verify(orderRepository).insertOrderDetail(detailCaptor.capture());
		assertThat(detailCaptor.getValue().getGoodsId()).isNull();
		assertThat(detailCaptor.getValue().getCustomId()).isEqualTo(20);
	}

	@Test
	@DisplayName("セット商品IDが0またはnullでザンギ数がnullの明細を登録する")
	void insertOrderWithEmptySetGoodsAndZangiCount() {
		when(orderRepository.insertOrder(any(OrderData.class))).thenReturn(24);
		when(orderRepository.insertOrderDetail(any(OrderDetailData.class))).thenReturn(1);

		OrderDetailRequest zeroSetGoods = detail("B001", 10, 1);
		zeroSetGoods.setSetGoodsId(0);
		zeroSetGoods.setPlusZangiCount(null);
		OrderDetailRequest nullSetGoods = detail("B002", 30, 1);
		nullSetGoods.setSetGoodsId(null);
		nullSetGoods.setPlusZangiCount(null);

		service.insertOrder(orderRequest(List.of(zeroSetGoods, nullSetGoods)));

		ArgumentCaptor<OrderDetailData> detailCaptor = ArgumentCaptor.forClass(OrderDetailData.class);
		org.mockito.Mockito.verify(orderRepository, org.mockito.Mockito.times(2))
				.insertOrderDetail(detailCaptor.capture());
		assertThat(detailCaptor.getAllValues()).extracting(OrderDetailData::getSetGoodsId)
				.containsExactly(0, null);
		assertThat(detailCaptor.getAllValues()).extracting(OrderDetailData::getPlusZangiCount)
				.containsExactly(0, 0);
	}

	@Test
	@DisplayName("複数行の注文を注文単位にまとめ商品とソースを表示する")
	void getActiveOrders() {
		when(orderRepository.getActiveOrdersByMail("guest@example.com")).thenReturn(List.of(
				activeRow("1", "B001", "弁当", "50", "2", "1200", "受付"),
				activeRow(1L, "B001", "弁当", "60", 2, 1200, "受付"),
				activeRow(1, "B001", "弁当", "10", 2, 1200, "受付"),
				activeRow(1, "B001", "弁当", null, 2, 1200, "受付"),
				activeRow(1, "S001", "サイド", null, 1, 1200, "受付"),
				activeRow(2, null, null, null, null, null, "完成"),
				activeRow(3, "B002", "小盛り", "10", 1, 300, "受付"),
				activeRow(3, "B003", "商品", null, 1, 300, "受付"),
				activeRowWithUnstableGoodsId(4)));

		List<ActiveOrderResponse> orders = service.getActiveOrders("guest@example.com");

		assertThat(orders).hasSize(4);
		assertThat(orders.get(0).getOrder().getSumMoney()).isEqualTo(1200);
		assertThat(orders.get(0).getDetails()).hasSize(2);
		assertThat(orders.get(0).getDetails().get(0).getSourceName()).isEqualTo("自家製タルタルソース");
		assertThat(orders.get(0).getDetails().get(0).getRiceAmount()).isEqualTo("普通 (250g)");
		assertThat(orders.get(0).getDetails().get(1).getRiceAmount()).isEqualTo("なし");
		assertThat(orders.get(0).getGoodsNames()).isEqualTo("弁当, サイド");
		assertThat(orders.get(1).getOrder().getSumMoney()).isZero();
		assertThat(orders.get(1).getDetails()).isEmpty();
		assertThat(orders.get(2).getDetails().get(0).getRiceAmount()).isEqualTo("小盛り (150g)");
		assertThat(orders.get(3).getDetails().get(0).getGoodsId()).isNull();
	}

	@Test
	@DisplayName("注文履歴の日時・商品画像・カスタム情報を変換する")
	void getOrderHistory() {
		Map<String, Object> nullId = historyRow(null, null, null, null, null, null, null);
		Map<String, Object> parentOnly = historyRow(1, "M0001", 1200,
				Timestamp.valueOf("2026-10-04 12:30:00"), null, null, null);
		Map<String, Object> detail = historyRow(1, "M0001", 1200, null,
				"B001", "", "82");
		Map<String, Object> second = historyRow(2, "M0002", 800, null,
				"B002", "menu.png", null);
		Map<String, Object> missingPhotoAndZeroCustom = historyRow(2, "M0002", 800, null,
				"B003", null, 0);
		when(orderRepository.getOrderHistoryByMail("guest@example.com"))
				.thenReturn(List.of(nullId, parentOnly, detail, second, missingPhotoAndZeroCustom));

		List<OrderHistoryResponse> history = service.getOrderHistory("guest@example.com");

		assertThat(history).hasSize(2);
		assertThat(history.get(0).getFormattedDate()).isEqualTo("2026年10月4日");
		assertThat(history.get(0).getItems().get(0).getPhoto()).isEqualTo("img/ザンギ弁当.jpg");
		assertThat(history.get(0).getItems().get(0).getCustomName()).isEqualTo("皆辣麻婆ソースだくだく");
		assertThat(history.get(0).getItems().get(0).getCustomPrice()).isEqualTo(180);
		assertThat(history.get(1).getItems().get(0).getPhoto()).isEqualTo("menu.png");
		assertThat(history.get(1).getItems().get(0).getCustomName()).isNull();
		assertThat(history.get(1).getItems().get(1).getPhoto()).isEqualTo("img/ザンギ弁当.jpg");
		assertThat(history.get(1).getItems().get(1).getCustomName()).isNull();
	}

	@Test
	@DisplayName("注文取消はIDが指定された場合だけ削除する")
	void cancelOrder() {
		service.cancelOrder(null);
		verify(orderRepository, never()).deleteOrder(anyInt());

		service.cancelOrder(3);
		verify(orderRepository).deleteOrder(3);
	}

	@Test
	@DisplayName("注文完成は有効な状態だけ更新して通知する")
	void completeOrder() {
		assertThatThrownBy(() -> service.completeOrder(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("注文IDが指定されていません。");
		when(orderRepository.getOrderById(1)).thenReturn(null);
		assertThatThrownBy(() -> service.completeOrder(1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("指定された注文が存在しません。");

		when(orderRepository.getOrderById(2)).thenReturn(Map.of("status", "完成"));
		assertThatThrownBy(() -> service.completeOrder(2))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("現在のステータス: 完成");

		when(orderRepository.getOrderById(3)).thenReturn(Map.of("status", "受付"));
		when(orderRepository.updateStatusToComplete(3)).thenReturn(0);
		assertThatThrownBy(() -> service.completeOrder(3))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("注文ステータスの更新に失敗しました。");

		when(orderRepository.getOrderById(4)).thenReturn(Map.of("status", "調理中"));
		when(orderRepository.updateStatusToComplete(4)).thenReturn(1);
		service.completeOrder(4);
		verify(notificationService).sendOrderCompleteNotification(4);
	}

	@Test
	@DisplayName("注文から復元したカートに料金・名称・既定値を設定する")
	void restoreCartFromOrder() {
		assertThat(service.restoreCartFromOrder(null)).isEmpty();
		when(orderRepository.getOrderDetailsByOrderId(5)).thenReturn(List.of(
				cartRow("B001", "弁当", "1000", "7", "80", "30"),
				cartRow("S001", "サイド", null, null, null, null),
				cartRow("B002", "小盛", 500, 5, 10, 10),
				cartRow("B005", "標準ライス", 400, 5, 0, 0)));

		List<CartData> cart = service.restoreCartFromOrder(5);

		assertThat(cart).hasSize(4);
		assertThat(cart.get(0).getPrice()).isEqualTo(1000);
		assertThat(cart.get(0).getZangiPrice()).isEqualTo(200);
		assertThat(cart.get(0).getSourceType()).isEqualTo("皆辣麻婆ソース");
		assertThat(cart.get(0).getSourcePrice()).isEqualTo(100);
		assertThat(cart.get(0).getRiceAmount()).isEqualTo("大盛り (350g)");
		assertThat(cart.get(0).getTotalPrice()).isEqualTo(1350);
		assertThat(cart.get(1).getPrice()).isZero();
		assertThat(cart.get(1).getZangiCount()).isZero();
		assertThat(cart.get(1).getRiceAmount()).isEqualTo("なし");
		assertThat(cart.get(1).getSourceType()).isEqualTo("なし");
		assertThat(cart.get(2).getRiceAmount()).isEqualTo("小盛り (150g)");
		assertThat(cart.get(2).getRicePrice()).isEqualTo(-30);
		assertThat(cart.get(3).getRiceAmount()).isEqualTo("普通 (250g)");
	}

	@Test
	@DisplayName("ソースコードとライスコードの各料金分岐をカート復元で検証する")
	void restoreCartForAllOptions() {
		List<Map<String, Object>> rows = new ArrayList<>();
		for (int code : new int[] {50, 51, 52, 60, 61, 62, 70, 71, 72, 80, 81, 82, 999}) {
			rows.add(cartRow("B001", "弁当", 100, 5, code, 20));
		}
		when(orderRepository.getOrderDetailsByOrderId(8)).thenReturn(rows);

		List<CartData> cart = service.restoreCartFromOrder(8);

		assertThat(cart).extracting(CartData::getSourcePrice)
				.containsExactly(80, 120, 150, 80, 120, 150, 80, 120, 150, 100, 140, 180, 0);
		assertThat(cart).extracting(CartData::getSourceType).doesNotContainNull();
		when(orderRepository.getOrderDetailsByOrderId(9)).thenReturn(List.of(
				cartRow("S001", "サイド", 100, 5, 0, 0),
				cartRow("B002", "弁当", 100, 5, 0, 10),
				cartRow("B003", "弁当", 100, 5, 0, 40),
				cartRow("B004", "弁当", 100, 5, 0, 99),
				cartRow(null, "商品IDなし", 100, 5, 0, 0)));

		List<CartData> rice = service.restoreCartFromOrder(9);

		assertThat(rice).extracting(CartData::getRiceAmount)
				.containsExactly("なし", "小盛り (150g)", "特盛 (450g)", "普通 (250g)", "普通 (250g)");
		assertThat(rice).extracting(CartData::getRicePrice).containsExactly(0, -30, 100, 0, 0);
	}

	private OrderRegisterRequest orderRequest(List<OrderDetailRequest> details) {
		OrderRegisterRequest request = new OrderRegisterRequest();
		request.setGetTime("2026-10-04T12:00:00");
		request.setRegisterTime("2026-10-04T11:00:00");
		request.setMail("guest@example.com");
		request.setSumMoney(1200);
		request.setMemo("memo");
		request.setStatus("受付");
		request.setOrderType("RESERVATION");
		request.setOrderDetails(details);
		return request;
	}

	private OrderDetailRequest detail(String goodsId, Integer customId, int count) {
		OrderDetailRequest detail = new OrderDetailRequest();
		detail.setGoodsId(goodsId);
		detail.setSetGoodsId(3);
		detail.setCustomId(customId);
		detail.setCount(count);
		detail.setPlusZangiCount(0);
		return detail;
	}

	private Map<String, Object> activeRow(Object orderId, String goodsId, String goodsName,
			Object customId, Object count, Object sumMoney, String status) {
		Map<String, Object> row = new HashMap<>();
		row.put("order_id", orderId);
		row.put("order_number", "M" + orderId);
		row.put("get_time", Timestamp.valueOf("2026-10-04 12:00:00"));
		row.put("mail", "guest@example.com");
		row.put("sum_money", sumMoney);
		row.put("memo", "memo");
		row.put("status", status);
		row.put("goods_id", goodsId);
		row.put("goods_name", goodsName);
		row.put("custom_id", customId);
		row.put("count", count);
		return row;
	}

	private Map<String, Object> activeRowWithUnstableGoodsId(Integer orderId) {
		return new HashMap<>(activeRow(orderId, "B004", "商品", null, 1, 300, "受付")) {
			private boolean firstGoodsIdRead = true;

			@Override
			public Object get(Object key) {
				if ("goods_id".equals(key)) {
					if (firstGoodsIdRead) {
						firstGoodsIdRead = false;
					} else {
						return null;
					}
				}
				return super.get(key);
			}
		};
	}

	private Map<String, Object> historyRow(Integer orderId, String orderNumber, Integer sumMoney,
			Timestamp getTime, String goodsId, String photo, Object customId) {
		Map<String, Object> row = new HashMap<>();
		row.put("order_id", orderId);
		row.put("order_number", orderNumber);
		row.put("sum_money", sumMoney);
		row.put("get_time", getTime);
		row.put("goods_id", goodsId);
		row.put("goods_name", "商品");
		row.put("price", "700");
		row.put("photo", photo);
		row.put("count", "2");
		row.put("custom_id", customId);
		return row;
	}

	private Map<String, Object> cartRow(String goodsId, String goodsName, Object price,
			Object zangiCount, Object customId, Object setGoodsId) {
		Map<String, Object> row = new HashMap<>();
		row.put("goods_id", goodsId);
		row.put("goods_name", goodsName);
		row.put("price", price);
		row.put("photo", "goods.png");
		row.put("plus_zangi_count", zangiCount);
		row.put("custom_id", customId);
		row.put("set_goods_id", setGoodsId);
		return row;
	}
}