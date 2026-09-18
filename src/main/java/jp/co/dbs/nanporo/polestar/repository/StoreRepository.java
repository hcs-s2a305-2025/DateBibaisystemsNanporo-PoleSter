package jp.co.dbs.nanporo.polestar.repository;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

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

}
