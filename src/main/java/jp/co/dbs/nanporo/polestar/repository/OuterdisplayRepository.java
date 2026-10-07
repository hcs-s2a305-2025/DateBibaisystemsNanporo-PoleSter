package jp.co.dbs.nanporo.polestar.repository;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OuterdisplayRepository {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    private static final String SELECT_ALL_ACTIVE_ORDERS = 
            "SELECT o.order_id, o.order_number, o.get_time, o.mail, o.sum_money, o.memo, o.status, "
            + "g.goods_id, g.goods_name, od.count "
            + "FROM order_t o "
            + "LEFT JOIN order_detail_t od ON o.order_id = od.order_id " // ★注文明細テーブルを結合
            + "LEFT JOIN goods_m g ON od.goods_id = g.goods_id "         // ★商品マスタと結合
            + "WHERE CAST(o.get_time AS DATE) = CURRENT_DATE "
            + "ORDER BY o.get_time ASC";

    public List<Map<String, Object>> getAllActiveOrders() {
        return jdbc.queryForList(SELECT_ALL_ACTIVE_ORDERS, Map.of());
    }
}