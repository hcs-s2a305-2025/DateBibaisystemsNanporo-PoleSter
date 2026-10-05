package jp.co.dbs.nanporo.polestar.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jp.co.dbs.nanporo.polestar.request.MobileOrderRequest;
import jp.co.dbs.nanporo.polestar.request.PaymentRequest;
import jp.co.dbs.nanporo.polestar.response.MobileOrderResponse;
import jp.co.dbs.nanporo.polestar.response.PaymentResponse;
import jp.co.dbs.nanporo.polestar.service.PosService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/w/pos")
@RequiredArgsConstructor
public class PolestarPosController {
    
    private final PosService posService;
    
    // 当日のモバイルオーダー（予約注文番号）取得API
    // POST /w/pos/mobile-order
    @PostMapping("/mobile-order")
    public ResponseEntity<MobileOrderResponse> getMobileOrder(@RequestBody MobileOrderRequest request) {
        try {
            if (request == null) {
                return ResponseEntity.badRequest().body(
                    MobileOrderResponse.builder()
                            .success(false)
                            .message("リクエストデータが空です。")
                            .build()
                );
            }

            MobileOrderResponse response = posService.getTodayMobileOrder(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                MobileOrderResponse.builder()
                        .success(false)
                        .message(e.getMessage() != null ? e.getMessage() : "予約注文の取得に失敗しました。")
                        .build()
            );
        }
    }

    // 会計登録処理API
    // POST /w/pos/payment
    @PostMapping("/payment")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        try {
            PaymentResponse response = posService.processPayment(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new PaymentResponse(false, e.getMessage(), null)
            );
        }
    }

    // 商品の販売状態（販売中/販売停止）を切り替えるAPI
    @PostMapping("/goods/toggle-sold-out")
    public ResponseEntity<Map<String, Object>> toggleGoodsSoldOut(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String goodsId = request.get("goodsId").toString();
            Boolean soldOut = (Boolean) request.get("soldOut");

            // Service層で DB (goods_m など) の sold_out カラムを更新
            posService.updateGoodsSoldOut(goodsId, soldOut);

            response.put("success", true);
            response.put("message", "販売状態を更新しました。");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
