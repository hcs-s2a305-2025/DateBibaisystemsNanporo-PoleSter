package jp.co.dbs.nanporo.polestar.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import jp.co.dbs.nanporo.polestar.data.UserData;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@Repository 
public class UserRepository {

    // Javaでデータベース操作を行うライブラリ
    @Autowired 
    private NamedParameterJdbcTemplate jdbc;

    private static final String COUNT_STAFF_LIST = 
        "SELECT COUNT(*) FROM user_m WHERE role = '店員' OR role = '店長'";

    public List<Map<String, Object>> getStaffList(Pageable pageable, String sort) {

        String order = "desc".equalsIgnoreCase(sort) ? "DESC" : "ASC";

        // 権限が店員・店長のユーザ一覧を取得
        String SELECT_STAFF_LIST = 
        "SELECT * FROM user_m WHERE role = '店員' OR role = '店長' ORDER BY mail " + order +  " LIMIT :limit OFFSET :offset";

        // クエリのパラメータを設定するマップ
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        // SELECT_STAFF_LISTクエリを実行し、結果を取得
        return jdbc.queryForList(SELECT_STAFF_LIST, params);
    }
    
    // 総件数を取得
    public int countStaffList() {
        return jdbc.queryForObject(COUNT_STAFF_LIST, new HashMap<>(), Integer.class);
    }

    // 1件取得
    public Map<String, Object> findByMail(String mail) {
        String sql = "SELECT * FROM user_m WHERE mail = :mail";
        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);
        return jdbc.queryForMap(sql, params);
    }

    // 更新
    public void updateStaff(String mail, String name, String role, boolean alive) {
        String sql = "UPDATE user_m SET name = :name, role = :role, alive = :alive WHERE mail = :mail";
        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);
        params.put("name", name);
        params.put("role", role);
        params.put("alive", alive);
        jdbc.update(sql, params);
    }

    // 削除
    public void deleteStaff(String mail) {
        String sql = "DELETE FROM user_m WHERE mail = :mail";
        
        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);
        
        jdbc.update(sql, params);
    }

    // 新規登録
   public void registerStaff(String mail, String name, String role) {
    String sql = "INSERT INTO user_m (mail, name, password, role, member_rank, gender, birthday, cancel_count, alive, point, point_card_complete) "
    + "VALUES (:mail, :name, :password, :role, :member_rank, :gender, :birthday, :cancel_count, true, 0, 0 )";
    
    Map<String, Object> params = new HashMap<>();
    params.put("mail", mail);
    params.put("name", name);
    params.put("password", "password"); // 初期パスワード
    params.put("role", role);
    
    params.put("member_rank", "NONE");
    params.put("gender", "未");
    params.put("birthday", java.sql.Date.valueOf("2000-01-01"));
    params.put("cancel_count", 0);

    jdbc.update(sql, params);
}
}
