package jp.co.dbs.nanporo.polestar.entity;

import lombok.Data;

@Data 
public class OrderDetailEntity {
    
    // 注文ID
    private Integer orderId;

    // 品目番号
    private String orderCount;

    // 商品ID
    private Integer goodsId;

    // セット商品ID
    private Integer setGoodsId;

    // 個数
    private Integer count;

    // プラスザンギ個数
    private Integer plusZangiCount;

    // カスタムID
    private Integer customId;
    
}
