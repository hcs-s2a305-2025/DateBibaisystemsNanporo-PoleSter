package jp.co.dbs.nanporo.polestar.request;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data 
public class GoodsEditRequest {
    
    // 商品ID
    private String goodsId;

    // 商品名
    private String goodsName;

    // 値段
    private Integer price;

    // 商品画像
    private String photo;

    // カロリー数
    private Integer calorie;

    // アレルギー名リスト
    private String allergy; // 選択されたアレルゲン名のリスト

    // ザンギ個数
    private String zangiCount;

    // 売り切れフラグ
    private Boolean soldOut;

    // 詳細
    private String detail;

    // 閲覧可能会員ランク
    private String rank;  // watch_rank に対応

    private String categoryId; // ここに "B", "S", "U" などが入ってきます
    
    // ゲッター・セッター
    public String getCategoryId() {
        return categoryId;
    }
    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
}
