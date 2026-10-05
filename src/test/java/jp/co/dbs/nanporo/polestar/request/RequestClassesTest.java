package jp.co.dbs.nanporo.polestar.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RequestClassesTest {

    @Test
    @DisplayName("注文番号を優先し、前後の空白を除去する")
    void testMobileOrderRequestPrefersOrderNo() {
        MobileOrderRequest request = new MobileOrderRequest();
        request.setOrderNo("  M-001  ");
        request.setMobileOrderNo("M-002");

        assertThat(request.getEffectiveOrderNo()).isEqualTo("M-001");
        assertThat(request.isMobileOrder()).isTrue();
    }

    @ParameterizedTest
    @CsvSource(value = {
            "NULL, '  m-002  ', m-002, true",
            "'   ', '  001  ', 001, false",
            "'   ', '   ', '', false",
            "NULL, NULL, '', false",
            "'  123  ', 'M-002', 123, false"
    }, nullValues = "NULL")
    @DisplayName("注文番号がない場合はモバイル番号へフォールバックする")
    void testMobileOrderRequestFallback(String orderNo, String mobileOrderNo,
            String expectedOrderNo, boolean expectedMobileOrder) {
        MobileOrderRequest request = new MobileOrderRequest();
        request.setOrderNo(orderNo);
        request.setMobileOrderNo(mobileOrderNo);

        assertThat(request.getEffectiveOrderNo()).isEqualTo(expectedOrderNo);
        assertThat(request.isMobileOrder()).isEqualTo(expectedMobileOrder);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "NULL, false",
            "M001, true",
            "m001, true",
            "' M001 ', true",
            "001, false",
            "'   ', false"
    }, nullValues = "NULL")
    @DisplayName("支払Requestから予約注文番号を判定する")
    void testPaymentRequestMobileOrderDetection(String mobileOrderNo, boolean expected) {
        PaymentRequest request = new PaymentRequest();
        request.setMobileOrderNo(mobileOrderNo);

        assertThat(request.isMobileOrder()).isEqualTo(expected);
    }

    @Test
    @DisplayName("支払Requestと明細・トッピングの全項目を保持する")
    void testPaymentRequestFields() {
        PaymentRequest.ToppingRequest topping = new PaymentRequest.ToppingRequest();
        topping.setName("大盛り");
        topping.setPrice(100);
        topping.setQuantity(1);

        PaymentRequest.PaymentItemRequest item = new PaymentRequest.PaymentItemRequest();
        item.setProductId("G01");
        item.setName("ザンギ弁当");
        item.setUnitPrice(800);
        item.setQuantity(2);
        item.setTotal(1700);
        item.setToppings(List.of(topping));

        PaymentRequest request = new PaymentRequest();
        request.setTransactionType("SALE");
        request.setQrId("QR01");
        request.setMobileOrderNo("M001");
        request.setSubtotal(1700);
        request.setDiscount(100);
        request.setReturnAmount(0);
        request.setTotal(1600);
        request.setReceived(2000);
        request.setChange(400);
        request.setPaymentMethod("現金");
        request.setItems(List.of(item));
        request.setUseCoupon("学生割引");

        PaymentRequest.ToppingRequest expectedTopping = new PaymentRequest.ToppingRequest();
        expectedTopping.setName("大盛り");
        expectedTopping.setPrice(100);
        expectedTopping.setQuantity(1);
        PaymentRequest.PaymentItemRequest expectedItem = new PaymentRequest.PaymentItemRequest();
        expectedItem.setProductId("G01");
        expectedItem.setName("ザンギ弁当");
        expectedItem.setUnitPrice(800);
        expectedItem.setQuantity(2);
        expectedItem.setTotal(1700);
        expectedItem.setToppings(List.of(expectedTopping));
        PaymentRequest expected = new PaymentRequest();
        expected.setTransactionType("SALE");
        expected.setQrId("QR01");
        expected.setMobileOrderNo("M001");
        expected.setSubtotal(1700);
        expected.setDiscount(100);
        expected.setReturnAmount(0);
        expected.setTotal(1600);
        expected.setReceived(2000);
        expected.setChange(400);
        expected.setPaymentMethod("現金");
        expected.setItems(List.of(expectedItem));
        expected.setUseCoupon("学生割引");

        assertEquivalent(request, expected);
        assertEquivalent(item, expectedItem);
        assertEquivalent(topping, expectedTopping);
        assertThat(request.getItems().get(0).getToppings().get(0).getPrice()).isEqualTo(100);
        assertThat(request.getUseCoupon()).isEqualTo("学生割引");
    }

    @Test
    @DisplayName("注文登録Requestの全項目を保持する")
    void testOrderRegisterRequest() {
        OrderDetailRequest detail = new OrderDetailRequest();
        detail.setGoodsId("G01");
        detail.setSetGoodsId(2);
        detail.setCount(3);
        detail.setPlusZangiCount(1);
        detail.setCustomId(50);

        OrderRegisterRequest request = new OrderRegisterRequest();
        request.setGetTime("2026-10-02T14:00:00");
        request.setMail("user@example.com");
        request.setRegisterTime("2026-10-01T12:00:00");
        request.setSumMoney(2400);
        request.setMemo("備考");
        request.setStatus("受付");
        request.setOrderType("RESERVATION");
        request.setOrderDetails(List.of(detail));

        OrderDetailRequest expectedDetail = new OrderDetailRequest();
        expectedDetail.setGoodsId("G01");
        expectedDetail.setSetGoodsId(2);
        expectedDetail.setCount(3);
        expectedDetail.setPlusZangiCount(1);
        expectedDetail.setCustomId(50);
        OrderRegisterRequest expected = new OrderRegisterRequest();
        expected.setGetTime("2026-10-02T14:00:00");
        expected.setMail("user@example.com");
        expected.setRegisterTime("2026-10-01T12:00:00");
        expected.setSumMoney(2400);
        expected.setMemo("備考");
        expected.setStatus("受付");
        expected.setOrderType("RESERVATION");
        expected.setOrderDetails(List.of(expectedDetail));

        assertEquivalent(request, expected);
        assertEquivalent(detail, expectedDetail);
    }

    @Test
    @DisplayName("商品編集Requestの全項目とカテゴリIDアクセサを保持する")
    void testGoodsEditRequest() {
        GoodsEditRequest request = new GoodsEditRequest();
        request.setGoodsId("G01");
        request.setGoodsName("ザンギ弁当");
        request.setPrice(800);
        request.setPhoto("goods.png");
        request.setCalorie(650);
        request.setAllergy("小麦");
        request.setZangiCount("5");
        request.setSoldOut(false);
        request.setDetail("説明");
        request.setRank("一般");
        request.setCategoryId("B");

        GoodsEditRequest expected = new GoodsEditRequest();
        expected.setGoodsId("G01");
        expected.setGoodsName("ザンギ弁当");
        expected.setPrice(800);
        expected.setPhoto("goods.png");
        expected.setCalorie(650);
        expected.setAllergy("小麦");
        expected.setZangiCount("5");
        expected.setSoldOut(false);
        expected.setDetail("説明");
        expected.setRank("一般");
        expected.setCategoryId("B");

        assertEquivalent(request, expected);
        assertThat(request.getCategoryId()).isEqualTo("B");
    }

    @Test
    @DisplayName("空のユーザー取得・更新Requestを生成できる")
    void testEmptyUserRequests() {
        assertThat(new UserGetRequest()).isNotNull();
        assertThat(new UserPutRequest()).isNotNull();
    }

    private void assertEquivalent(Object actual, Object expected) {
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.hashCode()).isEqualTo(expected.hashCode());
        assertThat(actual.toString()).isNotBlank();
    }
}