package jp.co.dbs.nanporo.polestar.data;

import lombok.Data;

@Data 
public class OrderDetailData {

    // 注文ID
    private Integer orderId;

    // 品目番号
    private Integer orderCount;

    // 商品ID
    private String goodsId;

    // セット商品ID
    private Integer setGoodsId;

    // 個数
    private Integer count;

    // プラスザンギ個数
    private Integer plusZangiCount;

    // カスタムID
    private Integer customId;
}
