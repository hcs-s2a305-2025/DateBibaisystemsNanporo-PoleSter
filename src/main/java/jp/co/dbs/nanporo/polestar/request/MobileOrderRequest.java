package jp.co.dbs.nanporo.polestar.request;

import lombok.Data;

@Data 
public class MobileOrderRequest {
    private String orderNo;
    private String mobileOrderNo;

    /**
     * 実際に使用する注文番号を取得（どちらのフィールドで送られてきても対応）
     */
    public String getEffectiveOrderNo() {
        if (orderNo != null && !orderNo.trim().isEmpty()) {
            return orderNo.trim();
        }
        if (mobileOrderNo != null && !mobileOrderNo.trim().isEmpty()) {
            return mobileOrderNo.trim();
        }
        return "";
    }

    /**
     * 頭文字が 'M' で始まっている（予約注文）か判定
     */
    public boolean isMobileOrder() {
        String no = getEffectiveOrderNo();
        return no.startsWith("M") || no.startsWith("m");
    }
}
