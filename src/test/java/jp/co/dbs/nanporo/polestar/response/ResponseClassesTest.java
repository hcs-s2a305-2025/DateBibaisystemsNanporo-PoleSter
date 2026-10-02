package jp.co.dbs.nanporo.polestar.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jp.co.dbs.nanporo.polestar.data.OrderData;
import jp.co.dbs.nanporo.polestar.entity.UserEntity;

class ResponseClassesTest {

    @Test
    @DisplayName("アクティブ注文レスポンスの親子情報を保持する")
    void testActiveOrderResponse() {
        OrderData order = new OrderData();
        order.setOrderId(101);
        order.setOrderNumber("A-101");
        order.setMail("user@example.com");
        order.setSumMoney(1800);
        order.setStatus("調理中");

        ActiveOrderResponse.OrderDetailItem detail = new ActiveOrderResponse.OrderDetailItem();
        detail.setGoodsId("G01");
        detail.setGoodsName("ザンギ弁当");
        detail.setCount(2);

        ActiveOrderResponse response = new ActiveOrderResponse();
        response.setOrder(order);
        response.setGoodsNames("ザンギ弁当, かつ丼");
        response.setDetails(List.of(detail));

        ActiveOrderResponse expected = new ActiveOrderResponse();
        expected.setOrder(order);
        expected.setGoodsNames("ザンギ弁当, かつ丼");
        expected.setDetails(List.of(detail));

        assertEquivalent(response, expected);
        assertThat(response.getOrder().getOrderNumber()).isEqualTo("A-101");
        assertThat(response.getDetails().get(0).getGoodsName()).isEqualTo("ザンギ弁当");
    }

    @Test
    @DisplayName("モバイル注文レスポンスのビルダーと明細を保持する")
    void testMobileOrderResponse() {
        MobileOrderResponse.MobileOrderItemDto item = MobileOrderResponse.MobileOrderItemDto.builder()
                .productId("G02")
                .name("牛丼")
                .unitPrice(600)
                .quantity(2)
                .unitTotal(1200)
                .build();

        MobileOrderResponse response = MobileOrderResponse.builder()
                .success(true)
                .message("注文を受け付けました")
                .orderId(42)
                .orderNo("M-042")
                .mail("guest@example.com")
                .items(List.of(item))
                .build();

        MobileOrderResponse expected = MobileOrderResponse.builder()
                .success(true)
                .message("注文を受け付けました")
                .orderId(42)
                .orderNo("M-042")
                .mail("guest@example.com")
                .items(List.of(item))
                .build();

        assertEquivalent(response, expected);
        assertThat(response.getItems().get(0).getUnitTotal()).isEqualTo(1200);
    }

    @Test
    @DisplayName("注文履歴レスポンスの各項目を保持する")
    void testOrderHistoryResponse() {
        OrderHistoryResponse.OrderDetailItem item = new OrderHistoryResponse.OrderDetailItem();
        item.setGoodsId("G03");
        item.setGoodsName("カレー");
        item.setGoodsPrice(700);
        item.setCustomName("大盛り");
        item.setCustomPrice(100);
        item.setCount(2);
        item.setPhoto("/img/curry.png");

        OrderHistoryResponse response = new OrderHistoryResponse();
        response.setOrderId(7);
        response.setOrderNumber("H-007");
        response.setFormattedDate("2026-10-02 12:34");
        response.setSumMoney(1600);
        response.setItems(List.of(item));

        OrderHistoryResponse expected = new OrderHistoryResponse();
        expected.setOrderId(7);
        expected.setOrderNumber("H-007");
        expected.setFormattedDate("2026-10-02 12:34");
        expected.setSumMoney(1600);
        expected.setItems(List.of(item));

        assertEquivalent(response, expected);
        assertThat(response.getItems().get(0).getCustomPrice()).isEqualTo(100);
    }

    @Test
    @DisplayName("注文登録レスポンスの採番結果を保持する")
    void testOrderRegisterResponse() {
        OrderRegisterResponse response = new OrderRegisterResponse();
        response.setOrderId(55);
        response.setOrderNumber("20261002-055");

        OrderRegisterResponse expected = new OrderRegisterResponse();
        expected.setOrderId(55);
        expected.setOrderNumber("20261002-055");

        assertEquivalent(response, expected);
        assertThat(response.getOrderNumber()).isEqualTo("20261002-055");
    }

    @Test
    @DisplayName("外部ディスプレイレスポンスの料理中と呼び出し中リストを保持する")
    void testOuterdisplayResponse() {
        OrderData order = new OrderData();
        order.setOrderId(11);
        order.setOrderNumber("O-011");
        order.setStatus("待ち");

        ActiveOrderResponse cooking = new ActiveOrderResponse();
        cooking.setOrder(order);
        cooking.setGoodsNames("オムライス");

        ActiveOrderResponse calling = new ActiveOrderResponse();
        calling.setOrder(order);
        calling.setGoodsNames("カレー");

        OuterdisplayResponse response = new OuterdisplayResponse();
        response.setCookingOrders(List.of(cooking));
        response.setCallingOrders(List.of(calling));

        OuterdisplayResponse expected = new OuterdisplayResponse();
        expected.setCookingOrders(List.of(cooking));
        expected.setCallingOrders(List.of(calling));

        assertEquivalent(response, expected);
        assertThat(response.getCookingOrders()).hasSize(1);
        assertThat(response.getCallingOrders()).hasSize(1);
    }

    @Test
    @DisplayName("支払レスポンスの結果データを保持する")
    void testPaymentResponse() {
        PaymentResponse response = new PaymentResponse(true, "支払い完了", 99);
        PaymentResponse expected = new PaymentResponse(true, "支払い完了", 99);

        assertEquivalent(response, expected);
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getTransactionId()).isEqualTo(99);
    }

    @Test
    @DisplayName("ユーザー一覧レスポンスと更新レスポンスを保持する")
    void testUserResponses() {
        UserEntity user = new UserEntity();
        user.setMail("member@example.com");
        user.setName("山田太郎");
        user.setRole("USER");
        user.setAlive(true);

        UserGetResponse userGetResponse = new UserGetResponse();
        userGetResponse.setUsers(List.of(user));
        userGetResponse.setTotalPages(2);

        UserGetResponse expectedUserGetResponse = new UserGetResponse();
        expectedUserGetResponse.setUsers(List.of(user));
        expectedUserGetResponse.setTotalPages(2);

        assertEquivalent(userGetResponse, expectedUserGetResponse);
        assertThat(userGetResponse.getUsers()).hasSize(1);
        assertThat(userGetResponse.getTotalPages()).isEqualTo(2);

        assertThat(new UserPutResponse()).isNotNull();
    }

    private void assertEquivalent(Object actual, Object expected) {
        assertThat(actual).isEqualTo(expected);
        assertThat(actual.hashCode()).isEqualTo(expected.hashCode());
        assertThat(actual.toString()).isNotBlank();
    }
}
