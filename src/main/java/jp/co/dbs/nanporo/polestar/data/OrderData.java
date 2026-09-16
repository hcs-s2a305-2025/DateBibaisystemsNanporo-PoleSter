package jp.co.dbs.nanporo.polestar.data;


import java.sql.Timestamp;

import lombok.Data;

// 注文トラン情報
@Data
public class OrderData {
    
    // 注文ID
    private int orderId;

    // 注文番号
    private String orderNumber;

    // 受け取り日時
    private Timestamp getTime;

    // メールアドレス
    private String mail;

    // 登録日時
    private Timestamp registerTime;

    // 合計金額
    private int sumMoney;

    // メモ
    private String memo;

    // ステータス
    private String status;
}
