package jp.co.dbs.nanporo.polestar.service;

import jp.co.dbs.nanporo.polestar.request.MobileOrderRequest;
import jp.co.dbs.nanporo.polestar.request.PaymentRequest;
import jp.co.dbs.nanporo.polestar.response.MobileOrderResponse;
import jp.co.dbs.nanporo.polestar.response.PaymentResponse;

public interface PosService {
    MobileOrderResponse getTodayMobileOrder(MobileOrderRequest request);
    PaymentResponse processPayment(PaymentRequest request);
    void updateGoodsSoldOut(String goodsId, Boolean soldOut);
}
