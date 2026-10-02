package jp.co.dbs.nanporo.polestar.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
class OuterdisplayRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    @InjectMocks
    private OuterdisplayRepository repository;

    @Test
    @DisplayName("外部表示用の注文一覧を返す")
    void testGetAllActiveOrders() {
        List<Map<String, Object>> expected = List.of(Map.of("order_id", 1));
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(expected);

        assertThat(repository.getAllActiveOrders()).isSameAs(expected);
    }
}