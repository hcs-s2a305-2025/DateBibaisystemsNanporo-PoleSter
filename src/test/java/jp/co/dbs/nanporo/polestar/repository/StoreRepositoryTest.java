package jp.co.dbs.nanporo.polestar.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import jp.co.dbs.nanporo.polestar.entity.GoodsEntity;
import jp.co.dbs.nanporo.polestar.request.GoodsEditRequest;

@ExtendWith(MockitoExtension.class)
class StoreRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    @InjectMocks
    private StoreRepository repository;

    @Test
    @DisplayName("全商品の一覧を返す")
    void testGetAllGoods() {
        List<Map<String, Object>> expected = List.of(Map.of("goods_id", "B001"));
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(expected);

        assertThat(repository.getAllGoods()).isSameAs(expected);
    }

    @Test
    @DisplayName("商品IDに一致する商品を返す")
    void testGetGoodsByIdFound() {
        Map<String, Object> expected = Map.of("goods_id", "B001");
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(List.of(expected));

        assertThat(repository.getGoodsById("B001")).isSameAs(expected);
    }

    @Test
    @DisplayName("商品が見つからない場合はnullを返す")
    void testGetGoodsByIdNotFound() {
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(List.of());

        assertThat(repository.getGoodsById("missing")).isNull();
    }

    @Test
    @DisplayName("カテゴリ未指定時の商品IDは弁当カテゴリから採番する")
    void testGenerateGoodsIdDefaultPrefix() {
        when(jdbc.queryForList(anyString(), anyMap(), eq(String.class))).thenReturn(List.of());

        assertThat(repository.generateGoodsId(null)).isEqualTo("B001");
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(jdbc).queryForList(anyString(), params.capture(), eq(String.class));
        assertThat(params.getValue()).containsEntry("prefix", "B%");
    }

    @Test
    @DisplayName("カテゴリ指定時は最大商品IDの次を採番する")
    void testGenerateGoodsIdWithExistingPrefix() {
        when(jdbc.queryForList(anyString(), anyMap(), eq(String.class))).thenReturn(List.of("S009"));

        assertThat(repository.generateGoodsId("S")).isEqualTo("S010");
    }

    @Test
    @DisplayName("空カテゴリは既定の弁当カテゴリを使う")
    void testGenerateGoodsIdWithBlankPrefix() {
        when(jdbc.queryForList(anyString(), anyMap(), eq(String.class))).thenReturn(List.of("B004"));

        assertThat(repository.generateGoodsId("")).isEqualTo("B005");
    }

    @Test
    @DisplayName("最大商品IDがnullの場合は初期番号を採番する")
    void testGenerateGoodsIdWithNullMaximum() {
        when(jdbc.queryForList(anyString(), anyMap(), eq(String.class)))
                .thenReturn(Arrays.asList((String) null));

        assertThat(repository.generateGoodsId("B")).isEqualTo("B001");
    }

    @Test
    @DisplayName("商品新規登録時に型変換と初期値を適用する")
    void testInsertGoodsWithDefaults() {
        GoodsEditRequest request = new GoodsEditRequest();
        request.setGoodsName("弁当");
        request.setPrice(800);
        request.setCalorie(null);
        request.setZangiCount("  ");
        request.setSoldOut(null);
        request.setRank(null);
        when(jdbc.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);

        assertThat(repository.insertGoods("B001", request, "photo.png", "小麦")) .isEqualTo(1);

        MapSqlParameterSource params = captureParameters();
        assertThat(params.getValue("goodsId")).isEqualTo("B001");
        assertThat(params.getValue("price")).isEqualTo(800);
        assertThat(params.getValue("calorie")).isEqualTo(0);
        assertThat(params.getValue("zangiCount")).isEqualTo(0);
        assertThat(params.getValue("soldOut")).isEqualTo(false);
        assertThat(params.getValue("watchRank")).isEqualTo("一般");
    }

    @Test
    @DisplayName("商品登録で明示的な売切状態と会員ランクを使う")
    void testInsertGoodsWithExplicitValues() {
        GoodsEditRequest request = new GoodsEditRequest();
        request.setSoldOut(true);
        request.setRank("ゴールド");
        request.setZangiCount("6");
        when(jdbc.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);

        repository.insertGoods("B002", request, "photo.png", "");

        MapSqlParameterSource params = captureParameters();
        assertThat(params.getValue("soldOut")).isEqualTo(true);
        assertThat(params.getValue("watchRank")).isEqualTo("ゴールド");
        assertThat(params.getValue("zangiCount")).isEqualTo(6);
    }

    @Test
    @DisplayName("商品更新時の不正な加算個数は0へ変換する")
    void testUpdateGoodsWithInvalidZangiCount() {
        GoodsEditRequest request = new GoodsEditRequest();
        request.setGoodsId("B001");
        request.setPrice(900);
        request.setCalorie(500);
        request.setZangiCount("invalid");
        request.setSoldOut(true);
        when(jdbc.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);

        assertThat(repository.updateGoods(request, null, "大豆")).isEqualTo(1);

        MapSqlParameterSource params = captureParameters();
        assertThat(params.getValue("zangiCount")).isEqualTo(0);
        assertThat(params.getValue("soldOut")).isEqualTo(true);
        assertThat(params.getValue("photo")).isNull();
    }

    @Test
    @DisplayName("商品更新時に未指定の売切状態をfalseへ変換する")
    void testUpdateGoodsWithDefaultSoldOut() {
        GoodsEditRequest request = new GoodsEditRequest();
        request.setZangiCount("5");
        request.setSoldOut(null);
        when(jdbc.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);

        repository.updateGoods(request, "photo.png", "");

        MapSqlParameterSource params = captureParameters();
        assertThat(params.getValue("soldOut")).isEqualTo(false);
        assertThat(params.getValue("zangiCount")).isEqualTo(5);
    }

    @Test
    @DisplayName("販売停止状態を更新する")
    void testUpdateSoldOut() {
        when(jdbc.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);

        assertThat(repository.updateSoldOut("B001", true)).isEqualTo(1);

        MapSqlParameterSource params = captureParameters();
        assertThat(params.getValue("goodsId")).isEqualTo("B001");
        assertThat(params.getValue("soldOut")).isEqualTo(true);
    }

    @Test
    @DisplayName("米カスタム一覧を返す")
    void testGetRiceCustoms() {
        List<Map<String, Object>> expected = List.of(Map.of("custom_id", 10));
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(expected);

        assertThat(repository.getRiceCustoms()).isSameAs(expected);
    }

    @Test
    @DisplayName("ソースカスタム一覧を返す")
    void testGetSauceCustoms() {
        List<Map<String, Object>> expected = List.of(Map.of("custom_id", 50));
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(expected);

        assertThat(repository.getSauceCustoms()).isSameAs(expected);
    }

    @Test
    @DisplayName("米カスタムIDを最大値の次から採番する")
    void testGenerateRiceCustomId() {
        when(jdbc.queryForList(anyString(), anyMap(), eq(Integer.class))).thenReturn(List.of(20));

        assertThat(repository.generateRiceCustomId()).isEqualTo(21);
    }

    @Test
    @DisplayName("米カスタムがない場合は初期IDを返す")
    void testGenerateFirstRiceCustomId() {
        when(jdbc.queryForList(anyString(), anyMap(), eq(Integer.class))).thenReturn(List.of());

        assertThat(repository.generateRiceCustomId()).isEqualTo(10);
    }

    @Test
    @DisplayName("最大米カスタムIDがnullなら初期IDを返す")
    void testGenerateRiceCustomIdWithNullMaximum() {
        when(jdbc.queryForList(anyString(), anyMap(), eq(Integer.class)))
                .thenReturn(Arrays.asList((Integer) null));

        assertThat(repository.generateRiceCustomId()).isEqualTo(10);
    }

    @Test
    @DisplayName("ソースカスタムIDを最大値の次から採番する")
    void testGenerateSauceCustomId() {
        when(jdbc.queryForList(anyString(), anyMap(), eq(Integer.class))).thenReturn(List.of(60));

        assertThat(repository.generateSauceCustomId()).isEqualTo(61);
    }

    @Test
    @DisplayName("ソースカスタムがない場合は初期IDを返す")
    void testGenerateFirstSauceCustomId() {
        when(jdbc.queryForList(anyString(), anyMap(), eq(Integer.class))).thenReturn(List.of());

        assertThat(repository.generateSauceCustomId()).isEqualTo(50);
    }

    @Test
    @DisplayName("最大ソースカスタムIDがnullなら初期IDを返す")
    void testGenerateSauceCustomIdWithNullMaximum() {
        when(jdbc.queryForList(anyString(), anyMap(), eq(Integer.class)))
                .thenReturn(Arrays.asList((Integer) null));

        assertThat(repository.generateSauceCustomId()).isEqualTo(50);
    }

    @Test
    @DisplayName("カスタムを新規登録する")
    void testInsertCustom() {
        when(jdbc.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);

        assertThat(repository.insertCustom(50, "ソース", 80, 20, "大豆", false)).isEqualTo(1);
        assertThat(captureParameters().getValue("customId")).isEqualTo(50);
    }

    @Test
    @DisplayName("カスタムを更新する")
    void testUpdateCustom() {
        when(jdbc.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);

        assertThat(repository.updateCustom(50, "ソース", 80, 20, "大豆", true)).isEqualTo(1);
        assertThat(captureParameters().getValue("sold_out")).isEqualTo(true);
    }

    @Test
    @DisplayName("カスタムを削除する")
    void testDeleteCustom() {
        when(jdbc.update(anyString(), anyMap())).thenReturn(1);

        assertThat(repository.deleteCustom(50)).isEqualTo(1);
    }

    @Test
    @DisplayName("全商品の情報を取得する")
    void testGetAll() {
        List<Map<String, Object>> expected = List.of(Map.of("goods_id", "B001"));
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(expected);

        assertThat(repository.getAll()).isSameAs(expected);
    }

    @Test
    @DisplayName("商品Entityがある場合はOptionalに格納する")
    void testGetGoodsEntityByIdFound() {
        GoodsEntity goods = new GoodsEntity();
        goods.setGoodsId("B001");
        doReturn(List.of(goods)).when(jdbc).query(anyString(), anyMap(), any(RowMapper.class));

        assertThat(repository.getGoodsEntityById("B001")).isEqualTo(Optional.of(goods));
    }

    @Test
    @DisplayName("商品Entityがない場合はOptional.emptyを返す")
    void testGetGoodsEntityByIdNotFound() {
        doReturn(List.of()).when(jdbc).query(anyString(), anyMap(), any(RowMapper.class));

        assertThat(repository.getGoodsEntityById("missing")).isEmpty();
    }

    private MapSqlParameterSource captureParameters() {
        ArgumentCaptor<MapSqlParameterSource> params = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbc).update(anyString(), params.capture());
        return params.getValue();
    }
}