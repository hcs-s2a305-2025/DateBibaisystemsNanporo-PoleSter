package jp.co.dbs.nanporo.polestar.response;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class OrderDetailGroupResponse {
    private String goodsId;
    private String goodsName;
    private Integer count;
    private Integer plusZangiCount;
    
    // カスタム（ご飯・ソース）の名称リスト
    private List<String> customNames = new ArrayList<>();
}
