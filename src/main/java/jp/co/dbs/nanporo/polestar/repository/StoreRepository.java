package jp.co.dbs.nanporo.polestar.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import jp.co.dbs.nanporo.polestar.entity.CustomEntity;
import jp.co.dbs.nanporo.polestar.entity.GoodsEntity;
import jp.co.dbs.nanporo.polestar.entity.SetGoodsEntity;
import jp.co.dbs.nanporo.polestar.request.GoodsEditRequest;

@Repository 
public class StoreRepository {
    
    @Autowired 
    private NamedParameterJdbcTemplate jdbc;

    /* ==================================================
     *  商品マスタ（goods_m）操作用SQL
     * ================================================== */

    // 全商品情報を取得するSQL
    private static final String SELECT_ALL_GOODS = 
            "SELECT goods_id, goods_name, price, photo, sold_out "
            + "FROM goods_m "
            + "ORDER BY goods_id ASC";

    /**
     * 全商品の一覧を取得します。
     */
    public List<Map<String, Object>> getAllGoods(){
        return jdbc.queryForList(SELECT_ALL_GOODS, Map.of());
    }

    // 商品ID指定で1件取得するSQL
    private static final String SELECT_GOODS_BY_ID = 
        "SELECT goods_id, goods_name, price, photo, calorie, allergy, zangi_count, sold_out, detail, watch_rank "
        + "FROM goods_m WHERE goods_id = :goodsId";

    /**
     * 商品IDをキーに商品情報を1件取得する
     */
    public Map<String, Object> getGoodsById(String goodsId) {
        List<Map<String, Object>> list = jdbc.queryForList(SELECT_GOODS_BY_ID, Map.of("goodsId", goodsId));
        return list.isEmpty() ? null : list.get(0);
    }

    // カテゴリ別（プレフィックス）の当日の最大商品IDを取得するSQL
    private static final String SELECT_MAX_GOODS_ID_BY_PREFIX = 
            "SELECT goods_id FROM goods_m "
            + "WHERE goods_id LIKE :prefix "
            + "ORDER BY goods_id DESC LIMIT 1";

    /**
     * 商品カテゴリの英字（B:弁当, S:サイド, U:裏商品）から次の商品ID（例: "B007"）を自動採番します。
     */
    public String generateGoodsId(String categoryPrefix) {
        if (categoryPrefix == null || categoryPrefix.isEmpty()) {
            categoryPrefix = "B"; // デフォルトは弁当(B)
        }

        Map<String, Object> params = new HashMap<>();
        params.put("prefix", categoryPrefix + "%");

        List<String> resultList = jdbc.queryForList(SELECT_MAX_GOODS_ID_BY_PREFIX, params, String.class);

        int nextSeq = 1;
        if (!resultList.isEmpty() && resultList.get(0) != null) {
            String maxGoodsId = resultList.get(0); // 例: "B006"
            String numStr = maxGoodsId.substring(1); // "006"
            nextSeq = Integer.parseInt(numStr) + 1;
        }

        // アルファベット + 3桁数字のゼロ埋め（例: B + 007 -> "B007"）
        return String.format("%s%03d", categoryPrefix, nextSeq);
    }

    // 新規商品を登録するSQL
    private static final String INSERT_GOODS = 
            "INSERT INTO goods_m ("
            + "goods_id, goods_name, photo, price, calorie, allergy, zangi_count, sold_out, detail, watch_rank"
            + ") VALUES ("
            + ":goodsId, :goodsName, :photo, :price, :calorie, :allergy, :zangiCount, :soldOut, :detail, :watchRank"
            + ")";

    /**
     * 新規商品をデータベースに登録します。
     * @param newGoodsId 採番済みの商品ID（例: "B007"）
     * @param req リクエストパラメータ
     * @param photoPath 画像のパス
     * @param allergyCsv アレルギーのカンマ区切り文字列
     */
    public int insertGoods(String newGoodsId, GoodsEditRequest req, String photoPath, String allergyCsv) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("goodsId", newGoodsId);
        params.addValue("goodsName", req.getGoodsName());
        params.addValue("photo", photoPath);
        params.addValue("price", toInteger(req.getPrice()));
        params.addValue("calorie", toInteger(req.getCalorie()));
        params.addValue("allergy", allergyCsv);
        params.addValue("zangiCount", toInteger(req.getZangiCount()));
        params.addValue("soldOut", req.getSoldOut() != null ? req.getSoldOut() : false);
        params.addValue("detail", req.getDetail());
        params.addValue("watchRank", req.getRank() != null ? req.getRank() : "一般");

