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

    private static final String SELECT_KITCHEN_ORDERS = 
            "SELECT o.order_id, o.order_number, o.get_time, o.register_time, o.memo, o.status, "
            + "od.order_count, od.goods_id, od.set_goods_id, g.goods_name, od.count, od.plus_zangi_count, "
            + "od.custom_id, od.source_custom_id, c.goods_name AS custom_name, "
            + "sg.goods_name AS set_goods_name, sg.price AS set_goods_price "
            + "FROM order_t o "
            + "LEFT JOIN order_detail_t od ON o.order_id = od.order_id "
            + "LEFT JOIN goods_m g ON od.goods_id = g.goods_id "
            + "LEFT JOIN custom_m c ON od.custom_id = c.custom_id "
            + "LEFT JOIN set_goods_m sg ON od.set_goods_id = sg.set_goods_id "
            + "WHERE o.status IN ('受付', '調理中') "
            + "AND CAST(o.get_time AS DATE) = CURRENT_DATE "
            + "ORDER BY o.get_time ASC, o.order_id ASC, od.order_count ASC";

    // private static final String UPDATE_STATUS_TO_READY = 
    //         "UPDATE order_t SET status = '受取可' WHERE order_id = :orderId";
    // ステータスを動的に更新するSQL
    private static final String UPDATE_STATUS = 
            "UPDATE order_t SET status = :status WHERE order_id = :orderId";

    // 対象注文の mail と order_number を取得
    private static final String SELECT_ORDER_BY_ID = 
            "SELECT mail, order_number FROM order_t WHERE order_id = :orderId";

    // notice_t への通知データ挿入
    private static final String INSERT_NOTICE = 
            "INSERT INTO notice_t (mail, register_time, content) "
            + "VALUES (:mail, CURRENT_TIMESTAMP, :content)";

    public List<Map<String, Object>> getKitchenOrders() {
        return jdbc.queryForList(SELECT_KITCHEN_ORDERS, Map.of());
    }

    public Map<String, Object> getOrderById(Integer orderId) {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("orderId", orderId);
        List<Map<String, Object>> list = jdbc.queryForList(SELECT_ORDER_BY_ID, params);
        return list.isEmpty() ? null : list.get(0);
    }

    // public void updateStatusToReady(Integer orderId) {
    //     MapSqlParameterSource params = new MapSqlParameterSource().addValue("orderId", orderId);
    //     jdbc.update(UPDATE_STATUS_TO_READY, params);
    // }
    // 指定したステータスに更新するメソッド
    public void updateStatus(Integer orderId, String status) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("orderId", orderId)
                .addValue("status", status);
        jdbc.update(UPDATE_STATUS, params);
    }

    public void insertNotice(String mail, String content) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("mail", mail)
                .addValue("content", content);
        jdbc.update(INSERT_NOTICE, params);
    }
}