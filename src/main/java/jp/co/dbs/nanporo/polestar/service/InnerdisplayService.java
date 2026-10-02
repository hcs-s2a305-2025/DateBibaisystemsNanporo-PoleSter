package jp.co.dbs.nanporo.polestar.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.co.dbs.nanporo.polestar.data.OrderData;
import jp.co.dbs.nanporo.polestar.repository.InnerdisplayRepository;
import jp.co.dbs.nanporo.polestar.response.ActiveOrderResponse;

@Service
public class InnerdisplayService {

    @Autowired
    private InnerdisplayRepository innerdisplayRepository;

    @Autowired 
    private NotificationService notificationService;

    // 数値変換ヘルパー
    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    /**
     * 厨房表示用の注文（受付・調理中）を取得します。
     */
    public List<ActiveOrderResponse> getActiveOrders(String mail) {
        // InnerdisplayRepository の getKitchenOrders() を呼び出し
        List<Map<String, Object>> rows = innerdisplayRepository.getKitchenOrders();
        Map<Integer, ActiveOrderResponse> map = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            Integer orderId = toInteger(row.get("order_id"));
            if (orderId == null) continue;

            // 親データの生成（初回のみ）
            ActiveOrderResponse response = map.computeIfAbsent(orderId, id -> {
                ActiveOrderResponse res = new ActiveOrderResponse();
                OrderData order = new OrderData();
                order.setOrderId(id);
                order.setOrderNumber((String) row.get("order_number"));
                order.setGetTime((java.sql.Timestamp) row.get("get_time"));
                order.setRegisterTime((java.sql.Timestamp) row.get("register_time")); // 画面用時間のセット
                order.setMemo((String) row.get("memo"));
                order.setStatus((String) row.get("status"));
                
                res.setOrder(order);
                res.setDetails(new ArrayList<>());
                return res;
            });

            // 明細データの追加
            String goodsId = (String) row.get("goods_id");
            if (goodsId != null) {
                Integer customId = toInteger(row.get("custom_id"));

                List<ActiveOrderResponse.OrderDetailItem> details = response.getDetails();
                ActiveOrderResponse.OrderDetailItem lastItem = details.isEmpty() ? null : details.get(details.size() - 1);

                // 直前の要素と同じ goodsId かつ customId がソースコード(50以上)なら既存の要素へまとめる
                if (lastItem != null && goodsId.equals(lastItem.getGoodsId()) && customId != null && customId >= 50) {
                    lastItem.setSourceName(getSourceName(String.valueOf(customId)));
                } else {
                    // 新しい商品として追加
                    ActiveOrderResponse.OrderDetailItem item = new ActiveOrderResponse.OrderDetailItem();
                    item.setGoodsId(goodsId);
                    item.setGoodsName((String) row.get("goods_name"));
                    item.setCount(toInteger(row.get("count")));

                    // ご飯の量判定
                    boolean isSideMenu = goodsId.toUpperCase().startsWith("S");
                    String riceCode = "20";
                    if (isSideMenu) {
                        riceCode = "0";
                    } else if (customId != null && customId < 50) {
                        riceCode = String.valueOf(customId);
                    }

                    item.setRiceAmount(getRiceName(riceCode));

                    // 1行目にソースコードが入っている場合
                    if (customId != null && customId >= 50) {
                        item.setSourceName(getSourceName(String.valueOf(customId)));
                    }

                    details.add(item);
                }
            }
        }

        // 商品名文字列の集計
        for (ActiveOrderResponse res : map.values()) {
            String names = res.getDetails().stream()
                    .map(ActiveOrderResponse.OrderDetailItem::getGoodsName)
                    .filter(name -> name != null)
                    .distinct()
                    .collect(Collectors.joining(", "));
            res.setGoodsNames(names);
        }

        return new ArrayList<>(map.values());
    }

    /**
     * ご飯コードから名称を取得するヘルパーメソッド
     */
    private String getRiceName(String code) {
        return switch (code) {
            case "0" -> "なし";
            case "10" -> "小盛り (150g)";
            case "20" -> "普通 (250g)";
            case "30" -> "大盛り (350g)";
            case "40" -> "特盛り (450g)";
            default -> "普通 (250g)";
        };
    }

    /**
     * ソースコードから名称を取得するヘルパーメソッド
     */
    private String getSourceName(String key) {
        return switch (key) {
            case "50" -> "おろしポン酢ソース";
            case "51" -> "おろしポン酢ソースだく";
            case "52" -> "おろしポン酢ソースだくだく";
            case "60" -> "自家製タルタルソース";
            case "61" -> "自家製タルタルソースだく";
            case "62" -> "自家製タルタルソースだくだく";
            case "70" -> "油淋鶏風ネギダレ";
            case "71" -> "油淋鶏風ネギダレだく";
            case "72" -> "油淋鶏風ネギダレだくだく";
            case "80" -> "皆辣麻婆ソース";
            case "81" -> "皆辣麻婆ソースだく";
            case "82" -> "皆辣麻婆ソースだくだく";
            default -> "なし";
        };
    }

    /**
     * 調理完了処理
     */
    @Transactional
    public void completeCook(Integer orderId) {
        Map<String, Object> order = innerdisplayRepository.getOrderById(orderId);
        
        if (order != null) {
            String mail = (String) order.get("mail");
            String orderNumber = (String) order.get("order_number");

            innerdisplayRepository.updateStatusToReady(orderId);

            if (mail != null && !mail.isEmpty()) {
                String noticeContent = "モバイル予約(" + orderNumber + ")の受取準備が整いました。";
                innerdisplayRepository.insertNotice(mail, noticeContent);
            }
        }
        notificationService.sendOrderCompleteNotification(orderId);
    }

    public @Nullable Object getKitchenOrdersGrouped() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getKitchenOrdersGrouped'");
    }
}