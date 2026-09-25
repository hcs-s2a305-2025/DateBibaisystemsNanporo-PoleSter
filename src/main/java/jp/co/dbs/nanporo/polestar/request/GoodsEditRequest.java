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

    // 閲覧可能会員ランク
    private String rank;          // watch_rank に対応
    
    // 商品カテゴリ
    private String categoryPrefix; // B, S, U など
    
    // 商品画像
    private MultipartFile imageFile;
    
    //　アレルギー名リスト
    private List<String> allergenNames; // 選択されたアレルゲン名のリスト
}
