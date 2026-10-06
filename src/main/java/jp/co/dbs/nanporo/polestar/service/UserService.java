package jp.co.dbs.nanporo.polestar.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.transaction.Transactional;
import jp.co.dbs.nanporo.polestar.data.UserData;
import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.repository.StoreRepository;
import jp.co.dbs.nanporo.polestar.repository.UserRepository;
import jp.co.dbs.nanporo.polestar.repository.UserRepository.SalesFlashDto;
import jp.co.dbs.nanporo.polestar.response.UserGetResponse;

@Service 
public class UserService {

    /** ユーザリポジトリ */
    @Autowired 
    private UserRepository repository;

    /** 店舗リポジトリ */
    @Autowired 
    private  StoreRepository storeRepository;

    /** パスワードエンコーダー */
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${ollama.api.url:http://localhost:11434/api/generate}")
    private String aiApiUrl = "http://localhost:11434/api/generate";

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
     * 従業員一覧をページネーション情報付きで取得します。
     *
     * @param pageable ページネーション情報
     * @param sort ソート指定文字列（例: "asc", "desc"）
     * @return 従業員一覧およびページ数が含まれた {@link UserGetResponse}
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
     * 顧客一覧をページネーション情報付きで取得します。
     *
     * @param pageable ページネーション情報
     * @param sort ソート指定文字列（例: "asc", "desc"）
     * @return 顧客一覧およびページ数が含まれた {@link UserGetResponse}
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
     * リザルトセット（マップリスト）から {@link UserEntity} のリストへ変換・マッピングします。
     *
     * @param resultSet DB取得結果のマップリスト
     * @return マッピング後の {@link UserEntity} リスト
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
     * 指定されたメールアドレスに一致するユーザ詳細情報を1件取得します。
     *
     * @param mail メールアドレス
     * @return 取得した {@link UserEntity}
     */
    public UserEntity findByMail(String mail) {
        Map<String, Object> row = repository.findByMail(mail);
        
        UserEntity user = new UserEntity();
        user.setMail((String) row.get("mail"));
        user.setName((String) row.get("name"));
        user.setRole((String) row.get("role"));
        user.setMemberRank((String) row.get("member_rank"));
        user.setAlive((Boolean) row.get("alive"));
        user.setPoint((int) row.get("point"));
        user.setPointCardComplete((int) row.get("point_card_complete"));
        user.setGender((String) row.get("gender"));
        user.setBirthday((Date)row.get("birthday"));

        return user;
    }

    /**
     * 指定されたメールアドレスの通知一覧を取得します。
     *
     * @param mail ログインユーザーのメールアドレス
     * @return 通知一覧
     */
    public List<Map<String, Object>> getNotificationsByMail(String mail) {
        return repository.findNotificationsByMail(mail);
    }


    /**
     * 従業員の基本情報を更新します。
     *
     * @param mail メールアドレス
     * @param name 名前
     * @param role 権限（役割）
     * @param alive 利用状態フラグ
     */
    public void updateStaff(String mail, String name, String role, boolean alive) {
        repository.updateStaff(mail, name, role, alive);
    }


    /**
     * 指定されたメールアドレスのユーザを物理削除します。
     *
     * @param mail 削除対象のメールアドレス
     */
    public void deleteUser(String mail) {
    repository.deleteUser(mail);
    }


    /**
     * 従業員を新規登録します。初期パスワードは「password」で暗号化して登録され、初期ランクは「一般」となります。
     *
     * @param mail メールアドレス
     * @param name 氏名
     * @param role 権限（役割）
     */
    public void registerStaff(String mail, String name, String role) {
        String password = passwordEncoder.encode("password"); // 初期パスワード:password
        repository.register(mail, name, password, role, "一般");
    }


    /**
     * 指定したユーザを利用停止状態に設定します。
     *
     * @param mail メールアドレス
     */
    public void stopUser(String mail) {
        repository.stopUser(mail);
    }


    /**
     * 指定したユーザの利用停止状態を解除します。
     *
     * @param mail メールアドレス
     */
    public void resumeUser(String mail) {
        repository.resumeUser(mail);
    }


    /**
     * 当日（本日）の予約・注文件数を取得します。
     *
     * @return 本日の予約件数
     */
    public int countOrder() {
        // 今日の日付
        LocalDate today = LocalDate.now();
        int cnt = repository.countOrder(today);

        return cnt;
    }


    /**
     * 当日（本日）付で「臨時休業」を登録し、予約を「キャンセル」にします。
     */
    public void insertClose() {
        // 今日の日付
        LocalDate today = LocalDate.now();
        repository.insertClose(today, "臨時休業");

        // 今日の予約をキャンセルに
        repository.cancelUpdate(today);
    }


    /**
     * 現在時刻の属する1時間（例: 10:00〜11:00）の売上フラッシュ情報（売上合計・客数）を取得します。
     *
     * @return 売上フラッシュ集計結果 {@link SalesFlashDto}
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
     * すべての顧客ユーザに向けて一括通知（お知らせ）を登録します。
     *
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
     * 指定した曜日と指定した週数分だけ「定休日」を一括登録します。
     *
     * @param dayName 曜日名（例: "月", "火"）
     * @param weeks 登録する週数
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
     * 日本語の曜日文字（"月", "火" など）を {@link DayOfWeek} 列挙型に変換します。
     *
     * @param dayName 曜日名
     * @return 変換後の {@link DayOfWeek}（対象外の文字の場合は {@code null}）
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
     * 入力された平文パスワードと、DBに保存されている暗号化パスワードを照合します。
     *
     * @param mail 対象ユーザのメールアドレス
     * @param password 照合を行う平文パスワード
     * @return 一致している場合は {@code true}、一致しない場合は {@code false}
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
     * パスワード変更を伴わない形でユーザの基本情報（メールアドレス・名前）を更新します。
     *
     * @param mail 新しいメールアドレス
     * @param nowMail 現在（変更前）のメールアドレス
     * @param name 新しい名前
     */
    public void updateNoPassword(String mail, String nowMail, String name) {
        repository.updateNoPassword(mail, nowMail, name);
    } 


    /**
     * パスワード変更を含めてユーザ情報（メールアドレス・名前・パスワード）を更新します。
     *
     * @param mail 新しいメールアドレス
     * @param nowMail 現在（変更前）のメールアドレス
     * @param name 新しい名前
     * @param password 新しいパスワード（ハッシュ化されて保存されます）
     */
    public void updateYesPassword(String mail, String nowMail, String name, String password) {

        password = passwordEncoder.encode(password);
        repository.updateYesPassword(mail, nowMail, name, password);
    }


    /**
     * 顧客アカウントを新規登録します。パスワードはハッシュ化され、名前とメールアドレスには同一の値が初期設定されます。
     *
     * @param mail メールアドレス
     * @param password パスワード
     */
    public void registerCustomer(String mail, String password) {
        password = passwordEncoder.encode(password);
        repository.register(mail, mail, password, "顧客", "一般");
    }


    /**
     * ユーザのプロフィール情報（性別・生年月日）を更新します。
     *
     * @param mail メールアドレス
     * @param gender 性別
     * @param birthday 生年月日（"YYYY-MM-DD" フォーマットの文字列）
     */
    public void updateProfile(String mail, String gender, String birthday) {
        repository.updateProfile(mail, gender, birthday);
    }


    /**
     * ローカルLLM (Ollama) を呼び出し、ユーザのプロフィールや利用可能なメニュー情報に基づいたAI接客提案文を生成します。
     *
     * @param user 提案対象のユーザ情報
     * @param userPrompt ユーザからの入力・リクエストテキスト
     * @return AIによって生成された回答メッセージ（エラー時はエラーメッセージ文字列）
     */
    public String getAi(UserEntity user, String userPrompt) {

        LocalDate birthday = user.getBirthday().toLocalDate();
        int age = Period.between(birthday, LocalDate.now()).getYears();
        String gender = user.getGender();
        List<Map<String, Object>> goodsList = storeRepository.getAll();

        StringBuilder menuText = new StringBuilder();
        for (Map<String, Object> goods : goodsList) {
            // 売り切れの商品は除外
            Boolean soldOut =  (Boolean) goods.get("sold_out");
            if (soldOut) {
                continue;
            }
            // 裏メニューは除外
            String rank = (String) goods.get("watch_rank");
            if (!"一般".equals(rank)) {
                continue;
            }
            String name = (String) goods.get("goods_name");
            Integer price = (Integer) goods.get("price");
            Integer calorie = (Integer) goods.get("calorie");
            String allergy = (String) goods.get("allergy");
            String detail = (String) goods.get("detail");

            // リストをテキストにする
            menuText.append(String.format("- %s (価格:%d円、アレルギー:%s、カロリー:%dkcal、説明:%s) \n",
                name, price, allergy, calorie, detail));
        }
        // プロンプト
        String systemPrompt = String.format("""
                あなたは「ORDER ZANGI」の優秀な接客AIアシスタントです。
                下記の【ユーザー情報】【提供可能なメニュー一覧】を基にメニューを提案してください。

                【ユーザー情報】
                ・年齢: %d歳
                ・性別: %s

                【提供可能なメニュー一覧】
                %s

                【ユーザーからの質問・希望】
                「%s」

                【厳格なルール】
                1. 提案する商品は、必ず上記の【提供可能なメニュー一覧】に記載されている「商品名」から選んでください。
                2. 一覧に存在しないメニューは絶対に提案・捏造しないでください。
                3. 候補がない場合は「該当するメニューはございません」と伝えて、リスト内の商品をおすすめしてください。
                4. 100～150文字程度で分かりやすく回答してください。
                """, age, gender, menuText.toString(), userPrompt);

        //Ollamaへの送信リクエスト作成 
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gemma2");
        requestBody.put("prompt", systemPrompt);
        requestBody.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();

        // API呼び出し レスポンス取得
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(aiApiUrl, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("response")) {
                return (String) responseBody.get("response");
            }

            return "上手く提案を作成できませんでした。";
        } catch (Exception e) {
            e.printStackTrace();
            return "エラーが発生しました。再度お試しください。";
        }
    }

    public String getCloseDay() {

        // 今日の日付
        LocalDate today = LocalDate.now();
        // 休業日リスト
        List<Map<String, Object>> list = repository.getCloseDay();

        String result = null;

        for(Map<String, Object> map : list) {
            Object value = map.get("close_day");

            if (value != null) {
                LocalDate closeDate = null;

                // DBからの型に応じて LocalDate に変換
                if (value instanceof java.sql.Date sqlDate) {
                    closeDate = sqlDate.toLocalDate();
                } else if (value instanceof LocalDate localDate) {
                    closeDate = localDate;
                } else if (value instanceof String strDate) {
                    closeDate = LocalDate.parse(strDate);
                }

                // 今日と一致した場合
                if (today.equals(closeDate)) {
                    result = (String) map.get("close_type");
                    break; // 見つかったらループを抜ける
                }
            }
        }

        return  result;
    }
}
