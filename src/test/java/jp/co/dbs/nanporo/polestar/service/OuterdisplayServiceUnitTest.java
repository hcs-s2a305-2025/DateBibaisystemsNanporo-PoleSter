package jp.co.dbs.nanporo.polestar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import jp.co.dbs.nanporo.polestar.repository.OuterdisplayRepository;
import jp.co.dbs.nanporo.polestar.response.ActiveOrderResponse;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class OuterdisplayServiceUnitTest {

	@Mock
	private OuterdisplayRepository repository;

	@InjectMocks
	private OuterdisplayService service;

	@Test
	@DisplayName("本日の注文を受取時刻順に並べ、文字列時刻も変換する")
	void getActiveOrders() {
		LocalDate today = LocalDate.now();
		when(repository.getAllActiveOrders()).thenReturn(List.of(
				row(1, "B001", "遅い商品", Timestamp.valueOf(today.atTime(14, 0)), "受付", 700L),
				row(2, "B002", "早い商品", Timestamp.valueOf(today.atTime(12, 0)).toString(), "調理中", "900"),
				row(3, null, null, Timestamp.valueOf(today.atTime(13, 0)), "受取可", null),
				row(4, "B004", "昨日の商品", Timestamp.valueOf(today.minusDays(1).atTime(12, 0)), "呼び出し中", 500),
				row(5, "B005", "日時なし", null, "受付", 500)));

		var orders = service.getActiveOrders();

		assertThat(orders).hasSize(3);
		assertThat(orders).extracting(order -> order.getOrder().getOrderId()).containsExactly(2, 3, 1);
		assertThat(orders.get(0).getOrder().getGetTime()).isEqualTo(Timestamp.valueOf(today.atTime(12, 0)));
		assertThat(orders.get(1).getOrder().getSumMoney()).isZero();
		assertThat(orders.get(1).getDetails()).isEmpty();
	}

	@Test
	@DisplayName("注文状態を調理中と呼び出し中へ振り分ける")
	void getDisplayOrders() {
		LocalDate today = LocalDate.now();
		when(repository.getAllActiveOrders()).thenReturn(List.of(
				row(1, "B001", "受付商品", Timestamp.valueOf(today.atTime(10, 0)), "受付", 100),
				row(2, "B002", "調理商品", Timestamp.valueOf(today.atTime(11, 0)), "調理中", 200),
				row(3, "B003", "完成商品", Timestamp.valueOf(today.atTime(12, 0)), "完成", 300),
				row(4, "B004", "受取商品", Timestamp.valueOf(today.atTime(13, 0)), "受取済", 400),
				row(5, "B005", "その他", Timestamp.valueOf(today.atTime(14, 0)), "キャンセル", 500)));

		var response = service.getDisplayOrders();

		assertThat(response.getCookingOrders()).hasSize(2);
		assertThat(response.getCookingOrders()).extracting(order -> order.getOrder().getStatus())
				.containsExactly("受付", "調理中");
		assertThat(response.getCallingOrders()).hasSize(2);
		assertThat(response.getCallingOrders()).extracting(order -> order.getOrder().getStatus())
				.containsExactly("完成", "受取済");
	}

	@Test
	@DisplayName("注文データがない応答はディスプレイのどちらの一覧にも含めない")
	void getDisplayOrdersWithMissingOrder() {
		OuterdisplayService serviceSpy = spy(service);
		doReturn(List.of(new ActiveOrderResponse())).when(serviceSpy).getActiveOrders();

		var response = serviceSpy.getDisplayOrders();

		assertThat(response.getCookingOrders()).isEmpty();
		assertThat(response.getCallingOrders()).isEmpty();
	}

	@Test
	@DisplayName("5分を過ぎた呼び出し注文を除外し1時間経過したキャッシュを削除する")
	void getDisplayOrdersExpiresCallingOrdersAndCache() {
		LocalDate today = LocalDate.now();
		when(repository.getAllActiveOrders()).thenReturn(List.of(
				row(7, "B007", "期限切れ注文", Timestamp.valueOf(today.atTime(12, 0)), "完成", 700)));

		@SuppressWarnings("unchecked")
		Map<Integer, LocalDateTime> startTimes = (Map<Integer, LocalDateTime>) ReflectionTestUtils
				.getField(service, "callingStartTimeMap");
		startTimes.put(7, LocalDateTime.now().minusMinutes(6));
		startTimes.put(99, LocalDateTime.now().minusHours(2));

		var response = service.getDisplayOrders();

		assertThat(response.getCallingOrders()).isEmpty();
		assertThat(startTimes).containsKey(7).doesNotContainKey(99);
	}

	@Test
	@DisplayName("注文応答に注文データがない場合は本日の注文一覧から除外する")
	void getActiveOrdersWithMissingOrder() {
		LocalDate today = LocalDate.now();
		when(repository.getAllActiveOrders()).thenReturn(List.of(
				row(6, "B006", "商品", Timestamp.valueOf(today.atTime(12, 0)), "受付", 500)));

		try (MockedConstruction<ActiveOrderResponse> responses = mockConstruction(ActiveOrderResponse.class,
				(mock, context) -> {
					when(mock.getOrder()).thenReturn(null);
					when(mock.getDetails()).thenReturn(new ArrayList<>());
				})) {
			assertThat(service.getActiveOrders()).isEmpty();
			assertThat(responses.constructed()).hasSize(1);
		}
	}

	private Map<String, Object> row(Integer id, String goodsId, String goodsName,
			Object getTime, String status, Object sumMoney) {
		Map<String, Object> row = new HashMap<>();
		row.put("order_id", id);
		row.put("order_number", "M" + id);
		row.put("get_time", getTime);
		row.put("mail", "guest@example.com");
		row.put("sum_money", sumMoney);
		row.put("memo", "memo");
		row.put("status", status);
		row.put("goods_id", goodsId);
		row.put("goods_name", goodsName);
		row.put("count", 1);
		return row;
	}
}