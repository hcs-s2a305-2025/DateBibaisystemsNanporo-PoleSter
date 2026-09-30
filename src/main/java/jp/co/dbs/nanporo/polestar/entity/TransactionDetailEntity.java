package jp.co.dbs.nanporo.polestar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;

@Data 
@Entity 
@Table(name = "transaction_detail_t")
@IdClass(TransactionDetailKey.class)
public class TransactionDetailEntity {
    
    // 取引ID
    @Id
    @Column(name = "transaction_id")
    private Integer transactionId;

    // 品目番号
    @Id
    @Column(name = "reservation_count")
    private Integer reservationCount;

    // 商品名]
    @Column(name = "goods_name", nullable = false, length = 50)
    private String goodsName;

    // セット商品名
    @Column(name = "set_goods_name", length = 50)
    private String setGoodsName;

    // 数量
    @Column(name = "count", nullable = false)
    private Integer count;

    // プラスザンギ個数
    @Column(name = "plus_zangi_count", nullable = false)
    private Integer plusZangiCount;

    // カスタムID
    @Column(name = "custom_id")
    private Integer customId;

    // 小計
    @Column(name = "price", nullable = false)
    private Integer price;
}
