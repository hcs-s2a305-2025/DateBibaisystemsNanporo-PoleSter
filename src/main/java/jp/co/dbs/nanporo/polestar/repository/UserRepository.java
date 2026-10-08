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


    /** 店員・店長の総件数を取得するSQL */
    private static final String COUNT_STAFF_LIST = 
        "SELECT COUNT(*) FROM user_m WHERE role = '店員' OR role = '店長'";

    /**
     * 従業員（店員・店長）の一覧をページネーションおよびソート条件付きで取得します。
     *
     * @param pageable ページネーション情報（ページサイズ、オフセットなど）
     * @param sort ソート順（"desc" の場合は降順、それ以外は昇順）
     * @return 従業員情報のマップリスト
     */
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
    
    /**
     * 従業員（店員・店長）の総件数を取得します。
     *
     * @return 従業員の総件数
     */
    public int countStaffList() {
        return jdbc.queryForObject(COUNT_STAFF_LIST, new HashMap<>(), Integer.class);
    }


    /** 顧客の総件数を取得するSQL */
    private static final String COUNT_CUSTOMER_LIST = 
        "SELECT COUNT(*) FROM user_m WHERE role = '顧客'";

    /**
     * 顧客の一覧をページネーションおよびソート条件付きで取得します。
     *
     * @param pageable ページネーション情報（ページサイズ、オフセットなど）
     * @param sort ソート順（"desc" の場合は降順、それ以外は昇順）
     * @return 顧客情報のマップリスト
     */
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
    
    /**
     * 顧客の総件数を取得します。
     *
     * @return 顧客の総件数
     */
    public int countCustomerList() {
        return jdbc.queryForObject(COUNT_CUSTOMER_LIST, new HashMap<>(), Integer.class);
    }


    /**
     * 指定されたメールアドレスに一致するユーザ情報を1件取得します。
     *
     * @param mail メールアドレス
     * @return 取得したユーザ情報のマップ
     */
    public Map<String, Object> findByMail(String mail) {
        String sql = "SELECT * FROM user_m WHERE mail = :mail";
        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);
        return jdbc.queryForMap(sql, params);
    }


    /**
     * 従業員（または指定ユーザ）の名前、権限、利用状態（alive）を更新します。
     *
     * @param mail 更新対象のメールアドレス
     * @param name 新しい名前
     * @param role 新しい権限
     * @param alive アカウント有効フラグ
     */
    public void updateStaff(String mail, String name, String role, boolean alive) {
        String sql = "UPDATE user_m SET name = :name, role = :role, alive = :alive WHERE mail = :mail";
        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);
        params.put("name", name);
        params.put("role", role);
        params.put("alive", alive);
        jdbc.update(sql, params);
    }


    /**
     * 指定されたメールアドレスのユーザ情報を削除します。
     *
     * @param mail 削除対象のメールアドレス
     */
    public void deleteUser(String mail) {
        String sql = "DELETE FROM user_m WHERE mail = :mail";
        
        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);
        
        jdbc.update(sql, params);
    }


    /**
     * ユーザ情報を新規登録します。（初期値として性別は「未」、誕生日は「1000-01-01」、aliveはfalseで設定されます）
     *
     * @param mail メールアドレス
     * @param name 氏名
     * @param password パスワード
     * @param role 権限
     * @param rank 会員ランク
     */
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


    /**
     * アカウントを停止状態に更新します（alive フラグを true に設定）。
     *
     * @param mail 対象のメールアドレス
     */
    public void stopUser(String mail) {
        String sql = "UPDATE user_m SET alive = true WHERE mail = :mail";

        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);

        jdbc.update(sql, params);
    }


    /**
     * アカウントの停止状態を解除します（alive フラグを false に設定）。
     *
     * @param mail 対象のメールアドレス
     */
    public void resumeUser(String mail) {
        String sql = "UPDATE user_m SET alive = false WHERE mail = :mail";

        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);

        jdbc.update(sql, params);
    }


    /**
     * 指定された日付の有効な注文数（キャンセルを除く）を取得します。
     *
     * @param getTime 対象の日付
     * @return 注文数
     */
    public int countOrder(LocalDate getTime) {
        String sql = "SELECT COUNT(*) FROM order_t WHERE CAST(get_time AS DATE) = :getTime AND status != 'キャンセル'";

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("getTime", getTime);

        int count = jdbc.queryForObject(sql, params, Integer.class);

        return count;
    }


    /**
     * 休業日情報を登録します。
     *
     * @param today 休業日
     * @param type 休業日種別
     */
    public void insertClose(LocalDate today, String type) {
        String sql = "INSERT INTO close_t (close_day, close_type) VALUES (:today, :type)";

        Map<String, Object> params = new HashMap<>();
        params.put("today", today);
        params.put("type", type);

        jdbc.update(sql, params);
    }


    /**
     * 時間帯別の売上集計データを保持するレコード。
     *
     * @param timeRange 時間帯を表す文字列（例: "10:00-11:00"）
     * @param totalSales 合計売上金額
     * @param customerCount 顧客数（注文数）
     */
    public record SalesFlashDto(String timeRange, int totalSales, int customerCount) {}

    /**
     * 指定された日時範囲内の売上合計金額および顧客数を取得します（キャンセルは除く）。
     *
     * @param timeRange 時間帯を表す文字列
     * @param start 集計開始日時
     * @param end 集計終了日時
     * @return 時間帯別売上集計結果 {@link SalesFlashDto}
     */
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

    /**
     * 権限が「顧客」であるすべてのユーザのメールアドレスリストを取得します。
     *
     * @return 顧客のメールアドレスのリスト
     */
    public List<String> findCustomerEmails() {
        String sql = "SELECT mail FROM user_m WHERE role = '顧客'";
        return jdbc.getJdbcTemplate().queryForList(sql, String.class);
    }

    /**
     * 通知テーブル（notice_t）内の通知IDの最大値を取得します。データが存在しない場合は 0 を返します。
     *
     * @return 通知IDの最大値（存在しない場合は 0）
     */
    public int getMaxNoticeId() {
        String sql = "SELECT COALESCE(MAX(notice_id), 0) FROM notice_t";
        Integer maxId = jdbc.getJdbcTemplate().queryForObject(sql, Integer.class);
        return maxId != null ? maxId : 0;
    }

    /**
     * 指定されたIDで通知情報を登録します。
     *
     * @param noticeId 通知ID
     * @param mail 送信先メールアドレス
     * @param registerTime 登録日時
     * @param content 通知内容
     */
    public void insertNoticeWithId(String mail, LocalDateTime registerTime, String content) {
        String sql = """
            INSERT INTO notice_t (mail, register_time, content)
            VALUES (:mail, :registerTime, :content)
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);
        params.put("registerTime", registerTime);
        params.put("content", content);

        jdbc.update(sql, params);
    }


    // 指定されたメールアドレスの通知一覧を取得
    public List<Map<String, Object>> findNotificationsByMail(String mail) {

        String sql = """
            SELECT notice_id, mail, register_time, content
            FROM notice_t
            WHERE mail = :mail
            ORDER BY register_time DESC, notice_id DESC
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);

        return jdbc.queryForList(sql, params);
    }

    
    /**
     * ユーザの基本情報（メールアドレス・名前・アイコン）を更新します（パスワード更新なし）。
     *
     * @param mail 新しいメールアドレス
     * @param nowMail 現在（変更前）のメールアドレス
     * @param name 新しい名前
     * @param icon 新しいアイコン
     */
    public  void updateNoPassword(String mail, String nowMail, String name, String icon) {
        String sql ="""
                UPDATE user_m
                SET mail = :mail,
                    name = :name,
                    icon = :icon
                WHERE mail = :nowMail
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);
        params.put("nowMail", nowMail);
        params.put("name", name);
        params.put("icon", icon);

        jdbc.update(sql, params);
    }

    /**
     * ユーザの基本情報（メールアドレス・名前・アイコン）およびパスワードを更新します。
     *
     * @param mail 新しいメールアドレス
     * @param nowMail 現在（変更前）のメールアドレス
     * @param name 新しい名前
     * @param password 新しいパスワード
     * @param icon 新しいアイコン
     */
    public  void updateYesPassword(String mail, String nowMail, String name, String password, String icon) {
        String sql ="""
                UPDATE user_m
                SET mail = :mail,
                    name = :name,
                    password = :password,
                    icon = :icon
                WHERE mail = :nowMail
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);
        params.put("nowMail", nowMail);
        params.put("name", name);
        params.put("password", password);
        params.put("icon", icon);

        jdbc.update(sql, params);
    }

    /**
     * ユーザのプロフィール情報（性別および生年月日）を更新します。
     *
     * @param mail 対象のメールアドレス
     * @param gender 性別
     * @param birthday 生年月日（"YYYY-MM-DD" フォーマットの文字列）
     */
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

    /**
     * ユーザのポイント、ポイントカード完了数、会員ランクを更新します。
     *
     * @param mail 対象のメールアドレス
     * @param point 新しいポイント数
     * @param pointCardComplete 新しいカード完了数
     * @param memberRank 新しい会員ランク
     */
    public void updateMemberPointAndRank(String mail, int point, int pointCardComplete, String memberRank) {
        String sql = """
                UPDATE user_m
                SET point = :point,
                    point_card_complete = :pointCardComplete,
                    member_rank = :memberRank
                WHERE mail = :mail
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("mail", mail);
        params.put("point", point);
        params.put("pointCardComplete", pointCardComplete);
        params.put("memberRank", memberRank);

        jdbc.update(sql, params);
    }

    /**
     * 割引券を1枚追加します（スタンプカード達成時）。
     */
    public void addStampCoupon(String mail, int count) {
        jdbc.update("UPDATE user_m SET stamp_coupon = stamp_coupon + :count WHERE mail = :mail",
                Map.of("mail", mail, "count", count));
    }

    /**
     * 割引券を1枚消費して提示状態にします。券がない・既に提示中の場合は更新されません。
     *
     * @return 更新できた場合 true
     */
    public boolean activateStampCoupon(String mail) {
        return jdbc.update("""
                UPDATE user_m SET stamp_coupon = stamp_coupon - 1, stamp_coupon_active = TRUE
                WHERE mail = :mail AND stamp_coupon > 0 AND stamp_coupon_active = FALSE
                """, Map.of("mail", mail)) > 0;
    }

    /**
     * 提示中の割引を取り消し、割引券を1枚戻します。
     *
     * @return 更新できた場合 true
     */
    public boolean cancelStampCoupon(String mail) {
        return jdbc.update("""
                UPDATE user_m SET stamp_coupon = stamp_coupon + 1, stamp_coupon_active = FALSE
                WHERE mail = :mail AND stamp_coupon_active = TRUE
                """, Map.of("mail", mail)) > 0;
    }

    /**
     * 会計時に提示中フラグを確認して解除します。
     *
     * @return 提示中だった場合 true
     */
    public boolean useActiveStampCoupon(String mail) {
        return jdbc.update("""
                UPDATE user_m SET stamp_coupon_active = FALSE
                WHERE mail = :mail AND stamp_coupon_active = TRUE
                """, Map.of("mail", mail)) > 0;
    }

    /**
     * 休業日一覧を取得します。
     *
     * @return 休業日一覧
     */
        public List<Map<String, Object>> getCloseDay() {
        String sql = """
                SELECT *
                FROM close_t
                """;

        Map<String, Object> params = new HashMap<>();

        return jdbc.queryForList(sql, params);
    }

    /**
     * 指定された日付の予約を「キャンセル」に更新します。
     *
     * @param today 当日の日付
     */
    public void cancelUpdate(LocalDate today) {
        String sql = """
            UPDATE order_t
            SET status = 'キャンセル'
            WHERE get_time >= :startTime 
                AND get_time < :endTime
            """;

        // 当日の 00:00:00 と 翌日の 00:00:00 を作成
        LocalDateTime startTime = today.atStartOfDay();
        LocalDateTime endTime = today.plusDays(1).atStartOfDay();

        Map<String, Object> params = new HashMap<>();
        params.put("startTime", startTime);
        params.put("endTime", endTime);

        jdbc.update(sql, params);
    }
}