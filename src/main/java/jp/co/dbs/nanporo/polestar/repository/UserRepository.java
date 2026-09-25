package jp.co.dbs.nanporo.polestar.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    /** ユーザ情報をメールアドレスで1件取得するSQL */
    private static final String SELECT_BY_MAIL = 
            "SELECT * FROM user_m WHERE mail = :mail";

    /**
     * メールアドレスを条件にユーザ情報を取得します。
     * @param data ユーザデータ
     * @return 取得結果のマップリスト
     */
    public List<Map<String, Object>> findByMail(UserData data) {
        Map<String, Object> params = new HashMap<>();
        params.put("mail", data.getMail());

        return jdbc.queryForList(SELECT_BY_MAIL, params);
    }

    // 従業員表示
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
    
    // 従業員の総件数を取得
    public int countStaffList() {
        return jdbc.queryForObject(COUNT_STAFF_LIST, new HashMap<>(), Integer.class);
    }

        // 顧客表示
    private static final String COUNT_CUSTOMER_LIST = 
        "SELECT COUNT(*) FROM user_m WHERE role = '顧客'";

    public List<Map<String, Object>> getCustomerList(Pageable pageable, String sort) {

        String order = "desc".equalsIgnoreCase(sort) ? "DESC" : "ASC";

        // 権限が顧客のユーザ一覧を取得
        String SELECT_CUSTOMER_LIST = 
        "SELECT * FROM user_m WHERE role = '顧客' ORDER BY mail " + order +  " LIMIT :limit OFFSET :offset";

        // クエリのパラメータを設定するマップ
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("limit", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        // SELECT_STAFF_LISTクエリを実行し、結果を取得
        return jdbc.queryForList(SELECT_CUSTOMER_LIST, params);
    }
    
    // 顧客の総件数を取得
    public int countCustomerList() {
        return jdbc.queryForObject(COUNT_CUSTOMER_LIST, new HashMap<>(), Integer.class);
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
    public void deleteUser(String mail) {
        String sql = "DELETE FROM user_m WHERE mail = :mail";
        
        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);
        
        jdbc.update(sql, params);
    }

    // 新規登録
    public void register(String mail, String name, String password, String role, String rank) {
        String sql = "INSERT INTO user_m (mail, name, password, role, member_rank, gender, birthday, cancel_count, alive, point, point_card_complete) "
        + "VALUES (:mail, :name, :password, :role, :member_rank, :gender, :birthday, :cancel_count, false, 0, 0 )";
        
        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);
        params.put("name", name);
        params.put("password", password);
        params.put("role", role);
        params.put("member_rank", rank);
        params.put("gender", "未");
        params.put("birthday", java.sql.Date.valueOf("1000-01-01"));
        params.put("cancel_count", 0);

        jdbc.update(sql, params);
    }

    // 停止
    public void stopUser(String mail) {
        String sql = "UPDATE user_m SET alive = true WHERE mail = :mail";

        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);

        jdbc.update(sql, params);
    }

    // 解除
    public void resumeUser(String mail) {
        String sql = "UPDATE user_m SET alive = false WHERE mail = :mail";

        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);

        jdbc.update(sql, params);
    }

    // 予約数
    public int countOrder(LocalDate getTime) {
        String sql = "SELECT COUNT(*) FROM order_t WHERE DATE(get_time) = :getTime AND status != 'キャンセル'";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("getTime", getTime);

        int count = jdbc.queryForObject(sql, params, Integer.class);

        return count;
    }

    // 休業日追加
    public void insertClose(LocalDate today, String type) {
        String sql = "INSERT INTO close_t (close_day, close_type) VALUES (:today, :type)";

        Map<String, Object> params = new HashMap<>();
        params.put("today", today);
        params.put("type", type);

        jdbc.update(sql, params);
    }

    // 売上フラッシュ
    // 時間帯別集計用のDTOクラス
    public record SalesFlashDto(String timeRange, int totalSales, int customerCount) {}

    public SalesFlashDto getHourlySalesFlash(String timeRange, LocalDateTime start, LocalDateTime end) {

        String sql = """
            SELECT 
                COALESCE(SUM(sum_money), 0) AS total_sales,
                COUNT(*) AS customer_count
            FROM order_t
            WHERE get_time BETWEEN :start AND :end
            AND status != 'キャンセル'
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("start", start);
        params.put("end", end);

        return jdbc.queryForObject(sql, params, (rs, rowNum) -> new SalesFlashDto(
            timeRange,
            rs.getInt("total_sales"),
            rs.getInt("customer_count")
        ));
    }

    // 顧客のメールアドレスを取得
    public List<String> findCustomerEmails() {
        String sql = "SELECT mail FROM user_m WHERE role = '顧客'";
        return jdbc.getJdbcTemplate().queryForList(sql, String.class);
    }

    // 通知ID最大値取得
    public int getMaxNoticeId() {
        String sql = "SELECT COALESCE(MAX(notice_id), 0) FROM notice_t";
        Integer maxId = jdbc.getJdbcTemplate().queryForObject(sql, Integer.class);
        return maxId != null ? maxId : 0;
    }

    // 通知登録
    public void insertNoticeWithId(int noticeId, String mail, LocalDateTime registerTime, String content) {
        String sql = """
            INSERT INTO notice_t (notice_id, mail, register_time, content)
            VALUES (:noticeId, :mail, :registerTime, :content)
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("noticeId", noticeId);
        params.put("mail", mail);
        params.put("registerTime", registerTime);
        params.put("content", content);

        jdbc.update(sql, params);
    }

    // ユーザ情報変更（パスワードなし）
    public  void updateNoPassword(String mail, String nowMail, String name) {
        String sql ="""
                UPDATE user_m
                SET mail = :mail,
                    name = :name
                WHERE mail = :nowMail
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);
        params.put("nowMail", nowMail);
        params.put("name", name);

        jdbc.update(sql, params);
    }

    // ユーザ情報変更（パスワードあり）
    public  void updateYesPassword(String mail, String nowMail, String name, String password) {
        String sql ="""
                UPDATE user_m
                SET mail = :mail,
                    name = :name,
                    password = :password
                WHERE mail = :nowMail
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);
        params.put("nowMail", nowMail);
        params.put("name", name);
        params.put("password", password);

        jdbc.update(sql, params);
    }

    // 性別、誕生日更新
    public void updateProfile(String mail, String gender, String birthday) {
        String sql = """
                UPDATE user_m
                SET gender = :gender,
                    birthday = :birthday
                WHERE mail = :mail
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);
        params.put("gender", gender);
        params.put("birthday", java.sql.Date.valueOf(birthday));

        jdbc.update(sql, params);
    }
}