        return jdbc.update(INSERT_GOODS, params);
    }

    // 商品情報を更新するSQL
    private static final String UPDATE_GOODS = 
            "UPDATE goods_m SET "
            + "goods_name = :goodsName, "
            + "price = :price, "
            + "calorie = :calorie, "
            + "allergy = :allergy, "
            + "zangi_count = :zangiCount, "
            + "sold_out = :soldOut, "
            + "detail = :detail, "
            + "watch_rank = :watchRank, "
            + "photo = COALESCE(:photo, photo) " // 画像がアップロードされなかった場合は既存の画像を保持
            + "WHERE goods_id = :goodsId";

    /**
     * 既存の商品情報を更新します。
     */
    public int updateGoods(GoodsEditRequest req, String photoPath, String allergyCsv) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("goodsId", req.getGoodsId());
        params.addValue("goodsName", req.getGoodsName());
        params.addValue("price", toInteger(req.getPrice()));
        params.addValue("calorie", toInteger(req.getCalorie()));
        params.addValue("allergy", allergyCsv);
        params.addValue("zangiCount", toInteger(req.getZangiCount()));
        params.addValue("soldOut", req.getSoldOut() != null ? req.getSoldOut() : false);
        params.addValue("detail", req.getDetail());
        params.addValue("watchRank", req.getRank());
        params.addValue("photo", photoPath);

        return jdbc.update(UPDATE_GOODS, params);
    }

    /**
     * オブジェクトや文字列を安全に Integer に変換するヘルパーメソッド
     */
    private Integer toInteger(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            String str = value.toString().trim();
            return str.isEmpty() ? 0 : Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /* ==================================================
     *  販売ステータス（sold_out）更新SQL
    * ================================================== */
    private static final String UPDATE_SOLD_OUT = 
            "UPDATE goods_m SET sold_out = :soldOut WHERE goods_id = :goodsId";

    /**
     * 商品の販売ステータス（sold_out）を切り替えます。
     */
    public int updateSoldOut(String goodsId, boolean soldOut) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("goodsId", goodsId);
        params.addValue("soldOut", soldOut);

        return jdbc.update(UPDATE_SOLD_OUT, params);
    }
    /**
     * 商品IDが Integer の場合のオーバーロードメソッド
     */
    public int updateSoldOut(Integer goodsId, boolean soldOut) {
        if (goodsId == null) return 0;
        return updateSoldOut(String.valueOf(goodsId), soldOut);
    }

    /* ==================================================
     *  カスタム／トッピングマスタ（custom_m）操作用SQL
     * ================================================== */
    
    // ごはんオプション取得 (custom_id: 10〜40)
    private static final String SELECT_RICE_CUSTOMS = 
            "SELECT custom_id, goods_name, price, calorie, allergy, sold_out FROM custom_m "
            + "WHERE custom_id BETWEEN 10 AND 49 ORDER BY custom_id ASC";

    public List<Map<String, Object>> getRiceCustoms() {
        return jdbc.queryForList(SELECT_RICE_CUSTOMS, Map.of());
    }

    // ソースオプション取得 (custom_id: 50〜90)
    private static final String SELECT_SAUCE_CUSTOMS = 
            "SELECT custom_id, goods_name, price, calorie, allergy, sold_out FROM custom_m "
            + "WHERE custom_id BETWEEN 50 AND 100 ORDER BY custom_id ASC";

    public List<Map<String, Object>> getSauceCustoms() {
        return jdbc.queryForList(SELECT_SAUCE_CUSTOMS, Map.of());
    }

    // ごはんオプションの最大IDを取得 (10〜40)
    private static final String SELECT_MAX_RICE_CUSTOM_ID = 
            "SELECT custom_id FROM custom_m "
            + "WHERE custom_id BETWEEN 10 AND 40 "
            + "ORDER BY custom_id DESC LIMIT 1";

    /**
     * ごはんオプションの新規ID（10〜40）を自動採番します。
     */
    public int generateRiceCustomId() {
        List<Integer> resultList = jdbc.queryForList(SELECT_MAX_RICE_CUSTOM_ID, new HashMap<>(), Integer.class);
        if (!resultList.isEmpty() && resultList.get(0) != null) {
            return resultList.get(0) + 1;
        }
        return 10; // 初期値
    }

    // ソースオプションの最大IDを取得 (50〜89)
    private static final String SELECT_MAX_SAUCE_CUSTOM_ID = 
            "SELECT custom_id FROM custom_m "
            + "WHERE custom_id BETWEEN 50 AND 89 "
            + "ORDER BY custom_id DESC LIMIT 1";

    /**
     * ソースオプションの新規ID（50〜89）を自動採番します。
     */
    public int generateSauceCustomId() {
        List<Integer> resultList = jdbc.queryForList(SELECT_MAX_SAUCE_CUSTOM_ID, new HashMap<>(), Integer.class);
        if (!resultList.isEmpty() && resultList.get(0) != null) {
            return resultList.get(0) + 1;
        }
        return 50; // 初期値
    }

    // トッピング（カスタム）新規作成SQL
    private static final String INSERT_CUSTOM = 
            "INSERT INTO custom_m ("
            + "custom_id, goods_name, price, calorie, allergy, sold_out"
            + ") VALUES ("
            + ":customId, :goodsName, :price, :calorie, :allergy, :sold_out"
            + ")";

    /**
     * トッピング（カスタムオプション）を新規登録します。
     */
    public int insertCustom(Integer customId, String goodsName, Integer price, Integer calorie, String allergy, Boolean soldOut) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("customId", customId);
        params.addValue("goodsName", goodsName);
        params.addValue("price", price);
        params.addValue("calorie", calorie);
        params.addValue("allergy", allergy);
        params.addValue("sold_out", soldOut);

        return jdbc.update(INSERT_CUSTOM, params);
    }

    // トッピング（カスタム）更新SQL
    private static final String UPDATE_CUSTOM = 
            "UPDATE custom_m SET "
            + "goods_name = :goodsName, "
            + "price = :price, "
            + "calorie = :calorie, "
            + "allergy = :allergy, "
            + "sold_out = :sold_out "
            + "WHERE custom_id = :customId";

    /**
     * トッピング（カスタムオプション）情報を更新します。
     */
    public int updateCustom(Integer customId, String goodsName, Integer price, Integer calorie, String allergy, Boolean soldOut) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("customId", customId);
        params.addValue("goodsName", goodsName);
        params.addValue("price", price);
        params.addValue("calorie", calorie);
        params.addValue("allergy", allergy);
        params.addValue("sold_out", soldOut);

        return jdbc.update(UPDATE_CUSTOM, params);
    }

    // トッピング削除SQL
    private static final String DELETE_CUSTOM = 
            "DELETE FROM custom_m WHERE custom_id = :customId";

    /**
     * 指定されたトッピング（カスタム）を削除します。
     */
    public int deleteCustom(Integer customId) {
        return jdbc.update(DELETE_CUSTOM, Map.of("customId", customId));
    }

    // 全商品のすべての情報をしゅとくするSQL
    private static final String SELECT_ALL = 
        "SELECT * FROM goods_m";

    /**
     * 全商品のすべての情報を取得します。
     */
    public List<Map<String, Object>> getAll(){
        return jdbc.queryForList(SELECT_ALL, Map.of());
    }

    /**
     * 商品IDをキーに GoodsEntity を1件取得する（Optional形式）
     */
    public Optional<GoodsEntity> getGoodsEntityById(String goodsId) {
        List<GoodsEntity> list = jdbc.query(
            SELECT_GOODS_BY_ID, 
            Map.of("goodsId", goodsId), 
            new BeanPropertyRowMapper<>(GoodsEntity.class)
        );
        return list.stream().findFirst();
    }

    /* ==================================================
     *  セット商品（set_goods_m）／カスタム（custom_m）Entity取得用SQL
     * ================================================== */
    private static final String SELECT_SET_GOODS_BY_ID = 
        "SELECT * FROM set_goods_m WHERE set_goods_id = :setGoodsId";

    private static final String SELECT_CUSTOM_BY_ID = 
        "SELECT * FROM custom_m WHERE custom_id = :customId";

    /**
     * セット商品IDをキーに SetGoodsEntity を1件取得する（Optional形式）
     */
    public Optional<SetGoodsEntity> getSetGoodsEntityById(Integer setGoodsId) {
        if (setGoodsId == null) {
            return Optional.empty();
        }
        List<SetGoodsEntity> list = jdbc.query(
            SELECT_SET_GOODS_BY_ID, 
            Map.of("setGoodsId", setGoodsId), 
            new BeanPropertyRowMapper<>(SetGoodsEntity.class)
        );
        return list.stream().findFirst();
    }

    /**
     * カスタムIDをキーに CustomEntity を1件取得する（Optional形式）
     */
    public Optional<CustomEntity> getCustomEntityById(Integer customId) {
        if (customId == null) {
            return Optional.empty();
        }
        List<CustomEntity> list = jdbc.query(
            SELECT_CUSTOM_BY_ID, 
            Map.of("customId", customId), 
            new BeanPropertyRowMapper<>(CustomEntity.class)
        );
        return list.stream().findFirst();
    }


}
