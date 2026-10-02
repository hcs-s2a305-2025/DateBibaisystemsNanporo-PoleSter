package jp.co.dbs.nanporo.polestar.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data 
@Entity 
@Table (name = "custom_m")
public class CustomEntity {
    
    // カスタムID
    @Id 
    private Integer customId;
    // カスタム名
    private String goodsName;
    // カスタムの価格
    private Integer price;
    // カスタムのカロリー
    private Integer calorie;
    // カスタムのアレルギー
    private String allergy;
    // カスタムの売り切れ状態
    private Boolean soldOut;

}
