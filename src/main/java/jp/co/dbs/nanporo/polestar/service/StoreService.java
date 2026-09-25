package jp.co.dbs.nanporo.polestar.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.co.dbs.nanporo.polestar.data.AllergenData;
import jp.co.dbs.nanporo.polestar.data.CategoryData;
import jp.co.dbs.nanporo.polestar.data.CustomData;
import jp.co.dbs.nanporo.polestar.data.GoodsData;
import jp.co.dbs.nanporo.polestar.repository.StoreRepository;
import jp.co.dbs.nanporo.polestar.request.GoodsEditRequest;

@Service 
public class StoreService{

    @Autowired 
    private StoreRepository storeRepository;

    // 標準アレルゲンリスト定義
    private static final List<String> STANDARD_ALLERGENS = Arrays.asList(
        "小麦", "卵", "乳", "えび", "かに", "そば", "落花生", "大豆", "牛肉", "豚肉", "鶏肉", "ごま"
    );

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

    // ごはんオプション一覧
    public List<CustomData> getRiceOptions() {
        return mapToCustomDataList(storeRepository.getRiceCustoms());
    }

    // ソースオプション一覧
    public List<CustomData> getSauceOptions() {
        return mapToCustomDataList(storeRepository.getSauceCustoms());
    }

    // 固定のカテゴリ（分類）一覧
    public List<CategoryData> getCategoryList() {
        return Arrays.asList(
            new CategoryData("B", "お弁当類"),
            new CategoryData("S", "単品・サイドメニュー"),
            new CategoryData("U", "裏商品")
        );
    }

    // 商品のアレルゲン状態を判定して一覧を生成
    public List<AllergenData> getAllergenList(String goodsId) {
        GoodsData goods = getGoodsDetail(goodsId);
        List<String> activeAllergies = new ArrayList<>();
        if (goods != null && goods.getAllergy() != null && !"なし".equals(goods.getAllergy())) {
            activeAllergies = Arrays.asList(goods.getAllergy().split(","));
        }

        List<AllergenData> list = new ArrayList<>();
        for (String name : STANDARD_ALLERGENS) {
            AllergenData data = new AllergenData();
            data.setName(name);
            data.setChecked(activeAllergies.contains(name));
            list.add(data);
        }
        return list;
    }

    // 商品情報の更新
    @Transactional
    public void updateGoods(GoodsEditRequest request) {
        String photoPath = null;
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            photoPath = "/img/" + request.getImageFile().getOriginalFilename();
        }

        // 送信されたアレルゲンリストをカンマ区切り文字列に変換
        String allergyCsv = "なし";
        if (request.getAllergenNames() != null && !request.getAllergenNames().isEmpty()) {
            allergyCsv = request.getAllergenNames().stream().collect(Collectors.joining(","));
        }

        storeRepository.updateGoods(request, photoPath, allergyCsv);
    }

    private GoodsData mapRowToGoodsData(Map<String, Object> row) {
        GoodsData goods = new GoodsData();
        goods.setGoodsId((String) row.get("goods_id"));
        goods.setGoodsName((String) row.get("goods_name"));
        goods.setPhoto((String) row.get("photo"));
        
        Object priceObj = row.get("price");
        goods.setPrice(priceObj != null ? ((Number) priceObj).intValue() : 0);
        
        Object calObj = row.get("calorie");
        goods.setCalorie(calObj != null ? ((Number) calObj).intValue() : 0);
        
        goods.setAllergy((String) row.get("allergy"));
        
        Object zangiObj = row.get("zangi_count");
        goods.setZangiCount(zangiObj != null ? ((Number) zangiObj).intValue() : 0);
        
        goods.setSoldOut((Boolean) row.get("sold_out"));
        goods.setDetail((String) row.get("detail"));
        goods.setWatchRank((String) row.get("watch_rank"));
        return goods;
    }

    private List<CustomData> mapToCustomDataList(List<Map<String, Object>> rows) {
        List<CustomData> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            CustomData custom = new CustomData();
            custom.setCustomId(((Number) row.get("custom_id")).intValue());
            custom.setGoodsName((String) row.get("goods_name"));
            
            Object priceObj = row.get("price");
            custom.setPrice(priceObj != null ? ((Number) priceObj).intValue() : 0);
            
            Object calObj = row.get("calorie");
            custom.setCalorie(calObj != null ? ((Number) calObj).intValue() : 0);
            
            custom.setAllergy((String) row.get("allergy"));
            list.add(custom);
        }
        return list;
    }
    
}
