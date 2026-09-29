package jp.co.dbs.nanporo.polestar.data;

import lombok.Data;

// 商品情報
@Data 
public class GoodsData {
    
    // 商品ID
    private String goodsId;

    // 商品名
    private String goodsName;

    // 写真
    private String photo;

    // 価格
    private Integer price;

    // カロリー
    private Integer calorie;

    // アレルギー
    private String allergy;

    // ザンギ個数
    private Integer zangiCount;

    // 売り切れ
    private Boolean soldOut;

    // 詳細（商品の詳細情報）
    private String detail;

    // 会員ランク
    private String watchRank;

    private String categoryId; // categoryId フィールドを追加

    // ゲッターとセッターを追加
    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public Boolean getSoldOut() { // または isSoldOut()
        return soldOut;
    }
    public void setSoldOut(Boolean soldOut) {
        this.soldOut = soldOut;
    }
}
