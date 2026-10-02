package jp.co.dbs.nanporo.polestar.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data 
@Entity 
@Table (name = "set_goods_m")
public class SetGoodsEntity {
    
    // セット商品ID
    @Id 
    private Integer setGoodsId;
    // セット商品名
    private String setGoodsName;
    // セット商品価格
    private Integer price;
    // セット商品カロリー
    private Integer calorie;
    // セット商品アレルギー
    private String allergy;
    // セット商品売り切れ状態
    private Boolean soldOut;
}
