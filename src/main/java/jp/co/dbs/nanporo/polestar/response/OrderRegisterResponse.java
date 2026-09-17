package jp.co.dbs.nanporo.polestar.response;

import lombok.Data;

@Data 
public class OrderRegisterResponse {
    
    /** 登録完了を通知するための注文ID */
    private Integer orderId;

    /** 採番された当日の注文番号 */
    private String orderNumber;
    
}
