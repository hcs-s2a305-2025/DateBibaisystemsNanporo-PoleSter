package jp.co.dbs.nanporo.polestar.response;

import java.util.List;

import jp.co.dbs.nanporo.polestar.data.OrderData;
import lombok.Data;

@Data 
public class ActiveOrderResponse {
    
    // 注文トラン情報（親）
    private OrderData order;

    // 画面表示用に結合した商品名
    private String goodsNames;

    // 注文明細トラン情報リスト（子）
    private List<OrderDetailItem> details;

    @Data
    public static class OrderDetailItem {
        private String goodsId;
        private String goodsName;
        private Integer count;
    }
}
