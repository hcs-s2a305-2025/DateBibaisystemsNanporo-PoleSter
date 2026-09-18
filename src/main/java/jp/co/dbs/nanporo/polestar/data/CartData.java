package jp.co.dbs.nanporo.polestar.data;

import lombok.Data;

@Data 
public class CartData {
    
    private String cartItemId;// カート内での一意な識別子（UUIDなど）
    private String goodsId; // 商品ID
    private String goodsName; // 商品名
    private Integer price; // 基本価格
    private String photo; // 画像パス
    private String orderDate; // 注文日
    // トッピングなどの情報
    private Integer zangiCount; // ザンギの個数
    private Integer zangiPrice; // ザンギの加算料金
    private String riceAmount; // ご飯の量
    private Integer ricePrice; // ご飯の加算料金
    private String sourceType; // ソース名
    private Integer sourcePrice; // ソースの加算料金
    private Integer totalPrice; // 1個当たりの小計（基本価格 + オプション料金）
}
