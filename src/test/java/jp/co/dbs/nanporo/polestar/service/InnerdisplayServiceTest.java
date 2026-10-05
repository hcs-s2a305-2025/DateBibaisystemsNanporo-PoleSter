package jp.co.dbs.nanporo.polestar.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class InnerdisplayServiceTest {

	@Autowired
	InnerdisplayService service;
	
	@Autowired 
    private NamedParameterJdbcTemplate jdbc;
	
	@Test
	@DisplayName("注文商品をリスト形式で返す")
	void testGetKitchenOrdersGrouped() {
		// 今日のデータを作成する
		String updateSql = "UPDATE order_t SET get_time = CURRENT_TIMESTAMP, status = '受付' WHERE order_id = 2";
		jdbc.update(updateSql, Map.of());
		
		// 商品名がNULLのデータを作成する
		String insertSql = "INSERT INTO order_t (order_id, order_number, mail, get_time, register_time, sum_money, memo, status) "
				+ " VALUES (3, 'M002', 'koserayuuki@example.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 'テスト', '受付')";
		jdbc.update(insertSql, Map.of());
		
		// テスト対象メソッドを実行する
		List<Map<String, Object>> response = service.getKitchenOrdersGrouped();
		
		// 実行結果の確認
		assertThat(response).isNotEmpty();
	}

	@Test
	@DisplayName("オーダーIDを入力しステータス更新と通知を作成する")
	void testCompleteCook() {
		// 今日のデータを作成する
		String updateSql = "UPDATE order_t SET get_time = CURRENT_TIMESTAMP, status = '受付' WHERE order_id = 2";
		jdbc.update(updateSql, Map.of());
		
		// 渡したい引数を設定
		int orderId = 2;
		
		// テスト対象メソッドを実行する
		service.completeCook(orderId);
		
		// 実行結果の確認
		String sql = "SELECT status FROM order_t WHERE order_id = :orderId";
		Map<String, Object> params = Map.of("orderId", orderId);
		Map<String, Object> result = jdbc.queryForMap(sql, params);
		
		assertThat(result.get("status")).isEqualTo("受取可");
	}
}
