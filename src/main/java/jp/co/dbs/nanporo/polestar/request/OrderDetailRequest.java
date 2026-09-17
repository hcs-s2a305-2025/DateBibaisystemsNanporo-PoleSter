package jp.co.dbs.nanporo.polestar.request;

import lombok.Data;

@Data 
public class OrderDetailRequest {

    // 商品ID
    private Integer goodsId;

    // セット商品ID
    private Integer setGoodsId;

    // 数量
    private Integer count;

    // プラスザンギ個数
    private Integer plusZangiCount;

    // カスタムID
    private Integer customId;
}
