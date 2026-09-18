package jp.co.dbs.nanporo.polestar.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jp.co.dbs.nanporo.polestar.data.GoodsData;
import jp.co.dbs.nanporo.polestar.repository.StoreRepository;

@Service 
public class StoreService{

    @Autowired 
    private StoreRepository storeRepository;

    public List<GoodsData> getMenuList(){
        List<Map<String, Object>> rows = storeRepository.getAllGoods();
        List<GoodsData> goodsList = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            GoodsData goods = new GoodsData();
            goods.setGoodsId((String) row.get("goods_id")); // または Integer
            goods.setGoodsName((String) row.get("goods_name"));
            
            Object priceObj = row.get("price");
            goods.setPrice(priceObj != null ? ((Number) priceObj).intValue() : 0);
            
            goods.setPhoto((String) row.get("photo"));
            
            goodsList.add(goods);
        }

        return goodsList;
    }

    /**
     * 商品詳細画面表示用の単一商品データを取得する
     */
    public GoodsData getGoodsDetail(String goodsId) {
        Map<String, Object> row = storeRepository.getGoodsById(goodsId);
        if (row == null) {
            return null;
        }

        GoodsData goods = new GoodsData();
        goods.setGoodsId((String) row.get("goods_id"));
        goods.setGoodsName((String) row.get("goods_name"));
        
        Object priceObj = row.get("price");
        goods.setPrice(priceObj != null ? ((Number) priceObj).intValue() : 0);
        
        goods.setPhoto((String) row.get("photo"));

        return goods;
    }
}
