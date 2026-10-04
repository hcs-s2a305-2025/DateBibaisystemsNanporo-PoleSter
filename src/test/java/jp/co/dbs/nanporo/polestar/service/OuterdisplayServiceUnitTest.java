package jp.co.dbs.nanporo.polestar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDate;
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

import jp.co.dbs.nanporo.polestar.repository.OuterdisplayRepository;

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
				row(1, "B001", "遅い商品", Timestamp.valueOf(today.atTime(14, 0)), "受付", 700),
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
				row(3, "B003", "受取商品", Timestamp.valueOf(today.atTime(12, 0)), "受取可", 300),
				row(4, "B004", "呼出商品", Timestamp.valueOf(today.atTime(13, 0)), "呼び出し中", 400),
				row(5, "B005", "その他", Timestamp.valueOf(today.atTime(14, 0)), "完了", 500)));

		var response = service.getDisplayOrders();

		assertThat(response.getCookingOrders()).hasSize(2);
		assertThat(response.getCookingOrders()).extracting(order -> order.getOrder().getStatus())
				.containsExactly("受付", "調理中");
		assertThat(response.getCallingOrders()).hasSize(2);
		assertThat(response.getCallingOrders()).extracting(order -> order.getOrder().getStatus())
				.containsExactly("受取可", "呼び出し中");
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