package jp.co.dbs.nanporo.polestar.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDate;

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

        // 2. 画像右側: 「お呼び出し中」のリストを抽出（ステータスが '受取可' または '呼び出し中' の注文）
        List<ActiveOrderResponse> callingList = allActiveOrders.stream()
                .filter(o -> o.getOrder() != null && 
                            ("受取可".equals(o.getOrder().getStatus()) || "呼び出し中".equals(o.getOrder().getStatus())))
                .collect(Collectors.toList());

        response.setCookingOrders(cookingList);
        response.setCallingOrders(callingList);

        return response;
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