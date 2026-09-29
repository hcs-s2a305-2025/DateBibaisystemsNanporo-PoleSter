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

    /**
     * 注文ごとにグループ化された調理カード情報を取得
     */
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
                item.put("count", row.get("count"));
                item.put("customName", row.get("custom_name"));
                items.add(item);
            }
        }

        return new ArrayList<>(groupedMap.values());
    }

    /**
     * 調理完了処理
     */
    @Transactional
    public void completeCook(Integer orderId) {
        innerdisplayRepository.updateStatusToReady(orderId);
    }
}