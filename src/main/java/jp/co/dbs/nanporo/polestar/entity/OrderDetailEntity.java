package jp.co.dbs.nanporo.polestar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;

@Data 
@Entity 
@Table(name = "order_detail_t")
@IdClass(OrderDetailKey.class)
public class OrderDetailEntity {
    
    // 注文ID
    @Id
    @Column(name = "order_id")
    private Integer orderId;

    // 品目番号
    @Id
    @Column(name = "order_count")
    private Integer orderCount;

    // 商品ID
    @Column(name = "goods_id", nullable = false, length = 10)
    private String goodsId;

    // セット商品ID
    @Column(name = "set_goods_id")
    private Integer setGoodsId;

    // 個数
    @Column(name = "count", nullable = false)
    private Integer count;

    // プラスザンギ個数
    @Column(name = "plus_zangi_count", nullable = false)
    private Integer plusZangiCount;

    // カスタムID
    @Column(name = "custom_id")
    private Integer customId;
    

    public Integer getSetGoodsId() {
        return setGoodsId;
    }
    public void setSetGoodsId(Integer setGoodsId) {
        this.setGoodsId = setGoodsId;
    }
}
