package jp.co.dbs.nanporo.polestar.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class InnerdisplayRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    @InjectMocks
    private InnerdisplayRepository repository;

    @Test
    @DisplayName("厨房向け注文一覧を取得する")
    void testGetKitchenOrders() {
        List<Map<String, Object>> expected = List.of(Map.of("order_id", 1));
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(expected);

        assertThat(repository.getKitchenOrders()).isSameAs(expected);
    }

    @Test
    @DisplayName("注文IDに一致する注文情報を取得する")
    void testGetOrderByIdFound() {
        Map<String, Object> expected = Map.of("mail", "user@example.com", "order_number", "M0001");
        when(jdbc.queryForList(anyString(), any(MapSqlParameterSource.class))).thenReturn(List.of(expected));

        assertThat(repository.getOrderById(15)).isSameAs(expected);
        ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).queryForList(anyString(), params.capture());
        assertThat(params.getValue().getValue("orderId")).isEqualTo(15);
    }

    @Test
    @DisplayName("注文IDが見つからない場合はnullを返す")
    void testGetOrderByIdNotFound() {
        when(jdbc.queryForList(anyString(), any(MapSqlParameterSource.class))).thenReturn(List.of());

        assertThat(repository.getOrderById(404)).isNull();
    }

    @Test
    @DisplayName("注文ステータスを受取可能に更新する")
    void testUpdateStatusToReady() {
        repository.updateStatusToReady(15);

        ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(anyString(), params.capture());
        assertThat(params.getValue().getValue("orderId")).isEqualTo(15);
    }

    @Test
    @DisplayName("通知内容を注文者の通知として登録する")
    void testInsertNotice() {
        repository.insertNotice("user@example.com", "準備完了");

        ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(anyString(), params.capture());
        assertThat(params.getValue().getValue("mail")).isEqualTo("user@example.com");
        assertThat(params.getValue().getValue("content")).isEqualTo("準備完了");
    }
}