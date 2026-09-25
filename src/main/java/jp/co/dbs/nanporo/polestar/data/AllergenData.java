package jp.co.dbs.nanporo.polestar.data;

import lombok.Data;

@Data 
public class AllergenData {
    
    // アレルギーID
    private String id;

    // アレルギー名
    private  String name;   // 例："小麦"

    // 商品に含まれているか
    private Boolean checked;
}
