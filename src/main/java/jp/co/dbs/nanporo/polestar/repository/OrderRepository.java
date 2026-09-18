package jp.co.dbs.nanporo.polestar.repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import jp.co.dbs.nanporo.polestar.data.OrderData;
import jp.co.dbs.nanporo.polestar.data.OrderDetailData;
import jp.co.dbs.nanporo.polestar.entity.OrderDetailEntity;
import jp.co.dbs.nanporo.polestar.entity.OrderEntity;

@Repository
public class OrderRepository {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    // 注文親データを追加するSQL
    private static final String INSERT_ORDER = 
            "INSERT INTO order_t ("
            + "order_number, get_time, mail, register_time, sum_money, memo, status"
            + ") VALUES ("
            + ":orderNumber, :getTime, :mail, :registerTime, :sumMoney, :memo, :status"
            + ")";

    /**
     * 注文情報をデータベースに登録し、自動採番された order_id を返します。
     */
    public int insertOrder(OrderData data) {
        // 1. 注文区分（予約/店頭）に応じた本日の注文番号を自動採番
        String orderNumber = generateOrderNumber(data.getOrderType());
        data.setOrderNumber(orderNumber);

        // 2. パラメータの設定と KeyHolder による自動生成IDの取得準備
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("orderNumber", data.getOrderNumber());
        params.addValue("getTime", data.getGetTime());
        params.addValue("mail", data.getMail());
        params.addValue("registerTime", data.getRegisterTime());
        params.addValue("sumMoney", data.getSumMoney());
        params.addValue("memo", data.getMemo());
        params.addValue("status", data.getStatus());

        KeyHolder keyHolder = new GeneratedKeyHolder();

        // SQLを実行して生成された order_id を取得
        jdbc.update(INSERT_ORDER, params, keyHolder, new String[] { "order_id" });

        // 自動採番された order_id を返す
        Number key = keyHolder.getKey();
        return (key != null) ? key.intValue() : 0;
    }
    
    // モバイル予約の当日の最大注文番号（M0001〜M9999）を取得するSQL
    private static final String SELECT_MAX_MOBILE_ORDER_NUMBER = 
            "SELECT order_number FROM order_t "
            + "WHERE order_number LIKE 'M%' "
            + "  AND register_time >= CURRENT_DATE AND register_time < CURRENT_DATE + INTERVAL '1 day' "
            + "ORDER BY order_number DESC LIMIT 1";

    // 店頭注文の当日の最大注文番号（0001〜9999）を取得するSQL
    private static final String SELECT_MAX_STORE_ORDER_NUMBER = 
            "SELECT order_number FROM order_t "
            + "WHERE order_number NOT LIKE 'M%' "
            + "  AND register_time >= CURRENT_DATE AND register_time < CURRENT_DATE + INTERVAL '1 day' "
            + "ORDER BY order_number DESC LIMIT 1";

    /**
     * 注文番号の自動採番ロジック
     * 店頭注文: 0001〜9999
     * モバイル予約: M0001〜M9999
     */
    private String generateOrderNumber(String orderType) {
        boolean isMobile = "RESERVATION".equalsIgnoreCase(orderType) || "MOBILE".equalsIgnoreCase(orderType);
        String sql = isMobile ? SELECT_MAX_MOBILE_ORDER_NUMBER : SELECT_MAX_STORE_ORDER_NUMBER;

        List<String> resultList = jdbc.queryForList(sql, new HashMap<>(), String.class);

        int nextSeq = 1;
        if (!resultList.isEmpty() && resultList.get(0) != null) {
            String maxOrderNum = resultList.get(0); // 例: "M0005" や "0005"
            String numStr = maxOrderNum.replace("M", "");
            nextSeq = Integer.parseInt(numStr) + 1;
        }

        // 4桁数字のゼロ埋め（例: 1 -> "0001"）
        String formattedSeq = String.format("%04d", nextSeq);

        // モバイル予約の場合は先頭に "M" を付与
        return isMobile ? "M" + formattedSeq : formattedSeq;
    }

    // 注文明細データを追加するSQL
    private static final String INSERT_ORDER_DETAIL = 
            "INSERT INTO order_detail_t ("
            + "order_id, order_count, goods_id, set_goods_id, count, plus_zangi_count, custom_id"
            + ") VALUES ("
            + ":orderId, :orderCount, :goodsId, :setGoodsId, :count, :plusZangiCount, :customId"
            + ")";

    /**
     * 注文明細情報をデータベースに登録します。
     */
    public int insertOrderDetail(OrderDetailData detail) {
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", detail.getOrderId());
        params.put("orderCount", detail.getOrderCount());
        params.put("goodsId", detail.getGoodsId());
        params.put("setGoodsId", detail.getSetGoodsId());
        params.put("count", detail.getCount());
        params.put("plusZangiCount", detail.getPlusZangiCount());
        params.put("customId", detail.getCustomId());

        return jdbc.update(INSERT_ORDER_DETAIL, params);
    }

    // 注文1件の詳細情報を取得するSQL
    private static final String SELECT_ORDER_DETAIL = 
            "SELECT * FROM order_t WHERE order_id = :orderId";

    /**
     * 注文IDを条件に、指定された1件の注文詳細情報を取得します。
     */
    public Map<String, Object> getOrderById(int orderId) {
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", orderId);

        try {
            return jdbc.queryForMap(SELECT_ORDER_DETAIL, params);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // ユーザーの「予約中（受付・調理中・完成）」の注文一覧を取得するSQL
    // private static final String SELECT_ACTIVE_ORDERS_BY_MAIL = 
    //         "SELECT * FROM order_t "
    //         + "WHERE mail = :mail "
    //         + "  AND status IN ('受付', '調理中', '完成') "
    //         + "ORDER BY get_time ASC";
    private static final String SELECT_ACTIVE_ORDERS_WITH_DETAILS = 
            "SELECT o.order_id, o.order_number, o.get_time, o.mail, o.sum_money, o.memo, o.status, "
            + "       d.order_count, d.goods_id, d.count, g.goods_name "
            + "FROM order_t o "
            + "LEFT JOIN order_detail_t d ON o.order_id = d.order_id "
            + "LEFT JOIN goods_m g ON d.goods_id = g.goods_id "
            + "WHERE o.mail = :mail AND o.status IN ('受付', '調理中', '完成') "
            + "ORDER BY o.get_time ASC, d.order_count ASC";

    /**
     * ログインユーザーの予約中（受付・調理中・完成）の注文一覧を取得します。
     */
    public List<Map<String, Object>> getActiveOrdersByMail(String mail) {
        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);

        return jdbc.queryForList(SELECT_ACTIVE_ORDERS_WITH_DETAILS, params);
    }

    // ユーザーの予約履歴一覧（降順）を取得するSQL
    private static final String SELECT_ORDER_HISTORY_BY_MAIL = 
            "SELECT o.order_id, o.order_number, o.get_time, o.sum_money, "
            + "       d.goods_id, d.count, d.custom_id "
            + "FROM order_t o "
            + "LEFT JOIN order_detail_t d ON o.order_id = d.order_id "
            + "WHERE o.mail = :mail "
            + "ORDER BY o.get_time DESC";

    /**
     * メールアドレスから過去の注文履歴一覧を取得します。
     */
    public List<Map<String, Object>> getOrderHistoryByMail(String mail) {
        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);

        return jdbc.queryForList(SELECT_ORDER_HISTORY_BY_MAIL, params);
    }

}