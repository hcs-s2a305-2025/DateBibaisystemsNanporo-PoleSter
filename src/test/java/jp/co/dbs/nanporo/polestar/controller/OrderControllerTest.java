package jp.co.dbs.nanporo.polestar.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import jp.co.dbs.nanporo.polestar.data.CartData;
import jp.co.dbs.nanporo.polestar.data.GoodsData;
import jp.co.dbs.nanporo.polestar.request.OrderRegisterRequest;
import jp.co.dbs.nanporo.polestar.service.OrderService;
import jp.co.dbs.nanporo.polestar.service.StoreService;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private StoreService storeService;

    @InjectMocks
    private OrderController controller;

    @Test
    @DisplayName("ログイン時に注文履歴をModelへ登録する")
    void testShowHistoryWithPrincipal() {
        String mail = "customer@example.com";
        Principal principal = () -> mail;
        when(orderService.getOrderHistory(mail)).thenReturn(List.of());
        var model = new ExtendedModelMap();

        assertThat(controller.showHistory(model, principal)).isEqualTo("history");
        assertThat(model.asMap()).containsEntry("historyList", List.of());
    }

    @Test
    @DisplayName("未ログイン時は履歴サービスを呼ばずに画面を表示する")
    void testShowHistoryWithoutPrincipal() {
        var model = new ExtendedModelMap();

        assertThat(controller.showHistory(model, null)).isEqualTo("history");
        assertThat(model.asMap()).isEmpty();
    }

    @Test
    @DisplayName("新規追加画面に商品の初期選択値を設定する")
    void testShowAddPageForNewCartItem() {
        MockHttpSession session = new MockHttpSession();
        GoodsData goods = goods();
        when(storeService.getGoodsDetail("G1")).thenReturn(goods);
        var model = new ExtendedModelMap();

        assertThat(controller.showAddPage("G1", null, session, model)).isEqualTo("menu/add");
        assertThat(model.asMap()).containsEntry("goods", goods)
                .containsEntry("selectedRice", "20").containsEntry("selectedSource", "0");
    }

    @Test
    @DisplayName("カート編集時に以前の選択値を復元する")
    void testShowAddPageRestoresCartOptions() {
        MockHttpSession session = new MockHttpSession();
        CartData cartItem = cartItem("cart-1", "G1", 100, "50");
        cartItem.setRiceCode("30");
        session.setAttribute("cart", List.of(cartItem));
        when(storeService.getGoodsDetail("G1")).thenReturn(goods());
        var model = new ExtendedModelMap();

        controller.showAddPage("G1", "cart-1", session, model);

        assertThat(model.asMap()).containsEntry("selectedRice", "30")
                .containsEntry("selectedSource", "50").containsEntry("editCartItemId", "cart-1");
    }

    @Test
    @DisplayName("一致するカート要素がない場合は選択初期値を維持する")
    void testShowAddPageWithoutMatchingCartItem() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("cart", List.of(cartItem("other", "G1", 100, "50")));
        when(storeService.getGoodsDetail("G1")).thenReturn(goods());
        var model = new ExtendedModelMap();

        controller.showAddPage("G1", "missing", session, model);

        assertThat(model.asMap()).containsEntry("selectedRice", "20").containsEntry("selectedSource", "0");
    }

    @Test
    @DisplayName("空の編集IDでは新規追加の初期値を使う")
    void testShowAddPageWithBlankEditCartItemId() {
        when(storeService.getGoodsDetail("G1")).thenReturn(goods());
        var model = new ExtendedModelMap();

        controller.showAddPage("G1", "  ", new MockHttpSession(), model);

        assertThat(model.asMap()).containsEntry("selectedRice", "20").containsEntry("selectedSource", "0");
    }

    @Test
    @DisplayName("編集IDがあってもセッションカートがない場合は初期値を使う")
    void testShowAddPageWithMissingSessionCart() {
        when(storeService.getGoodsDetail("G1")).thenReturn(goods());
        var model = new ExtendedModelMap();

        controller.showAddPage("G1", "cart-1", new MockHttpSession(), model);

        assertThat(model.asMap()).containsEntry("selectedRice", "20").containsEntry("selectedSource", "0");
    }

    @Test
    @DisplayName("保存値がnullのカートオプションは初期値を維持する")
    void testShowAddPageWithNullSavedOptions() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("cart", List.of(cartItem("cart-1", "G1", 100, null)));
        when(storeService.getGoodsDetail("G1")).thenReturn(goods());
        var model = new ExtendedModelMap();

        controller.showAddPage("G1", "cart-1", session, model);

        assertThat(model.asMap()).containsEntry("selectedRice", "20").containsEntry("selectedSource", "0");
    }

    @Test
    @DisplayName("予約注文ではログイン情報と予約状態を設定する")
    void testPostOrderReservation() {
        var request = new jp.co.dbs.nanporo.polestar.request.OrderRegisterRequest();
        Principal principal = () -> "customer@example.com";

        assertThat(controller.postOrder(request, "reserve", principal)).isEqualTo("redirect:/order/complete");

        assertThat(request.getMail()).isEqualTo("customer@example.com");
        assertThat(request.getOrderType()).isEqualTo("RESERVATION");
        assertThat(request.getStatus()).isEqualTo("受付");
        assertThat(request.getRegisterTime()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        verify(orderService).insertOrder(request);
    }

    @Test
    @DisplayName("店頭注文では入力済み登録日時と店頭状態を維持する")
    void testPostOrderStore() {
        var request = new jp.co.dbs.nanporo.polestar.request.OrderRegisterRequest();
        request.setRegisterTime("2026-10-01 12:00:00");

        controller.postOrder(request, "store", null);

        assertThat(request.getMail()).isNull();
        assertThat(request.getOrderType()).isEqualTo("STORE");
        assertThat(request.getStatus()).isEqualTo("調理中");
        assertThat(request.getRegisterTime()).isEqualTo("2026-10-01 12:00:00");
        verify(orderService).insertOrder(request);
    }

    @Test
    @DisplayName("登録日時が空文字の場合は現在日時を補完する")
    void testPostOrderWithBlankRegisterTime() {
        var request = new jp.co.dbs.nanporo.polestar.request.OrderRegisterRequest();
        request.setRegisterTime("");

        controller.postOrder(request, "store", null);

        assertThat(request.getRegisterTime()).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
        verify(orderService).insertOrder(request);
    }

    @Test
    @DisplayName("空のカート画面を合計0円で表示する")
    void testShowCartWithoutSessionCart() {
        var model = new ExtendedModelMap();

        assertThat(controller.showCart(new MockHttpSession(), model)).isEqualTo("menu/cart");
        assertThat(model.asMap()).containsEntry("reservedList", List.of()).containsEntry("grandTotal", 0);
    }

    @Test
    @DisplayName("カート内商品の合計金額を表示する")
    void testShowCartWithItems() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("cart", List.of(cartItem("1", "G1", 350, "50"), cartItem("2", "G2", 120, "0")));
        var model = new ExtendedModelMap();

        controller.showCart(session, model);

        assertThat(model.get("grandTotal")).isEqualTo(470);
    }

    @Test
    @DisplayName("商品情報がない場合はメニューへ戻る")
    void testAddToCartWhenGoodsNotFound() {
        when(storeService.getGoodsDetail("missing")).thenReturn(null);
        MockHttpSession session = new MockHttpSession();

        assertThat(controller.addToCart("missing", 0, "20", "0", null, session)).isEqualTo("redirect:/menu");
        assertThat(session.getAttribute("cart")).isNull();
    }

    @Test
    @DisplayName("商品を標準オプションでカートに追加する")
    void testAddToCartNewItem() {
        when(storeService.getGoodsDetail("G1")).thenReturn(goods());
        MockHttpSession session = new MockHttpSession();

        assertThat(controller.addToCart("G1", 5, "20", "0", null, session)).isEqualTo("redirect:/cart");

        CartData item = ((List<CartData>) session.getAttribute("cart")).get(0);
        assertThat(item.getGoodsId()).isEqualTo("G1");
        assertThat(item.getZangiPrice()).isZero();
        assertThat(item.getRiceAmount()).isEqualTo("普通 (250g)");
        assertThat(item.getSourceType()).isEqualTo("なし");
        assertThat(item.getTotalPrice()).isEqualTo(100);
    }

    @Test
    @DisplayName("空の編集IDでは新しいカートIDを発行する")
    void testAddToCartWithBlankEditId() {
        when(storeService.getGoodsDetail("G1")).thenReturn(goods());
        MockHttpSession session = new MockHttpSession();

        controller.addToCart("G1", 5, "20", "0", "  ", session);

        CartData item = ((List<CartData>) session.getAttribute("cart")).get(0);
        assertThat(item.getCartItemId()).isNotBlank().isNotEqualTo("  ");
    }

    @ParameterizedTest(name = "米コード {0} の加算料金と表示名")
    @CsvSource({"10, -30, '小盛り (150g)'", "20, 0, '普通 (250g)'", "30, 50, '大盛り (350g)'", "40, 100, '特盛 (450g)'"})
    void testAddToCartRiceOptions(String riceCode, int expectedPrice, String expectedName) {
        when(storeService.getGoodsDetail("G1")).thenReturn(goods());
        MockHttpSession session = new MockHttpSession();

        controller.addToCart("G1", 6, riceCode, "0", null, session);

        CartData item = ((List<CartData>) session.getAttribute("cart")).get(0);
        assertThat(item.getRicePrice()).isEqualTo(expectedPrice);
        assertThat(item.getRiceAmount()).isEqualTo(expectedName);
        assertThat(item.getZangiPrice()).isEqualTo(100);
    }

    @ParameterizedTest(name = "ソースコード {0} の加算料金と表示名")
    @CsvSource({
            "50, 80, 'おろしポン酢ソース'", "51, 120, 'おろしポン酢ソースだく'", "52, 150, 'おろしポン酢ソースだくだく'",
            "60, 80, '自家製タルタルソース'", "61, 120, '自家製タルタルソースだく'", "62, 150, '自家製タルタルソースだくだく'",
            "70, 80, '油淋鶏風ネギダレ'", "71, 120, '油淋鶏風ネギダレだく'", "72, 150, '油淋鶏風ネギダレだくだく'",
            "80, 100, '皆辣麻婆ソース'", "81, 140, '皆辣麻婆ソースだく'", "82, 180, '皆辣麻婆ソースだくだく'",
            "99, 0, 'なし'"})
    void testAddToCartSourceOptions(String sourceCode, int expectedPrice, String expectedName) {
        when(storeService.getGoodsDetail("G1")).thenReturn(goods());
        MockHttpSession session = new MockHttpSession();

        controller.addToCart("G1", 0, "20", sourceCode, null, session);

        CartData item = ((List<CartData>) session.getAttribute("cart")).get(0);
        assertThat(item.getSourcePrice()).isEqualTo(expectedPrice);
        assertThat(item.getSourceType()).isEqualTo(expectedName);
    }

    @Test
    @DisplayName("同じカート要素を編集して既存要素を置き換える")
    void testAddToCartReplacesEditedItem() {
        when(storeService.getGoodsDetail("G1")).thenReturn(goods());
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("cart", new ArrayList<>(List.of(cartItem("edit-1", "OLD", 10, "0"))));

        controller.addToCart("G1", 5, "20", "0", "edit-1", session);

        List<CartData> cart = (List<CartData>) session.getAttribute("cart");
        assertThat(cart).hasSize(1);
        assertThat(cart.get(0).getCartItemId()).isEqualTo("edit-1");
        assertThat(cart.get(0).getGoodsId()).isEqualTo("G1");
    }

    @Test
    @DisplayName("カートがない場合の商品編集はカート画面へ戻る")
    void testEditCartItemWithoutCart() {
        assertThat(controller.editCartItem("missing", new MockHttpSession())).isEqualTo("redirect:/cart");
    }

    @Test
    @DisplayName("カートに対象商品がない場合はカート画面へ戻る")
    void testEditCartItemNotFound() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("cart", List.of(cartItem("other", "G1", 100, "0")));

        assertThat(controller.editCartItem("missing", session)).isEqualTo("redirect:/cart");
    }

    @Test
    @DisplayName("対象商品を編集画面へ渡す")
    void testEditCartItemFound() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("cart", List.of(cartItem("cart-1", "G1", 100, "0")));

        assertThat(controller.editCartItem("cart-1", session))
                .isEqualTo("redirect:/menu/add?goodsId=G1&editCartItemId=cart-1");
    }

    @Test
    @DisplayName("カート内の商品を削除する")
    void testRemoveFromCart() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("cart", new ArrayList<>(List.of(cartItem("remove", "G1", 100, "0"),
                cartItem("keep", "G2", 200, "0"))));

        assertThat(controller.removeFromCart("remove", session)).isEqualTo("redirect:/cart");
        assertThat((List<CartData>) session.getAttribute("cart")).extracting(CartData::getCartItemId)
                .containsExactly("keep");
    }

    @Test
    @DisplayName("カートがない場合も商品削除画面へ戻る")
    void testRemoveFromCartWithoutCart() {
        assertThat(controller.removeFromCart("missing", new MockHttpSession())).isEqualTo("redirect:/cart");
    }

    @Test
    @DisplayName("カート全体を空にする")
    void testClearCart() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("cart", List.of(cartItem("1", "G1", 100, "0")));

        assertThat(controller.clearCart(session)).isEqualTo("redirect:/cart");
        assertThat(session.getAttribute("cart")).isNull();
    }

    @Test
    @DisplayName("カートがない場合は注文確定せずに戻る")
    void testCheckoutWithoutCart() {
        assertThat(controller.checkout("2026-10-02", "14:00", null, new MockHttpSession(), () -> "a@example.com"))
                .isEqualTo("redirect:/cart");
        verify(orderService, never()).insertOrder(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("空のカートは注文確定できない")
    void testCheckoutWithEmptyCart() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("cart", List.of());

        assertThat(controller.checkout("2026-10-02", "14:00", null, session, () -> "a@example.com"))
                .isEqualTo("redirect:/cart");
    }

    @Test
    @DisplayName("受取日時の形式不正はカート画面へ戻る")
    void testCheckoutInvalidDateFormat() {
        assertInvalidPickup("not-a-date", "14:00");
    }

    @Test
    @DisplayName("過去の日付は受取日時として拒否する")
    void testCheckoutPastDate() {
        assertInvalidPickup(LocalDate.now().minusDays(1).toString(), "14:00");
    }

    @Test
    @DisplayName("明後日より先の日付は受取日時として拒否する")
    void testCheckoutDateTooFar() {
        assertInvalidPickup(LocalDate.now().plusDays(3).toString(), "14:00");
    }

    @Test
    @DisplayName("15時を超える受取時刻は拒否する")
    void testCheckoutAfterClosingTime() {
        assertInvalidPickup(LocalDate.now().plusDays(1).toString(), "15:01");
    }

    @Test
    @DisplayName("30分未満の直前予約は拒否する")
    void testCheckoutTooSoon() {
        LocalDateTime soon = LocalDateTime.now().plusMinutes(10);
        assertInvalidPickup(soon.toLocalDate().toString(), soon.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    @Test
    @DisplayName("有効な受取日時で注文を登録しカートを削除する")
    void testCheckoutSuccess() {
        MockHttpSession session = new MockHttpSession();
        CartData item = cartItem("1", "G1", 450, "50");
        item.setZangiCount(2);
        session.setAttribute("cart", List.of(item));
        Principal principal = () -> "customer@example.com";
        String pickupDate = LocalDate.now().plusDays(1).toString();

        assertThat(controller.checkout(pickupDate, "14:00", "少なめ", session, principal)).isEqualTo("redirect:/home");

        ArgumentCaptor<OrderRegisterRequest> requestCaptor = ArgumentCaptor.forClass(OrderRegisterRequest.class);
        verify(orderService).insertOrder(requestCaptor.capture());
        OrderRegisterRequest request = requestCaptor.getValue();
        assertThat(request.getMail()).isEqualTo("customer@example.com");
        assertThat(request.getOrderType()).isEqualTo("RESERVATION");
        assertThat(request.getStatus()).isEqualTo("受付");
        assertThat(request.getGetTime()).isEqualTo(pickupDate + "T14:00:00");
        assertThat(request.getSumMoney()).isEqualTo(450);
        assertThat(request.getMemo()).isEqualTo("少なめ");
        assertThat(request.getOrderDetails()).hasSize(1);
        assertThat(session.getAttribute("cart")).isNull();
    }

    @Test
    @DisplayName("秒を含む受取時刻をそのまま注文に設定する")
    void testCheckoutWithSecondsInPickupTime() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("cart", List.of(cartItem("1", "G1", 100, "50")));
        String pickupDate = LocalDate.now().plusDays(1).toString();

        assertThat(controller.checkout(pickupDate, "14:00:00", null, session, () -> "customer@example.com"))
                .isEqualTo("redirect:/home");

        ArgumentCaptor<OrderRegisterRequest> requestCaptor = ArgumentCaptor.forClass(OrderRegisterRequest.class);
        verify(orderService).insertOrder(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getGetTime()).isEqualTo(pickupDate + "T14:00:00");
    }

    @Test
    @DisplayName("ログイン済みの注文をキャンセルする")
    void testCancelOrderWithPrincipal() {
        assertThat(controller.cancelOrder(10, () -> "customer@example.com")).isEqualTo("redirect:/");
        verify(orderService).cancelOrder(10);
    }

    @Test
    @DisplayName("未ログイン時は注文をキャンセルしない")
    void testCancelOrderWithoutPrincipal() {
        assertThat(controller.cancelOrder(10, null)).isEqualTo("redirect:/");
        verify(orderService, never()).cancelOrder(10);
    }

    @Test
    @DisplayName("ログイン済みの注文を完成状態にする")
    void testCompleteOrderWithPrincipal() {
        assertThat(controller.completeOrder(11, () -> "staff@example.com")).isEqualTo("redirect:/");
        verify(orderService).completeOrder(11);
    }

    @Test
    @DisplayName("未ログイン時は注文を完成状態にしない")
    void testCompleteOrderWithoutPrincipal() {
        assertThat(controller.completeOrder(11, null)).isEqualTo("redirect:/");
        verify(orderService, never()).completeOrder(11);
    }

    @Test
    @DisplayName("注文を復元してカートへ移し元注文をキャンセルする")
    void testEditOrderRestoresCart() {
        List<CartData> restored = List.of(cartItem("1", "G1", 100, "0"));
        when(orderService.restoreCartFromOrder(12)).thenReturn(restored);
        MockHttpSession session = new MockHttpSession();

        assertThat(controller.editOrder(12, session, () -> "customer@example.com")).isEqualTo("redirect:/cart");
        assertThat(session.getAttribute("cart")).isSameAs(restored);
        verify(orderService).cancelOrder(12);
    }

    @Test
    @DisplayName("注文を復元できない場合は既存注文をキャンセルしない")
    void testEditOrderWithoutRestoredCart() {
        when(orderService.restoreCartFromOrder(12)).thenReturn(List.of());
        MockHttpSession session = new MockHttpSession();

        controller.editOrder(12, session, () -> "customer@example.com");

        assertThat(session.getAttribute("cart")).isNull();
        verify(orderService, never()).cancelOrder(12);
    }

    @Test
    @DisplayName("復元結果がnullの場合は元注文をキャンセルしない")
    void testEditOrderWithNullRestoredCart() {
        when(orderService.restoreCartFromOrder(12)).thenReturn(null);
        MockHttpSession session = new MockHttpSession();

        controller.editOrder(12, session, () -> "customer@example.com");

        assertThat(session.getAttribute("cart")).isNull();
        verify(orderService, never()).cancelOrder(12);
    }

    @Test
    @DisplayName("未ログイン時は注文カートを復元しない")
    void testEditOrderWithoutPrincipal() {
        MockHttpSession session = new MockHttpSession();

        assertThat(controller.editOrder(12, session, null)).isEqualTo("redirect:/cart");
        verify(orderService, never()).restoreCartFromOrder(12);
    }

    @Test
    @DisplayName("過去注文を再注文用カートへ復元する")
    void testReorderRestoresCart() {
        List<CartData> restored = List.of(cartItem("1", "G1", 100, "0"));
        when(orderService.restoreCartFromOrder(13)).thenReturn(restored);
        MockHttpSession session = new MockHttpSession();

        assertThat(controller.reorder(13, session)).isEqualTo("redirect:/cart");
        assertThat(session.getAttribute("cart")).isSameAs(restored);
    }

    @Test
    @DisplayName("過去注文を復元できなくてもカート画面へ戻る")
    void testReorderWithoutRestoredCart() {
        when(orderService.restoreCartFromOrder(13)).thenReturn(null);
        MockHttpSession session = new MockHttpSession();

        assertThat(controller.reorder(13, session)).isEqualTo("redirect:/cart");
        assertThat(session.getAttribute("cart")).isNull();
    }

    @Test
    @DisplayName("再注文の復元結果が空の場合はカートを変更しない")
    void testReorderWithEmptyRestoredCart() {
        when(orderService.restoreCartFromOrder(13)).thenReturn(List.of());
        MockHttpSession session = new MockHttpSession();

        assertThat(controller.reorder(13, session)).isEqualTo("redirect:/cart");
        assertThat(session.getAttribute("cart")).isNull();
    }

    private void assertInvalidPickup(String date, String time) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("cart", List.of(cartItem("1", "G1", 100, "50")));

        assertThat(controller.checkout(date, time, null, session, () -> "customer@example.com"))
                .isEqualTo("redirect:/cart");
        verify(orderService, never()).insertOrder(org.mockito.ArgumentMatchers.any());
    }

    private GoodsData goods() {
        GoodsData goods = new GoodsData();
        goods.setGoodsId("G1");
        goods.setGoodsName("ザンギ弁当");
        goods.setPrice(100);
        goods.setPhoto("photo.png");
        return goods;
    }

    private CartData cartItem(String itemId, String goodsId, int totalPrice, String sourceCode) {
        CartData item = new CartData();
        item.setCartItemId(itemId);
        item.setGoodsId(goodsId);
        item.setGoodsName("商品");
        item.setTotalPrice(totalPrice);
        item.setSourceCode(sourceCode);
        item.setZangiCount(0);
        return item;
    }
}