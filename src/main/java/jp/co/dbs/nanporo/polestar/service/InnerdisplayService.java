package jp.co.dbs.nanporo.polestar.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.co.dbs.nanporo.polestar.repository.InnerdisplayRepository;

@Service
public class InnerdisplayService {

    @Autowired
    private InnerdisplayRepository innerdisplayRepository;

    @Autowired 
    private NotificationService notificationService;

    public List<Map<String, Object>> getKitchenOrdersGrouped() {
        List<Map<String, Object>> rawList = innerdisplayRepository.getKitchenOrders();
        Map<Integer, Map<String, Object>> groupedMap = new LinkedHashMap<>();

        for (Map<String, Object> row : rawList) {
            Integer orderId = ((Number) row.get("order_id")).intValue();

            Map<String, Object> order = groupedMap.computeIfAbsent(orderId, id -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("orderId", row.get("order_id"));
                map.put("orderNumber", row.get("order_number"));
                map.put("getTime", row.get("get_time"));
                map.put("registerTime", row.get("register_time"));
                map.put("memo", row.get("memo"));
                map.put("status", row.get("status"));
                map.put("items", new ArrayList<Map<String, Object>>());
                return map;
            });

            if (row.get("goods_name") != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("goodsName", row.get("goods_name"));
                item.put("setGoodsId", row.get("set_goods_id"));
                item.put("count", row.get("count"));
                item.put("customName", row.get("custom_name"));
                items.add(item);
            }
        }

        return new ArrayList<>(groupedMap.values());
    }

    /**
     * 調理完了処理：ステータス更新 ＋ 通知（notice_t）作成
     */
    @Transactional
    public void completeCook(Integer orderId) {
        // 1. 対象の注文情報（メールアドレス・予約番号）を取得
        Map<String, Object> order = innerdisplayRepository.getOrderById(orderId);
        
        if (order != null) {
            String mail = (String) order.get("mail");
            String orderNumber = (String) order.get("order_number");

            // 2. 注文ステータスを '受取可' に更新
            innerdisplayRepository.updateStatusToReady(orderId);

            // 3. 通知用メッセージを作成して notice_t に登録（メールアドレスが存在する場合）
            if (mail != null && !mail.isEmpty()) {
                String noticeContent = "モバイル予約(" + orderNumber + ")の受取準備が整いました。";
                innerdisplayRepository.insertNotice(mail, noticeContent);
            }
        }
        notificationService.sendOrderCompleteNotification(orderId);
    }
}