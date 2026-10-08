package jp.co.dbs.nanporo.polestar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jp.co.dbs.nanporo.polestar.repository.InnerdisplayRepository;
import jp.co.dbs.nanporo.polestar.response.ActiveOrderResponse;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class InnerdisplayServiceUnitTest {

	@Mock
	private InnerdisplayRepository repository;

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private InnerdisplayService service;

	@Test
	@DisplayName("厨房注文をまとめ、ライス量とソース行を表示用に変換する")
	void getActiveOrders() {
		List<Map<String, Object>> rows = new ArrayList<>();
		rows.add(row(null, "B000", "無効", 20, 1));
		rows.add(row("1", "B001", "弁当", "10", "2"));
		rows.add(row(1L, "B001", "弁当", 51, 2));
		rows.add(row(1, "S001", "サイド", null, 1));
		rows.add(row(2, "B002", null, 999, 1));
		rows.add(row(2, null, null, null, null));
		when(repository.getKitchenOrders()).thenReturn(rows);

		List<ActiveOrderResponse> orders = service.getActiveOrders("staff@example.com");

		assertThat(orders).hasSize(2);
		assertThat(orders.get(0).getOrder().getOrderId()).isEqualTo(1);
		assertThat(orders.get(0).getDetails()).hasSize(2);
		assertThat(orders.get(0).getDetails().get(0).getRiceAmount()).isEqualTo("小盛り (150g)");
		assertThat(orders.get(0).getDetails().get(0).getSourceName()).isEqualTo("おろしポン酢ソースだく");
		assertThat(orders.get(0).getDetails().get(1).getRiceAmount()).isEqualTo("なし");
		assertThat(orders.get(1).getGoodsNames()).isEmpty();
		assertThat(orders.get(1).getDetails()).hasSize(1);
		assertThat(orders.get(1).getDetails().get(0).getSourceName()).isEqualTo("なし");
	}

	@Test
	@DisplayName("同じ商品のnullカスタムIDと50未満のコードは別明細として追加する")
	void getActiveOrdersWithNonSourceCustomCodes() {
		when(repository.getKitchenOrders()).thenReturn(List.of(
				row(3, "B003", "弁当", 20, 1),
				row(3, "B003", "弁当", null, 1),
				row(3, "B003", "弁当", 49, 1)));

		List<ActiveOrderResponse> orders = service.getActiveOrders("staff@example.com");

		assertThat(orders).hasSize(1);
		assertThat(orders.get(0).getDetails()).hasSize(3);
		assertThat(orders.get(0).getDetails()).extracting(ActiveOrderResponse.OrderDetailItem::getGoodsId)
				.containsExactly("B003", "B003", "B003");
	}

	@Test
	@DisplayName("ライス・ソースの各コードを名称へ変換する")
	void getActiveOrdersForAllOptions() {
		List<Map<String, Object>> rows = new ArrayList<>();
		int orderId = 10;
		for (Object code : List.of(0, 10, 20, 30, 40, 999)) {
			rows.add(row(orderId++, "B" + orderId, "商品", code, 1));
		}

		for (int code : new int[] {50, 51, 52, 60, 61, 62, 70, 71, 72, 80, 81, 82}) {
			rows.add(row(orderId++, "B" + orderId, "商品", code, 1));
		}
		rows.add(row(orderId, "B999", "商品", -1, 1));
		when(repository.getKitchenOrders()).thenReturn(rows);

		List<ActiveOrderResponse> orders = service.getActiveOrders("staff@example.com");

		assertThat(orders).hasSize(19);
		assertThat(orders).extracting(order -> order.getDetails().get(0).getRiceAmount())
				.containsExactly("なし", "小盛り (150g)", "普通 (250g)", "大盛り (350g)",
						"特盛り (450g)", "普通 (250g)", "普通 (250g)", "普通 (250g)",
						"普通 (250g)", "普通 (250g)", "普通 (250g)", "普通 (250g)",
						"普通 (250g)", "普通 (250g)", "普通 (250g)", "普通 (250g)",
						"普通 (250g)", "普通 (250g)", "普通 (250g)");
		assertThat(orders.subList(6, 18)).extracting(order -> order.getDetails().get(0).getSourceName())
				.containsExactly("おろしポン酢ソース", "おろしポン酢ソースだく", "おろしポン酢ソースだくだく",
						"自家製タルタルソース", "自家製タルタルソースだく", "自家製タルタルソースだくだく",
						"油淋鶏風ネギダレ", "油淋鶏風ネギダレだく", "油淋鶏風ネギダレだくだく",
						"皆辣麻婆ソース", "皆辣麻婆ソースだく", "皆辣麻婆ソースだくだく");
	}

	@Test
	@DisplayName("同じ明細行のご飯とソースを表示し、商品を重複させない")
	void getActiveOrdersWithRiceAndSourceOnOneDetail() {
		Map<String, Object> row = row(20, "B020", "ザンギ弁当", 30, 1);
		row.put("source_custom_id", 61);
		row.put("plus_zangi_count", 2);
		row.put("set_goods_id", 11);
		row.put("set_goods_name", "満腹ザンギセット（味噌汁＋ポテトサラダ）");
		row.put("set_goods_price", 200);
		when(repository.getKitchenOrders()).thenReturn(List.of(row));

		List<ActiveOrderResponse> orders = service.getActiveOrders("staff@example.com");

		assertThat(orders).hasSize(1);
		assertThat(orders.get(0).getDetails()).hasSize(1);
		assertThat(orders.get(0).getDetails().get(0).getRiceAmount()).isEqualTo("大盛り (350g)");
		assertThat(orders.get(0).getDetails().get(0).getSourceName()).isEqualTo("自家製タルタルソースだく");
		assertThat(orders.get(0).getDetails().get(0).getSourcePrice()).isEqualTo(120);
		assertThat(orders.get(0).getDetails().get(0).getZangiCount()).isEqualTo(2);
		assertThat(orders.get(0).getDetails().get(0).getSetName())
				.isEqualTo("満腹ザンギセット（味噌汁＋ポテトサラダ）");
		assertThat(orders.get(0).getDetails().get(0).getSetPrice()).isEqualTo(200);
	}

	@Test
	@DisplayName("厨房表示でセットと新形式ソースを扱い、未指定値は既定値にする")
	void getActiveOrdersWithSetAndSourceFields() {
		Map<String, Object> withOptions = row(30, "B030", "弁当", null, 1);
		withOptions.put("plus_zangi_count", null);
		withOptions.put("set_goods_id", 11);
		withOptions.put("set_goods_name", "セット");
		withOptions.put("set_goods_price", 200);
		withOptions.put("source_custom_id", 61);
		Map<String, Object> noOptions = row(31, "B031", "別商品", 20, 1);
		noOptions.put("plus_zangi_count", null);
		noOptions.put("set_goods_id", 0);
		noOptions.put("source_custom_id", 0);
		Map<String, Object> duplicateDifferentFormat = row(30, "B030", "弁当", 61, 1);
		duplicateDifferentFormat.put("source_custom_id", 61);
		when(repository.getKitchenOrders()).thenReturn(List.of(withOptions, noOptions, duplicateDifferentFormat));

		List<ActiveOrderResponse> orders = service.getActiveOrders("staff@example.com");

		assertThat(orders.get(0).getDetails()).hasSize(2);
		assertThat(orders.get(0).getDetails().get(0).getZangiCount()).isZero();
		assertThat(orders.get(0).getDetails().get(0).getSetName()).isEqualTo("セット");
		assertThat(orders.get(0).getDetails().get(0).getSourceName()).isEqualTo("自家製タルタルソースだく");
		assertThat(orders.get(0).getDetails().get(1).getZangiCount()).isZero();
	}

	@Test
	@DisplayName("調理完了時にモバイル注文は完成通知し、店頭注文は受取済にする")
	void completeCook() {
		when(repository.getOrderById(1)).thenReturn(Map.of("mail", "guest@example.com", "order_number", "M0001"));
		when(repository.getOrderById(2)).thenReturn(Map.of("mail", "", "order_number", "M0002"));
		when(repository.getOrderById(3)).thenReturn(null);
		Map<String, Object> orderWithoutMail = new HashMap<>();
		orderWithoutMail.put("mail", null);
		orderWithoutMail.put("order_number", "M0004");
		when(repository.getOrderById(4)).thenReturn(orderWithoutMail);
		when(repository.getOrderById(5)).thenReturn(Map.of("mail", "guest@example.com", "order_number", "T0005"));
		Map<String, Object> orderWithoutNumber = new HashMap<>();
		orderWithoutNumber.put("mail", "guest@example.com");
		orderWithoutNumber.put("order_number", null);
		when(repository.getOrderById(6)).thenReturn(orderWithoutNumber);

		service.completeCook(1);
		service.completeCook(2);
		service.completeCook(3);
		service.completeCook(4);
		service.completeCook(5);
		service.completeCook(6);

		verify(repository).updateStatus(1, "完成");
		verify(repository).updateStatus(2, "完成");
		verify(repository).updateStatus(4, "完成");
		verify(repository).updateStatus(5, "受取済");
		verify(repository).updateStatus(6, "受取済");
		verify(repository, never()).updateStatus(3, "完成");
		verify(repository, never()).updateStatus(3, "受取済");
		verify(notificationService).sendOrderCompleteNotification(1);
		verify(notificationService).sendOrderCompleteNotification(2);
		verify(notificationService).sendOrderCompleteNotification(4);
		verify(notificationService, never()).sendOrderCompleteNotification(3);
		verify(notificationService, never()).sendOrderCompleteNotification(5);
		verify(notificationService, never()).sendOrderCompleteNotification(6);
	}

	@Test
	@DisplayName("未実装の集約APIはUnsupportedOperationExceptionを返す")
	void getKitchenOrdersGrouped() {
		assertThatThrownBy(() -> service.getKitchenOrdersGrouped())
				.isInstanceOf(UnsupportedOperationException.class)
				.hasMessage("Unimplemented method 'getKitchenOrdersGrouped'");
	}

	private Map<String, Object> row(Object orderId, String goodsId, String goodsName,
			Object customId, Object count) {
		Map<String, Object> row = new HashMap<>();
		row.put("order_id", orderId);
		row.put("order_number", "M" + orderId);
		row.put("get_time", Timestamp.valueOf("2026-10-04 12:00:00"));
		row.put("register_time", Timestamp.valueOf("2026-10-04 11:00:00"));
		row.put("memo", "memo");
		row.put("status", "調理中");
		row.put("goods_id", goodsId);
		row.put("goods_name", goodsName);
		row.put("custom_id", customId);
		row.put("count", count);
		row.put("plus_zangi_count", 0);
		return row;
	}
}