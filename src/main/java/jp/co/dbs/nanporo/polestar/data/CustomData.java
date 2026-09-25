package jp.co.dbs.nanporo.polestar.data;

import lombok.Data;

@Data 
public class CustomData {

    // カスタムID
    private Integer customId;

    // カスタム商品名
    private String goodsName;

    // 値段（加算される）
    private Integer price;

    // カロリー数
    private Integer calorie;

    // アレルギー
    private String allergy;
}
