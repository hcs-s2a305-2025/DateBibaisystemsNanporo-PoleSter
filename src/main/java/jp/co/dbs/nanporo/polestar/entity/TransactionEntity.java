package jp.co.dbs.nanporo.polestar.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data 
@Entity 
@Table(name = "transaction_t")
public class TransactionEntity {
    
    // 取引ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Integer transactionId;

    // 注文番号
    @Column(name = "order_id", nullable = false)
    private Integer orderId;

    // メール
    @Column(name = "mail", nullable = false, length = 50)
    private String mail;

    // 取引日
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    // クーポン利用
    @Column(name = "use_coupon", length = 9)
    private String useCoupon;

    // 預り金
    @Column(name = "received_money", nullable = false)
    private Integer receivedMoney;

    // お釣り
    @Column(name = "change_money", nullable = false)
    private Integer changeMoney;

    // 合計金額
    @Column(name = "sum_money", nullable = false)
    private Integer sumMoney;

}
