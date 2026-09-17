package jp.co.dbs.nanporo.polestar.request;


import java.util.List;

import lombok.Data;

@Data 
public class OrderRegisterRequest {
    
    // 受け取り日時
    private String getTime;

    // メールアドレス
    private String mail;

    // 登録日時
    private String registerTime;

    // 合計金額
    private Integer sumMoney;

    // メモ
    private String memo;

    // ステータス
    private String status;

    /** 注文区分 (例: RESERVATION / STORE) */
    private String orderType;

    /** 注文明細リスト */
    private List<OrderDetailRequest> orderDetails;

}
