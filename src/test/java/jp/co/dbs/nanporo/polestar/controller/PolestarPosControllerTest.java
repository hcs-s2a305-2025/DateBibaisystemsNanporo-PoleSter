package jp.co.dbs.nanporo.polestar.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import jp.co.dbs.nanporo.polestar.request.MobileOrderRequest;
import jp.co.dbs.nanporo.polestar.request.PaymentRequest;
import jp.co.dbs.nanporo.polestar.response.MobileOrderResponse;
import jp.co.dbs.nanporo.polestar.response.PaymentResponse;
import jp.co.dbs.nanporo.polestar.service.PosService;

@ExtendWith(MockitoExtension.class)
class PolestarPosControllerTest {

    @Mock
    private PosService posService;

    @InjectMocks
    private PolestarPosController controller;

    @Test
    @DisplayName("モバイル注文情報の取得結果を200で返す")
    void testGetMobileOrder() {
        MobileOrderRequest request = new MobileOrderRequest();
        MobileOrderResponse response = MobileOrderResponse.builder().success(true).build();
        when(posService.getTodayMobileOrder(request)).thenReturn(response);

        var result = controller.getMobileOrder(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    @DisplayName("nullリクエストを明示的な400レスポンスにする")
    void testGetMobileOrderWithNullRequest() {
        var result = controller.getMobileOrder(null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().isSuccess()).isFalse();
        assertThat(result.getBody().getMessage()).isEqualTo("リクエストデータが空です。");
    }

    @Test
    @DisplayName("モバイル注文取得の例外を400レスポンスに変換する")
    void testGetMobileOrderFailure() {
        MobileOrderRequest request = new MobileOrderRequest();
        when(posService.getTodayMobileOrder(request)).thenThrow(new IllegalStateException("not found"));

        var result = controller.getMobileOrder(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody()).extracting(MobileOrderResponse::isSuccess).isEqualTo(false);
        assertThat(result.getBody().getMessage()).isEqualTo("not found");
    }

    @Test
    @DisplayName("メッセージなしの例外には予約取得の既定メッセージを返す")
    void testGetMobileOrderFailureWithoutMessage() {
        MobileOrderRequest request = new MobileOrderRequest();
        when(posService.getTodayMobileOrder(request)).thenThrow(new IllegalStateException());

        var result = controller.getMobileOrder(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().getMessage()).isEqualTo("予約注文の取得に失敗しました。");
    }

    @Test
    @DisplayName("会計処理結果を200で返す")
    void testProcessPayment() {
        PaymentRequest request = new PaymentRequest();
        PaymentResponse response = new PaymentResponse(true, "ok", 1);
        when(posService.processPayment(request)).thenReturn(response);

        var result = controller.processPayment(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isSameAs(response);
    }

    @Test
    @DisplayName("会計処理の例外を400レスポンスに変換する")
    void testProcessPaymentFailure() {
        PaymentRequest request = new PaymentRequest();
        doThrow(new IllegalArgumentException("invalid payment")).when(posService).processPayment(request);

        var result = controller.processPayment(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody().isSuccess()).isFalse();
        assertThat(result.getBody().getMessage()).isEqualTo("invalid payment");
        assertThat(result.getBody().getTransactionId()).isNull();
        verify(posService).processPayment(request);
    }
}