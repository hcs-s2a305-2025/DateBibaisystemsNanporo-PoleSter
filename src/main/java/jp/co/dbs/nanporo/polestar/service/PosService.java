package jp.co.dbs.nanporo.polestar.service;

import java.util.Map;

import jp.co.dbs.nanporo.polestar.request.MobileOrderRequest;
import jp.co.dbs.nanporo.polestar.request.PaymentRequest;
import jp.co.dbs.nanporo.polestar.response.MobileOrderResponse;
import jp.co.dbs.nanporo.polestar.response.PaymentResponse;

public interface PosService {
    MobileOrderResponse getTodayMobileOrder(MobileOrderRequest request);
    PaymentResponse processPayment(PaymentRequest request);
    void updateGoodsSoldOut(String goodsId, Boolean soldOut);
    Map<String, Object> getCasherHistory(java.time.LocalDate date);
    // 取引の会計金額（合計・預かり・おつり）を更新します
    void updateTransactionMoney(Integer transactionId, Integer sumMoney, Integer receivedMoney, Integer changeMoney);
}
