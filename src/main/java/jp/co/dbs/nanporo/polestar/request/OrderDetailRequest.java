package jp.co.dbs.nanporo.polestar.request;

import lombok.Data;

@Data 
public class OrderDetailRequest {

    // 注文明細ID
    private Integer orderCount;

    // 商品ID
    private String goodsId;

    // セット商品ID
    private Integer setGoodsId;

    // 数量
    private Integer count;

    // プラスザンギ個数
    private Integer plusZangiCount;

    // カスタムID
    private Integer customId;


    public Integer getSetGoodsId() {
        return setGoodsId;
    }
    public void setSetGoodsId(Integer setGoodsId) {
        this.setGoodsId = setGoodsId;
    }
}
