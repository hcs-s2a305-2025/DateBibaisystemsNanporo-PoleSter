package jp.co.dbs.nanporo.polestar.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CasherHistoryDto {
    private Integer transactionId;  // 取引ID
    private String time;             // 取引時刻 (例: "14:30:15")
    private String goodsName;        // 購入商品名一覧 (例: "ザンギ弁当 ×2, ポテトサラダ")
    private String detail;           // トッピング・カスタム・クーポンなどの詳細
    private Integer sumMoney;        // 合計金額
    private Integer receivedMoney;   // 預かり金額
    private Integer changeMoney;     // おつり
}
