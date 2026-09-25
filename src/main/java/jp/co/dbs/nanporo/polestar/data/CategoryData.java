package jp.co.dbs.nanporo.polestar.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
public class CategoryData {

    // カテゴリID
    private String id;

    // カテゴリ名
    private String name;
    
}
