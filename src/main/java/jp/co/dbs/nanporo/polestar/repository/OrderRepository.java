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

import jp.co.dbs.nanporo.polestar.entity.OrderDetailEntity;
import jp.co.dbs.nanporo.polestar.entity.OrderEntity;

@Repository
public class OrderRepository {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    // --- SQL定義 ---
    // 指定日の最大注文番号を取得（日ごとリセット用）
    private static final String SELECT_MAX_ORDER_NUMBER = 
            "SELECT COALESCE(MAX(CAST(order_number AS INT)), 0) FROM order_t "
            + "WHERE get_time >= :startOfDay AND get_time < :endOfDay";

    // 注文トラン登録
    private static final String INSERT_ORDER = 
            "INSERT INTO order_t (order_number, get_time, mail, register_time, sum_money, memo, status) "
            + "VALUES (:orderNumber, :getTime, :mail, CURRENT_TIMESTAMP, :sumMoney, :memo, :status)";

    // 注文明細トラン登録
    private static final String INSERT_ORDER_DETAIL = 
            "INSERT INTO order_detail_t (order_id, order_count, goods_id, set_goods_id, count, plus_zangi_count, custom_id) "
            + "VALUES (:orderId, :orderCount, :goodsId, :setGoodsId, :count, :plusZangiCount, :customId)";

    // 注文一覧取得（ユーザー用）
    private static final String SELECT_ORDERS_BY_MAIL = 
            "SELECT * FROM order_t WHERE mail = :mail ORDER BY get_time DESC";

    // 注文1件の詳細取得
    private static final String SELECT_ORDER_BY_ID = 
            "SELECT * FROM order_t WHERE order_id = :orderId";

    // 注文明細リストの取得
    private static final String SELECT_ORDER_DETAILS_BY_ORDER_ID = 
            "SELECT * FROM order_detail_t WHERE order_id = :orderId ORDER BY order_count";

    // 注文トラン更新（内容変更）
    private static final String UPDATE_ORDER = 
            "UPDATE order_t SET get_time = :getTime, sum_money = :sumMoney, memo = :memo "
            + "WHERE order_id = :orderId AND mail = :mail";

    // 注文ステータス更新
    private static final String UPDATE_ORDER_STATUS = 
            "UPDATE order_t SET status = :status WHERE order_id = :orderId";

    // 注文明細削除（更新・削除時に使用）
    private static final String DELETE_ORDER_DETAILS = 
            "DELETE FROM order_detail_t WHERE order_id = :orderId";

    // 注文トラン削除
    private static final String DELETE_ORDER = 
            "DELETE FROM order_t WHERE order_id = :orderId AND mail = :mail";


    // --- メソッド実装 ---

    /** 指定日の最大の注文番号（数値）を取得します */
    public int getMaxOrderNumberByDate(LocalDateTime startOfDay, LocalDateTime endOfDay) {
        Map<String, Object> params = new HashMap<>();
        params.put("startOfDay", startOfDay);
        params.put("endOfDay", endOfDay);
        
        Integer maxNo = jdbc.queryForObject(SELECT_MAX_ORDER_NUMBER, params, Integer.class);
        return maxNo != null ? maxNo : 0;
    }

    /** 注文親データ（order_t）を登録し、自動採番された order_id を返します */
    public int insertOrder(OrderEntity order) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("orderNumber", order.getOrderNumber())
                .addValue("getTime", order.getGetTime())
                .addValue("mail", order.getMail())
                .addValue("sumMoney", order.getSumMoney())
                .addValue("memo", order.getMemo())
                .addValue("status", order.getStatus() != null ? order.getStatus() : "受付");

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(INSERT_ORDER, params, keyHolder, new String[] { "order_id" });
        return keyHolder.getKey().intValue();
    }

    /** 注文明細データ（order_detail_t）を1件登録します */
    public int insertOrderDetail(OrderDetailEntity detail) {
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", detail.getOrderId());
        params.put("orderCount", detail.getOrderCount());
        params.put("goodsId", detail.getGoodsId());
        params.put("setGoodsId", detail.getSetGoodsId());
        params.put("count", detail.getCount());
        params.put("plusZangiCount", detail.getPlusZangiCount() != null ? detail.getPlusZangiCount() : 0);
        params.put("customId", detail.getCustomId());

        return jdbc.update(INSERT_ORDER_DETAIL, params);
    }

    /** ユーザーの注文一覧を取得します */
    public List<Map<String, Object>> getOrdersByMail(String mail) {
        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);
        return jdbc.queryForList(SELECT_ORDERS_BY_MAIL, params);
    }

    /** 注文1件の情報を取得します */
    public Map<String, Object> getOrderById(int orderId) {
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", orderId);
        try {
            return jdbc.queryForMap(SELECT_ORDER_BY_ID, params);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /** 注文に紐づく明細一覧を取得します */
    public List<Map<String, Object>> getOrderDetailsByOrderId(int orderId) {
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", orderId);
        return jdbc.queryForList(SELECT_ORDER_DETAILS_BY_ORDER_ID, params);
    }

    /** 注文内容（受取日時・金額・メモ）を更新します */
    public int updateOrder(OrderEntity order) {
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", order.getOrderId());
        params.put("mail", order.getMail());
        params.put("getTime", order.getGetTime());
        params.put("sumMoney", order.getSumMoney());
        params.put("memo", order.getMemo());

        return jdbc.update(UPDATE_ORDER, params);
    }

    /** 注文ステータスのみ更新します */
    public int updateOrderStatus(int orderId, String status) {
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", orderId);
        params.put("status", status);

        return jdbc.update(UPDATE_ORDER_STATUS, params);
    }

    /** 指定された注文IDの明細をすべて削除します（注文内容変更時の再登録用） */
    public int deleteOrderDetails(int orderId) {
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", orderId);

        return jdbc.update(DELETE_ORDER_DETAILS, params);
    }

    /** 注文データ（order_t）を削除します */
    public int deleteOrder(int orderId, String mail) {
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", orderId);
        params.put("mail", mail);

        return jdbc.update(DELETE_ORDER, params);
    }
}