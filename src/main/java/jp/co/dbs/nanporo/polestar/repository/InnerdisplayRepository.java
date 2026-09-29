package jp.co.dbs.nanporo.polestar.repository;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InnerdisplayRepository {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    // 調理対象の注文一覧取得（'受付' または '調理中' かつ 本日分）
    private static final String SELECT_KITCHEN_ORDERS = 
            "SELECT o.order_id, o.order_number, o.get_time, o.memo, o.status, "
            + "od.order_count, od.goods_id, g.goods_name, od.count, od.custom_id, c.goods_name AS custom_name "
            + "FROM order_t o "
            + "LEFT JOIN order_detail_t od ON o.order_id = od.order_id "
            + "LEFT JOIN goods_m g ON od.goods_id = g.goods_id "
            + "LEFT JOIN custom_m c ON od.custom_id = c.custom_id "
            + "WHERE o.status IN ('受付', '調理中') "
            + "AND CAST(o.get_time AS DATE) = CURRENT_DATE "
            + "ORDER BY o.get_time ASC, o.order_id ASC, od.order_count ASC";

    // ステータスを '受取可' に更新
    private static final String UPDATE_STATUS_TO_READY = 
            "UPDATE order_t SET status = '受取可' WHERE order_id = :orderId";

    /**
     * 本日の調理待ち注文一覧を取得
     */
    public List<Map<String, Object>> getKitchenOrders() {
        return jdbc.queryForList(SELECT_KITCHEN_ORDERS, Map.of());
    }

    /**
     * 調理完了時にステータスを '受取可' に更新
     */
    public void updateStatusToReady(Integer orderId) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("orderId", orderId);
        jdbc.update(UPDATE_STATUS_TO_READY, params);
    }
}