package jp.co.dbs.nanporo.polestar.response;

import java.util.List;

import lombok.Data;

@Data
public class OrderHistoryResponse {

    /** 注文ID */
    private Integer orderId;

    /** 注文番号 */
    private String orderNumber;

    /** 画面表示用日時 */
    private String formattedDate;

    /** 合計金額 */
    private Integer sumMoney;

    // 注文明細リスト
    private List<OrderDetailItem> items; 

    @Data
    public static class OrderDetailItem {

        // 商品ID
        private String goodsId;

        // 商品名
        private String goodsName;

        // 商品価格
        private Integer goodsPrice;

        // カスタムトッピング名
        private String customName;

        // カスタムトッピング価格
        private Integer customPrice;

        // 個数
        private Integer count;

        // 画像パス
        private String photo;
    }
    
}
