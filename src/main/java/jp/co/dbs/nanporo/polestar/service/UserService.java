package jp.co.dbs.nanporo.polestar.service;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jp.co.dbs.nanporo.polestar.data.UserData;
import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.repository.UserRepository;
import jp.co.dbs.nanporo.polestar.repository.UserRepository.SalesFlashDto;
import jp.co.dbs.nanporo.polestar.response.UserGetResponse;

@Service 
public class UserService {

    @Autowired 
    private UserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 指定されたメールアドレスからユーザを検索するメソッド
     * @param data ユーザデータ
     * @return UserEntity 検索されたユーザエンティティ
     * @throws UsernameNotFoundException 該当するユーザが存在しない場合
     */
    // ログイン画面で入力されたメールアドレスをもとに、ユーザ情報を取得する
    public UserEntity getUser(UserData data){
        // リポジトリへ問い合わせ
        List<Map<String, Object>> table = repository.findByMail(data);

        // テーブルからエンティティへ変換
        List<UserEntity> list = tableToEntity(table);

        if (list.size() == 1){
            return list.get(0);
        } else {
            throw new UsernameNotFoundException("ユーザが見つかりません： " + data.getMail());
        }
    }


    /**
     * テーブル（List<Map>）を UserEntity のリストに変換するヘルパー関数
     * LoginUserDetails で使用されるフィールド名に合わせてマッピングします。
     * @param table ユーザデータが含まれたテーブル
     * @return エンティティ型に変換されたリスト
     */
    private List<UserEntity> tableToEntity(List<Map<String, Object>> table) {
        List<UserEntity> list = new ArrayList<>();

        for (Map<String, Object> row : table) {
            UserEntity entity = new UserEntity();
            entity.setMail((String) row.get("mail"));
            entity.setPassword((String) row.get("password"));
            entity.setRole((String) row.get("role"));
            entity.setName((String) row.get("name"));
            entity.setAlive((Boolean) row.get("alive"));
            
            list.add(entity);
        }

        return list;
    }


    /**
     * 従業員一覧を取得するメソッド
     * @param pageable ページ
     *        sort     ソート
     * @return UserGetResponse 従業員一覧リスト
     */
    public  UserGetResponse getStaffList(Pageable pageable, String sort) {
        // リポジトリに処理を依頼
        List<Map<String, Object>> resultSet = repository.getStaffList(pageable, sort);
        
        // テーブル構成からエンティティクラスへ変換
        List<UserEntity> staffList = toResponse(resultSet);

        int totalCount = repository.countStaffList();
        int totalPages = (int) Math.ceil((double) totalCount / pageable.getPageSize());

        // レスポンスクラスを生成
        UserGetResponse response = new UserGetResponse();
        response.setUsers(staffList);
        response.setTotalPages(totalPages);
        return response;
    }


    /**
     * 顧客一覧を取得するメソッド
     * @param pageable ページ
     *        sort     ソート
     * @return UserGetResponse 顧客一覧リスト
     */
    public  UserGetResponse getCustomerList(Pageable pageable, String sort) {
        // リポジトリに処理を依頼
        List<Map<String, Object>> resultSet = repository.getCustomerList(pageable, sort);
        
        // テーブル構成からエンティティクラスへ変換
        List<UserEntity> customerList = toResponse(resultSet);

        int totalCount = repository.countCustomerList();
        int totalPages = (int) Math.ceil((double) totalCount / pageable.getPageSize());

        // レスポンスクラスを生成
        UserGetResponse response = new UserGetResponse();
        response.setUsers(customerList);
        response.setTotalPages(totalPages);
        return response;
    }

    
    /**
     * UserEntityへ値を設定するメソッド
     * @param resultSet リザルト
     * @return users 一覧をセットしたリスト
     */
    private List<UserEntity> toResponse(List<Map<String, Object>> resultSet) {
        // 配列の初期化
        List<UserEntity> users = new ArrayList<UserEntity>();

        // テーブル行ごとの繰り返し
        for (Map<String, Object> row : resultSet) {
            UserEntity user = new UserEntity();
            user.setMail((String) row.get("mail"));
            user.setName((String) row.get("name"));
            user.setPassword((String) row.get("password"));
            user.setRole((String) row.get("role"));
            user.setMemberRank((String) row.get("member_rank"));
            user.setGender((String) row.get("gender"));
            user.setBirthday((Date) row.get("birthday"));
            user.setCancelCount((int) row.get("cancel_count"));
            user.setAlive((boolean) row.get("alive"));
            user.setPoint((int) row.get("point"));
            user.setPointCardComplete((int) row.get("point_card_complete"));

            users.add(user);
        }
        return users;
    }


    /**
     * メールアドレスから特定の1件を取得するメソッド
     * @param mail メールアドレス
     * @return user 
     */
    public UserEntity findByMail(String mail) {
        Map<String, Object> row = repository.findByMail(mail);
        
        UserEntity user = new UserEntity();
        user.setMail((String) row.get("mail"));
        user.setName((String) row.get("name"));
        user.setRole((String) row.get("role"));
        user.setAlive((Boolean) row.get("alive"));

        return user;
    }


    /**
     * 従業員の情報をアップデートするメソッド
     * @param mail メールアドレス
     *        name 名前
     *        role 権限
     *        alive 状態
     */
    public void updateStaff(String mail, String name, String role, boolean alive) {
        repository.updateStaff(mail, name, role, alive);
    }


