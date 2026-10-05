package jp.co.dbs.nanporo.polestar.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    public List<GoodsData> getMenuList(String prefix, String userMemberRank){
        List<Map<String, Object>> rows;
        // prefix が指定されている場合は該当カテゴリを検索、未指定時は全件検索
        if (prefix != null && !prefix.trim().isEmpty()) {
            rows = storeRepository.getGoodsByPrefix(prefix.trim());
        } else {
            rows = storeRepository.getAllGoods();
        }
        int userRankLevel = getRankLevel(userMemberRank);
        List<GoodsData> goodsList = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            GoodsData goods = mapRowToGoodsData(row);
            // ユーザーのランクレベル >= 商品の閲覧ランクレベル の場合のみ表示
            if (userRankLevel >= getRankLevel(goods.getWatchRank())) {
                goodsList.add(goods);
            }
        }

        return goodsList;
    }
    /**
     * 管理画面・POS用（全ランクの商品を取得する）
     */
    public List<GoodsData> getMenuList(String prefix) {
        // ゴールド（最高ランク）を指定することで全商品を取得
        return getMenuList(prefix, "ゴールド");
    }
    /**
     * ランク名を比較用の数値レベルに変換するヘルパーメソッド
     */
    private int getRankLevel(String rank) {
        if (rank == null) {
            return 1; // 未指定時は一般
        }
        switch (rank) {
            case "ブロンズ":
                return 2;
            case "シルバー":
                return 3;
            case "ゴールド":
                return 4;
            case "一般":
            default:
                return 1;
        }
    }


    /**
     * 商品詳細画面表示用の単一商品データを取得する
     */
    public GoodsData getGoodsDetail(String goodsId) {
        if (goodsId == null || goodsId.trim().isEmpty()){
            return null;
        }
        Map<String, Object> row = storeRepository.getGoodsById(goodsId);
        if (row == null) {
            return null;
        }
        return mapRowToGoodsData(row);
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

    // 商品情報の保存（新規登録および更新）
    @Transactional
    public void saveGoods(GoodsEditRequest request) {
        String photoPath = null;
        if (request.getPhoto() != null && !request.getPhoto().isEmpty()) {
            photoPath = "/img/" + request.getPhoto();
        }

        // request.getAllergy() は String型として受け取りそのまま設定
        String allergyCsv = request.getAllergy();
        if (allergyCsv == null || "なし".equals(allergyCsv)) {
            allergyCsv = "";
        }

        // goodsId が空の場合は「新規登録」、存在する場合は「更新」
        if (request.getGoodsId() == null || request.getGoodsId().trim().isEmpty()) {
            // カテゴリプレフィックス（例: "B", "S", "U"）を取得（未指定時は"B"）
            String prefix = request.getCategoryId();
            if (prefix == null || prefix.isEmpty()) {
                prefix = "B";
            }
            // 次のID（例: "B007"）を自動採番
            String newGoodsId = storeRepository.generateGoodsId(prefix);
            storeRepository.insertGoods(newGoodsId, request, photoPath, allergyCsv);
        } else {
            storeRepository.updateGoods(request, photoPath, allergyCsv);
        }
    }

    /**
     * 商品の販売ステータス（sold_out）を変更します。
     */
    @Transactional
    public void updateSoldOut(String goodsId, boolean soldOut) {
        storeRepository.updateSoldOut(goodsId, soldOut);
    }

    /**
     * DBの取得結果マップを GoodsData に変換します。
     */
    private GoodsData mapRowToGoodsData(Map<String, Object> row) {
        GoodsData goods = new GoodsData();
        goods.setGoodsId(getString(row, "goods_id"));
        goods.setGoodsName(getString(row, "goods_name"));
        goods.setPhoto(getString(row, "photo"));
        goods.setPrice(getInt(row, "price"));
        goods.setCalorie(getInt(row, "calorie"));
        goods.setAllergy(getString(row, "allergy"));
        goods.setZangiCount(getInt(row, "zangi_count"));
        
        // sold_out の Boolean 判定
        Object soldObj = getValue(row, "sold_out");
        if (soldObj instanceof Boolean) {
            goods.setSoldOut((Boolean) soldObj);
        } else if (soldObj != null) {
            goods.setSoldOut("true".equalsIgnoreCase(soldObj.toString()) || "1".equals(soldObj.toString()));
        } else {
            goods.setSoldOut(false);
        }
        
        goods.setDetail(getString(row, "detail"));
        goods.setWatchRank(getString(row, "watch_rank"));
        goods.setCategoryId(getString(row, "category_id"));
        return goods;
    }
    
    /**
     * DBの取得結果マップリストを CustomData のリストに変換します。
     */
    private List<CustomData> mapToCustomDataList(List<Map<String, Object>> rows) {
        List<CustomData> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            CustomData custom = new CustomData();
            custom.setCustomId(getInt(row, "custom_id"));
            custom.setGoodsName(getString(row, "goods_name"));
            custom.setPrice(getInt(row, "price"));
            custom.setCalorie(getInt(row, "calorie"));
            custom.setAllergy(getString(row, "allergy"));
            // sold_out の値を CustomData にマッピングする
            Object soldObj = getValue(row, "sold_out");
            if (soldObj instanceof Boolean) {
                custom.setSoldOut((Boolean) soldObj);
            } else if (soldObj != null) {
                custom.setSoldOut("true".equalsIgnoreCase(soldObj.toString()) || "1".equals(soldObj.toString()));
            } else {
                custom.setSoldOut(false);
            }
            list.add(custom);
        }
        return list;
    }

    // --- マッピング用のヘルパーメソッド ---
    private Object getValue(Map<String, Object> row, String key) {
        if (row.containsKey(key)) return row.get(key);
        if (row.containsKey(key.toUpperCase())) return row.get(key.toUpperCase());
        if (row.containsKey(key.toLowerCase())) return row.get(key.toLowerCase());
        return null;
    }

    private String getString(Map<String, Object> row, String key) {
        Object val = getValue(row, key);
        return val != null ? val.toString() : null;
    }

    private Integer getInt(Map<String, Object> row, String key) {
        Object val = getValue(row, key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        } else if (val != null) {
            try {
                return Integer.parseInt(val.toString());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
}
