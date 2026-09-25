package jp.co.dbs.nanporo.polestar.repository;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import jp.co.dbs.nanporo.polestar.request.GoodsEditRequest;

@Repository 
public class StoreRepository {
    
    @Autowired 
    private NamedParameterJdbcTemplate jdbc;

    private static final String SELECT_ALL_GOODS = 
            "SELECT goods_id, goods_name, price, photo "
            + "FROM goods_m "
            + "ORDER BY goods_id ASC";

    public List<Map<String, Object>> getAllGoods(){
        return jdbc.queryForList(SELECT_ALL_GOODS, Map.of());
    }

    private static final String SELECT_GOODS_BY_ID = 
            "SELECT goods_id, goods_name, price, photo FROM goods_m WHERE goods_id = :goodsId";

    /**
     * 商品IDをキーに商品情報を1件取得する
     */
    public Map<String, Object> getGoodsById(String goodsId) {
        List<Map<String, Object>> list = jdbc.queryForList(SELECT_GOODS_BY_ID, Map.of("goodsId", goodsId));
        return list.isEmpty() ? null : list.get(0);
    }

    // ごはんオプション取得 (custom_id: 10〜40)
    private static final String SELECT_RICE_CUSTOMS = 
            "SELECT custom_id, goods_name, price, calorie, allergy FROM custom_m "
            + "WHERE custom_id BETWEEN 10 AND 40 ORDER BY custom_id ASC";

    public List<Map<String, Object>> getRiceCustoms() {
        return jdbc.queryForList(SELECT_RICE_CUSTOMS, Map.of());
    }

    // ソースオプション取得 (custom_id: 50〜90)
    private static final String SELECT_SAUCE_CUSTOMS = 
            "SELECT custom_id, goods_name, price, calorie, allergy FROM custom_m "
            + "WHERE custom_id BETWEEN 50 AND 90 ORDER BY custom_id ASC";

    public List<Map<String, Object>> getSauceCustoms() {
        return jdbc.queryForList(SELECT_SAUCE_CUSTOMS, Map.of());
    }

    // 商品情報の更新 (goods_m)
    public void updateGoods(GoodsEditRequest req, String photoPath, String allergyCsv) {
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE goods_m SET ");
        sql.append("goods_name = :goodsName, ");
        sql.append("price = :price, ");
        sql.append("watch_rank = :watchRank, ");
        sql.append("allergy = :allergy ");
        if (photoPath != null) {
            sql.append(", photo = :photo ");
        }
        sql.append("WHERE goods_id = :goodsId");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("goodsId", req.getGoodsId())
                .addValue("goodsName", req.getGoodsName())
                .addValue("price", req.getPrice())
                .addValue("watchRank", req.getRank())
                .addValue("allergy", allergyCsv)
                .addValue("photo", photoPath);

        jdbc.update(sql.toString(), params);
    }

}
