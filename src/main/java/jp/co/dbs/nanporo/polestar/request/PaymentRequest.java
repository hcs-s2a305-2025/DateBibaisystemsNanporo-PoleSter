package jp.co.dbs.nanporo.polestar.request;

import java.util.List;

import lombok.Data;

@Data 
public class PaymentRequest {
    private String transactionType; // SALE, RETURN
    private String qrId;
    private String mobileOrderNo;   // 注文番号（Mから始まる場合は予約注文）
    private Integer subtotal;
    private Integer discount;
    private Integer returnAmount;
    private Integer total;
    private Integer received;
    private Integer change;
    private String paymentMethod;
    private List<PaymentItemRequest> items;

    /**
     * 頭文字が 'M' で始まっている（予約注文）か判定
     */
    public boolean isMobileOrder() {
        return mobileOrderNo != null && 
            (mobileOrderNo.trim().startsWith("M") || mobileOrderNo.trim().startsWith("m"));
    }

    @Data
    public static class PaymentItemRequest {
        private String productId;
        private String name;
        private Integer unitPrice;
        private Integer quantity;
        private Integer total;
        private List<ToppingRequest> toppings;
    }

    @Data
    public static class ToppingRequest {
        private Integer customId;
        private Integer setGoodsId;
        private Integer plusZangiCount;
        private String name;
        private Integer price;
        private Integer quantity;
    }

    // 既存のフィールド...
    private String useCoupon; //  ("学生割引", "学生割引,スタンプカード割引" 等)

    public String getUseCoupon() {
        return useCoupon;
    }

    public void setUseCoupon(String useCoupon) {
        this.useCoupon = useCoupon;
    }
}
