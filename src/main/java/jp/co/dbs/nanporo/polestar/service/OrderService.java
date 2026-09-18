package jp.co.dbs.nanporo.polestar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.co.dbs.nanporo.polestar.data.OrderData;
import jp.co.dbs.nanporo.polestar.data.OrderDetailData;
import jp.co.dbs.nanporo.polestar.entity.OrderDetailEntity;
import jp.co.dbs.nanporo.polestar.entity.OrderEntity;
import jp.co.dbs.nanporo.polestar.repository.OrderRepository;
import jp.co.dbs.nanporo.polestar.request.OrderDetailRequest;
import jp.co.dbs.nanporo.polestar.request.OrderRegisterRequest;
import jp.co.dbs.nanporo.polestar.response.ActiveOrderResponse;
import jp.co.dbs.nanporo.polestar.response.OrderHistoryResponse;
import jp.co.dbs.nanporo.polestar.response.OrderRegisterResponse;

@Service 
public class OrderService {

    @Autowired 
    private OrderRepository orderRepository;

    // 文字列や数値型を安全に Integer へ変換するメソッド
    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }

    /**
     * 新規注文を登録します。
     * 日ごとの注文番号の自動採番と、親データ・明細データの登録を同一トランザクションで実行します。
     */
    @Transactional 
    public OrderRegisterResponse insertOrder(OrderRegisterRequest request) {
        // リクエストからデータへ変換
        OrderData data = new OrderData();

        // リクエストの値を設定
        data.setGetTime(java.sql.Timestamp.valueOf(java.time.LocalDateTime.parse(request.getGetTime())));
        data.setMail(request.getMail());
        data.setRegisterTime(java.sql.Timestamp.valueOf(java.time.LocalDateTime.parse(request.getRegisterTime())));
        data.setSumMoney(request.getSumMoney());
        data.setMemo(request.getMemo());
        data.setStatus(request.getStatus());

        // 1.予約・店頭注文の識別情報を設定
        data.setOrderType(request.getOrderType());

        // 2. 注文親データ（order_t）を登録
        // ※ 採番処理（order_id / order_number の生成）は orderRepository.insertOrder 内で実行
        int orderId = orderRepository.insertOrder(data);
        if (orderId <= 0) {
            throw new RuntimeException("注文情報の登録に失敗しました。");
        }

        // 3. 注文明細データ（order_detail_t）をループして登録
        List<OrderDetailRequest> detailList = request.getOrderDetails();
        if (detailList != null && !detailList.isEmpty()) {
            int orderCount = 1; // 明細内の連番 (1, 2, 3...)
            for (OrderDetailRequest detailRequest : detailList) {
                // 親の orderId と明細連番をセット
                OrderDetailData detail = new OrderDetailData();
                detail.setOrderId(orderId);
                detail.setOrderCount(Integer.valueOf(orderCount++));
                detail.setGoodsId(detailRequest.getGoodsId());
                detail.setSetGoodsId(detailRequest.getSetGoodsId());
                detail.setCount(detailRequest.getCount());
                detail.setPlusZangiCount(detailRequest.getPlusZangiCount());
                detail.setCustomId(detailRequest.getCustomId());

                int insertedDetail = orderRepository.insertOrderDetail(detail);
                if (insertedDetail != 1) {
                    throw new RuntimeException("注文明細の登録に失敗しました。");
                }
            }
        }

        // 4. レスポンスの生成
        OrderRegisterResponse response = new OrderRegisterResponse();
        response.setOrderId(orderId);

        return response;
    }

    /**
     * ログインユーザーの予約中の注文（受付・調理中・完成）を取得します。
     */
    public List<ActiveOrderResponse> getActiveOrders(String mail) {
        List<Map<String, Object>> rows = orderRepository.getActiveOrdersByMail(mail);
        Map<Integer, ActiveOrderResponse> map = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            // Integer orderId = (Integer) row.get("order_id");
            Integer orderId = toInteger(row.get("order_id"));

            // 親データの生成（初回のみ）
            ActiveOrderResponse response = map.computeIfAbsent(orderId, id -> {
                ActiveOrderResponse res = new ActiveOrderResponse();
                
                OrderData order = new OrderData();
                order.setOrderId(id);
                order.setOrderNumber((String) row.get("order_number"));
                order.setGetTime((java.sql.Timestamp) row.get("get_time"));
                order.setMail((String) row.get("mail"));
                // order.setSumMoney((Integer) row.get("sum_money"));
                Integer sumMoney = toInteger(row.get("sum_money"));
                order.setSumMoney(sumMoney != null ? sumMoney : 0);
                order.setMemo((String) row.get("memo"));
                order.setStatus((String) row.get("status"));
                
                res.setOrder(order);
                res.setDetails(new ArrayList<>());
                return res;
            });

            // 明細データの追加
            if (row.get("goods_id") != null) {
                ActiveOrderResponse.OrderDetailItem item = new ActiveOrderResponse.OrderDetailItem();
                item.setGoodsId((String) row.get("goods_id"));
                item.setGoodsName((String) row.get("goods_name"));
                // item.setCount((Integer) row.get("count"));
                item.setCount(toInteger(row.get("count")));
                response.getDetails().add(item);
            }
        }

        // 表示用の商品名文字列（カンマ区切り）を生成
        for (ActiveOrderResponse res : map.values()) {
            String names = res.getDetails().stream()
                    .map(ActiveOrderResponse.OrderDetailItem::getGoodsName)
                    .collect(Collectors.joining(", "));
            res.setGoodsNames(names);
        }

        return new ArrayList<>(map.values());
    }

    /**
     * ログインユーザーの予約履歴一覧を取得します。
     */
    public List<OrderHistoryResponse> getOrderHistory(String mail) {
        List<Map<String, Object>> rows = orderRepository.getOrderHistoryByMail(mail);
        Map<Integer, OrderHistoryResponse> historyMap = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月d日");

        for (Map<String, Object> row : rows) {
            Integer orderId = (Integer) row.get("order_id");

            // 注文親データの生成（初回のみ）
            OrderHistoryResponse response = historyMap.computeIfAbsent(orderId, id -> {
                OrderHistoryResponse res = new OrderHistoryResponse();
                res.setOrderId(id);
                res.setOrderNumber((String) row.get("order_number"));
                res.setSumMoney((Integer) row.get("sum_money"));
                
                if (row.get("get_time") != null) {
                    java.sql.Timestamp getTime = (java.sql.Timestamp) row.get("get_time");
                    res.setFormattedDate(getTime.toLocalDateTime().format(formatter));
                }
                res.setItems(new ArrayList<>());
                return res;
            });

            // 明細データの追加（マスタ等から名称・価格を取得する想定）
            if (row.get("goods_id") != null) {
                OrderHistoryResponse.OrderDetailItem item = new OrderHistoryResponse.OrderDetailItem();
                // ※実際の開発では goods_id / custom_id から商品マスタを参照してセットします
                item.setGoodsName("元祖ザンギ弁当 (5個)"); 
                item.setGoodsPrice(780);
                item.setCustomName("タルタルソース");
                item.setCustomPrice(50);
                item.setCount((Integer) row.get("count"));
                
                response.getItems().add(item);
            }
        }

        return new ArrayList<>(historyMap.values());
    }
    
}
