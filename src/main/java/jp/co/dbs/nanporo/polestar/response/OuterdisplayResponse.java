package jp.co.dbs.nanporo.polestar.response;

import java.util.List;
import lombok.Data;

@Data
public class OuterdisplayResponse {
    // 画像左側: 「調理中」の注文リスト
    private List<ActiveOrderResponse> cookingOrders;

    // 画像右側: 「お呼び出し中の番号」の注文リスト
    private List<ActiveOrderResponse> callingOrders;
}