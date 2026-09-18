package jp.co.dbs.nanporo.polestar.service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    // 従業員の一覧取得
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

    // 顧客の一覧取得
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

    // 編集データ取得（１件）
    public UserEntity findByMail(String mail) {
        Map<String, Object> row = repository.findByMail(mail);
        
        UserEntity user = new UserEntity();
        user.setMail((String) row.get("mail"));
        user.setName((String) row.get("name"));
        user.setRole((String) row.get("role"));
        user.setAlive((Boolean) row.get("alive"));

        return user;
    }

    // 更新処理
    public void updateStaff(String mail, String name, String role, boolean alive) {
        repository.updateStaff(mail, name, role, alive);
    }

    // 削除処理
    public void deleteUser(String mail) {
    repository.deleteUser(mail);
    }

    // 新規登録処理
    public void registerStaff(String mail, String name, String role) {
        repository.registerStaff(mail, name, role);
    }

    // 停止処理
    public void stopUser(String mail) {
        repository.stopUser(mail);
    }

    // 解除処理
    public void resumeUser(String mail) {
        repository.resumeUser(mail);
    }

    // 予約数
    public int countOrder() {
        // 今日の日付
        LocalDate today = LocalDate.now();
        int cnt = repository.countOrder(today);

        return cnt;
    }

    // 休業日追加
    public void insertClose() {
        // 今日の日付
        LocalDate today = LocalDate.now();
        repository.insertClose(today, "臨時休業");
    }

    // 売上フラッシュ
    public SalesFlashDto getHourlySalesFlash() {
        // 現在の時間を取得
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = now.withMinute(59).withSecond(59).withNano(999999999);

        // 表示用の時間帯文字列を作成
        String timeRange = String.format("%02d:00 〜 %02d:00", now.getHour(), now.getHour() + 1);

        return repository.getHourlySalesFlash(timeRange, start, end);
    }

    // 一斉送信
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
}
