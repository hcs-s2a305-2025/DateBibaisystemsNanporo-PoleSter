package jp.co.dbs.nanporo.polestar.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.dbs.nanporo.polestar.data.OrderData;
import jp.co.dbs.nanporo.polestar.repository.OuterdisplayRepository;
import jp.co.dbs.nanporo.polestar.response.ActiveOrderResponse;
import jp.co.dbs.nanporo.polestar.response.OuterdisplayResponse;

@Service 
public class OuterdisplayService {

    @Autowired
    private OuterdisplayRepository outerdisplayRepository;

    // お呼び出し開始時刻を保持するスレッドセーフなマップ（注文ID -> お呼び出し開始日時）
    private final Map<Integer, LocalDateTime> callingStartTimeMap = new ConcurrentHashMap<>();

    // 数値型・文字列型を安全に Integer へ変換するメソッド
    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    /**
     * 外出しディスプレイ用メイン処理
     * 画像のように「調理中」と「お呼び出し中」に分けたデータを返却します。
     */
    public OuterdisplayResponse getDisplayOrders() {
        // 全件のアクティブ注文（受取日時順）を取得
        List<ActiveOrderResponse> allActiveOrders = getActiveOrders();

        OuterdisplayResponse response = new OuterdisplayResponse();

        // 1. 画像左側: 「調理中」のリストを抽出（ステータスが '受付' または '調理中' の注文）
        List<ActiveOrderResponse> cookingList = allActiveOrders.stream()
                .filter(o -> o.getOrder() != null && 
                            ("受付".equals(o.getOrder().getStatus()) || "調理中".equals(o.getOrder().getStatus())))
                .collect(Collectors.toList());

        // 2. 予約注文は「完成」の間、店頭注文は「受取済」の後も呼び出し中に表示する
        LocalDateTime now = LocalDateTime.now();
        List<ActiveOrderResponse> callingList = allActiveOrders.stream()
                .filter(o -> o.getOrder() != null && (
                        "完成".equals(o.getOrder().getStatus())
                                || ("受取済".equals(o.getOrder().getStatus())
                                        && !isMobileOrder(o.getOrder().getOrderNumber()))))
                .filter(o -> {
                    Integer orderId = o.getOrder().getOrderId();
                    // 初めてお呼び出し対象になった注文の時刻を記録（すでに存在する場合は保持）
                    callingStartTimeMap.putIfAbsent(orderId, now);
                    
                    LocalDateTime startTime = callingStartTimeMap.get(orderId);
                    
                    // お呼び出し開始から 5分（300秒）以内のみ表示リストに含める
                    return Duration.between(startTime, now).getSeconds() < 300;
                })
                .collect(Collectors.toList());

        // 古いメモリキャッシュの自動クリーンアップ（1時間以上経過した不要なデータを削除）
        callingStartTimeMap.entrySet().removeIf(entry -> 
            Duration.between(entry.getValue(), now).toHours() >= 1
        );

        response.setCookingOrders(cookingList);
        response.setCallingOrders(callingList);

        return response;
    }

    private boolean isMobileOrder(String orderNumber) {
        return orderNumber != null && orderNumber.toUpperCase().startsWith("M");
    }

    /**
     * 全件のアクティブ注文（受取前）を取得し、get_time（受取日時）の早い順にソートします。
     */
    public List<ActiveOrderResponse> getActiveOrders() {
        List<Map<String, Object>> rows = outerdisplayRepository.getAllActiveOrders();
        Map<Integer, ActiveOrderResponse> map = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            Integer orderId = toInteger(row.get("order_id"));

            ActiveOrderResponse response = map.computeIfAbsent(orderId, id -> {
                ActiveOrderResponse res = new ActiveOrderResponse();
                
                OrderData order = new OrderData();
                order.setOrderId(id);
                order.setOrderNumber((String) row.get("order_number"));
                
                Object getTimeObj = row.get("get_time");
                if (getTimeObj instanceof Timestamp) {
                    order.setGetTime((Timestamp) getTimeObj);
                } else if (getTimeObj != null) {
                    order.setGetTime(Timestamp.valueOf(getTimeObj.toString()));
                }

                order.setMail((String) row.get("mail"));
                
                Integer sumMoney = toInteger(row.get("sum_money"));
                order.setSumMoney(sumMoney != null ? sumMoney : 0);
                order.setMemo((String) row.get("memo"));
                order.setStatus((String) row.get("status"));
                
                res.setOrder(order);
                res.setDetails(new ArrayList<>());
                return res;
            });

            if (row.get("goods_id") != null) {
                ActiveOrderResponse.OrderDetailItem item = new ActiveOrderResponse.OrderDetailItem();
                item.setGoodsId((String) row.get("goods_id"));
                item.setGoodsName((String) row.get("goods_name"));
                item.setCount(toInteger(row.get("count")));
                response.getDetails().add(item);
            }
        }

        for (ActiveOrderResponse res : map.values()) {
            String names = res.getDetails().stream()
                    .map(ActiveOrderResponse.OrderDetailItem::getGoodsName)
                    .collect(Collectors.joining(", "));
            res.setGoodsNames(names);
        }

        // 今日の日付を取得
        LocalDate today = LocalDate.now();

        // 1. 本日日付のデータのみに絞り込み
        // 2. 受取日時（get_time）の早い順に並び替え
        return map.values().stream()
                .filter(res -> {
                    if (res.getOrder() == null || res.getOrder().getGetTime() == null) {
                        return false;
                    }
                    // get_time の日付部分が今日と一致するか判定
                    LocalDate orderDate = res.getOrder().getGetTime().toLocalDateTime().toLocalDate();
                    return today.equals(orderDate);
                })
                .sorted((o1, o2) -> {
                    Timestamp t1 = o1.getOrder().getGetTime();
                    Timestamp t2 = o2.getOrder().getGetTime();
                    return t1.compareTo(t2); // 早い時間順
                })
                .collect(Collectors.toList());
    }
}