package jp.co.dbs.nanporo.polestar.response;

import lombok.Data;
import java.util.List;
import jp.co.dbs.nanporo.polestar.data.OrderData;

@Data
public class ActiveOrderResponse {

    private OrderData order;
    private List<OrderDetailItem> details;
    private String goodsNames;

    @Data
    public static class OrderDetailItem {
        private String goodsId;
        private String goodsName;
        private Integer count;
        private Integer zangiCount;
        
        // ご飯関係
        private String riceAmount;
        private Integer ricePrice;

        // ソース関係
        private String sourceName;
        private Integer sourcePrice;

        // セット商品関係
        private String setName;
        private Integer setPrice;
    }
}