    /**
     * ユーザを削除するメソッド
     * @param mail メールアドレス
     */
    public void deleteUser(String mail) {
    repository.deleteUser(mail);
    }


    /**
     * 従業員を新規登録するメソッド
     * @param mail メールアドレス
     *        name 名前
     *        role 権限
     */
    public void registerStaff(String mail, String name, String role) {
        String password = passwordEncoder.encode("password"); // 初期パスワード:password
        repository.register(mail, name, password, role, "一般");
    }


    /**
     * ユーザを利用停止にするメソッド
     * @param mail メールアドレス
     */
    public void stopUser(String mail) {
        repository.stopUser(mail);
    }


    /**
     * ユーザを停止解除するメソッド
     * @param mail メールアドレス
     */
    public void resumeUser(String mail) {
        repository.resumeUser(mail);
    }


    /**
     * 今日の予約件数を取得するメソッド
     * @return cnt 予約件数
     */
    public int countOrder() {
        // 今日の日付
        LocalDate today = LocalDate.now();
        int cnt = repository.countOrder(today);

        return cnt;
    }


    /**
     * 休業日を追加するメソッド
     */
    public void insertClose() {
        // 今日の日付
        LocalDate today = LocalDate.now();
        repository.insertClose(today, "臨時休業");
    }


    /**
     * 売上フラッシュの情報を取得するメソッド
     * @return repository.getHourlySalesFlash(timeRange, start, end);
     */
    public SalesFlashDto getHourlySalesFlash() {
        // 現在の時間を取得
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = now.withMinute(59).withSecond(59).withNano(999999999);

        // 表示用の時間帯文字列を作成
        String timeRange = String.format("%02d:00 〜 %02d:00", now.getHour(), now.getHour() + 1);

        return repository.getHourlySalesFlash(timeRange, start, end);
    }


    /**
     * 通知を登録するメソッド
     * @param content 通知内容
     */
    @Transactional
    public void sendBroadcastNotice(String content) {
        List<String> customerEmails = repository.findCustomerEmails();
        if (customerEmails.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 最大値取得
        int nextId = repository.getMaxNoticeId() + 1;

        // 顧客の人数分ループ
        for (String email : customerEmails) {
            repository.insertNoticeWithId(nextId, email, now, content);
            nextId++; // ID+1
        }
    }


    /**
     * システム自動停止を行うメソッド
     * @param dayName 曜日名
     *        weeks 何週間分
     */
    @Transactional
    public void closeDays(String dayName, int weeks) {
        DayOfWeek targetDay = parseDayOfWeek(dayName);

        if (targetDay == null){
            return;
        }

        LocalDate current = LocalDate.now().with(TemporalAdjusters.nextOrSame(targetDay));

        for (int i = 0; i < weeks; i++) {
            repository.insertClose(current, "定休日");
            current = current.plusWeeks(1);// 1週間後へ
        }
    }


    /**
     * 曜日名をシステムで使える形に変換するメソッド
     * @param dayName 曜日名
     * @return DayOfWeek.~
     */
    // DayOfWeekに変換
    private DayOfWeek parseDayOfWeek(String dayName) {
        switch (dayName) {
            case "月": return DayOfWeek.MONDAY;
            case "火": return DayOfWeek.TUESDAY;
            case "水": return DayOfWeek.WEDNESDAY;
            case "木": return DayOfWeek.THURSDAY;
            case "金": return DayOfWeek.FRIDAY;
            case "土": return DayOfWeek.SATURDAY;
            case "日": return DayOfWeek.SUNDAY;
            default: return null;
        }
    }


    /**
     * パスワードを比較するメソッド
     * @param mail メールアドレス
     *        password 受け取ったパスワード
     * @return result true/false
     */
    public boolean passwordCheck(String mail, String password) {

        boolean result = false;

        Map<String, Object> user = repository.findByMail(mail);
        String nowPassword = (String) user.get("password");

        if(passwordEncoder.matches(password, nowPassword)) {
            result = true;
        }

        return  result;
    }


    /**
     * ユーザ情報変更（パスワードなし）を行うメソッド
     * @param mail 変更するメールアドレス
     *        nowMail 現在のメールアドレス
     *        name 名前
     */
    public void updateNoPassword(String mail, String nowMail, String name) {
        repository.updateNoPassword(mail, nowMail, name);
    } 


    /**
     * ユーザ情報変更（パスワードあり）を行うメソッド
     * @param mail 変更するメールアドレス
     *        nowMail 現在のメールアドレス
     *        name 名前
     *        password パスワード
     */
    public void updateYesPassword(String mail, String nowMail, String name, String password) {

        password = passwordEncoder.encode(password);
        repository.updateYesPassword(mail, nowMail, name, password);
    }


    /**
     * 新規顧客登録を行うメソッド
     * @param mail メールアドレス
     *        password パスワード
     */
    public void registerCustomer(String mail, String password) {
        password = passwordEncoder.encode(password);
        repository.register(mail, mail, password, "顧客", "一般");
    }


    /**
     * 性別・誕生日の更新を行うメソッド
     * @param mail メールアドレス
     *        gender 性別
     *        birthday 誕生日
     */
    public void updateProfile(String mail, String gender, String birthday) {
        repository.updateProfile(mail, gender, birthday);
    }
}
