package jp.co.dbs.nanporo.polestar.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;

import jp.co.dbs.nanporo.polestar.data.OrderData;
import jp.co.dbs.nanporo.polestar.data.OrderDetailData;

@ExtendWith(MockitoExtension.class)
class OrderRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    @InjectMocks
    private OrderRepository repository;

    @Test
    @DisplayName("既存モバイル注文番号を採番して主注文を登録する")
    void testInsertMobileOrder() {
        when(jdbc.queryForList(anyString(), anyMap(), eq(String.class))).thenReturn(List.of("M0012"));
        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(2);
            keyHolder.getKeyList().add(Map.of("order_id", 42));
            return 1;
        }).when(jdbc).update(anyString(), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class));
        OrderData order = orderData("RESERVATION");

        int orderId = repository.insertOrder(order);

        assertThat(orderId).isEqualTo(42);
        assertThat(order.getOrderNumber()).isEqualTo("M0013");
        ArgumentCaptor<SqlParameterSource> params = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).update(anyString(), params.capture(), any(KeyHolder.class), any(String[].class));
        assertThat(params.getValue().getValue("orderNumber")).isEqualTo("M0013");
        assertThat(params.getValue().getValue("mail")).isEqualTo(order.getMail());
    }

    @Test
    @DisplayName("モバイル注文がない場合は先頭番号を採番する")
    void testInsertMobileOrderWithoutExistingNumber() {
        when(jdbc.queryForList(anyString(), anyMap(), eq(String.class))).thenReturn(List.of());
        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(2);
            keyHolder.getKeyList().add(Map.of("order_id", 1));
            return 1;
        }).when(jdbc).update(anyString(), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class));
        OrderData order = orderData("MOBILE");

        repository.insertOrder(order);

        assertThat(order.getOrderNumber()).isEqualTo("M0001");
    }

    @Test
    @DisplayName("店頭注文番号を採番し生成IDがない場合は0を返す")
    void testInsertStoreOrderWithoutGeneratedKey() {
        when(jdbc.queryForList(anyString(), anyMap(), eq(String.class))).thenReturn(List.of("0009"));
        when(jdbc.update(anyString(), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .thenReturn(1);
        OrderData order = orderData("STORE");

        assertThat(repository.insertOrder(order)).isZero();
        assertThat(order.getOrderNumber()).isEqualTo("0010");
    }

    @Test
    @DisplayName("注文がない場合は店頭注文番号0001を採番する")
    void testInsertFirstStoreOrder() {
        when(jdbc.queryForList(anyString(), anyMap(), eq(String.class))).thenReturn(List.of());
        when(jdbc.update(anyString(), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .thenReturn(1);
        OrderData order = orderData("STORE");

        repository.insertOrder(order);

        assertThat(order.getOrderNumber()).isEqualTo("0001");
    }

    @Test
    @DisplayName("最大注文番号がnullの場合は初期番号を採番する")
    void testInsertOrderWithNullMaximum() {
        when(jdbc.queryForList(anyString(), anyMap(), eq(String.class)))
                .thenReturn(Arrays.asList((String) null));
        when(jdbc.update(anyString(), any(SqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                .thenReturn(1);
        OrderData order = orderData("STORE");

        repository.insertOrder(order);

        assertThat(order.getOrderNumber()).isEqualTo("0001");
    }

    @Test
    @DisplayName("注文明細を全項目付きで登録する")
    void testInsertOrderDetail() {
        OrderDetailData detail = new OrderDetailData();
        detail.setOrderId(3);
        detail.setOrderCount(2);
        detail.setGoodsId("G01");
        detail.setSetGoodsId(4);
        detail.setCount(2);
        detail.setPlusZangiCount(1);
        detail.setCustomId(50);
        when(jdbc.update(anyString(), anyMap())).thenReturn(1);

        assertThat(repository.insertOrderDetail(detail)).isEqualTo(1);

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(jdbc).update(anyString(), params.capture());
        assertThat(params.getValue()).containsEntry("orderId", 3).containsEntry("orderCount", 2)
                .containsEntry("goodsId", "G01").containsEntry("setGoodsId", 4)
                .containsEntry("count", 2).containsEntry("plusZangiCount", 1).containsEntry("customId", 50);
    }

    @Test
    @DisplayName("注文詳細を取得する")
    void testGetOrderByIdFound() {
        Map<String, Object> order = Map.of("order_id", 3);
        when(jdbc.queryForMap(anyString(), anyMap())).thenReturn(order);

        assertThat(repository.getOrderById(3)).isSameAs(order);
    }

    @Test
    @DisplayName("注文詳細がない場合はnullを返す")
    void testGetOrderByIdNotFound() {
        when(jdbc.queryForMap(anyString(), anyMap())).thenThrow(new EmptyResultDataAccessException(1));

        assertThat(repository.getOrderById(404)).isNull();
    }

    @Test
    @DisplayName("完了対象注文の状態を更新する")
    void testUpdateStatusToComplete() {
        when(jdbc.update(anyString(), anyMap())).thenReturn(1);

        assertThat(repository.updateStatusToComplete(3)).isEqualTo(1);
    }

    @Test
    @DisplayName("メールアドレスで予約中注文を取得する")
    void testGetActiveOrdersByMail() {
        List<Map<String, Object>> expected = List.of(Map.of("order_id", 3));
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(expected);

        assertThat(repository.getActiveOrdersByMail("user@example.com")).isSameAs(expected);
    }

    @Test
    @DisplayName("メールアドレスで注文履歴を取得する")
    void testGetOrderHistoryByMail() {
        List<Map<String, Object>> expected = List.of(Map.of("order_id", 3));
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(expected);

        assertThat(repository.getOrderHistoryByMail("user@example.com")).isSameAs(expected);
    }

    @Test
    @DisplayName("注文削除は明細を先に削除する")
    void testDeleteOrderDeletesDetailsFirst() {
        repository.deleteOrder(3);

        InOrder order = inOrder(jdbc);
        order.verify(jdbc).update(argThat(sql -> sql.contains("DELETE FROM order_detail_t")), anyMap());
        order.verify(jdbc).update(argThat(sql -> sql.contains("DELETE FROM order_t")), anyMap());
    }

    @Test
    @DisplayName("注文IDから商品情報付き明細を取得する")
    void testGetOrderDetailsByOrderId() {
        List<Map<String, Object>> expected = List.of(Map.of("goods_id", "G01"));
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(expected);

        assertThat(repository.getOrderDetailsByOrderId(3)).isSameAs(expected);
    }

    private OrderData orderData(String orderType) {
        OrderData order = new OrderData();
        order.setOrderType(orderType);
        order.setMail("user@example.com");
        order.setSumMoney(1200);
        return order;
    }
}