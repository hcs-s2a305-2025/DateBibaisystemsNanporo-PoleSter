package jp.co.dbs.nanporo.polestar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import jp.co.dbs.nanporo.polestar.data.UserData;
import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.repository.StoreRepository;
import jp.co.dbs.nanporo.polestar.repository.UserRepository;
import jp.co.dbs.nanporo.polestar.repository.UserRepository.SalesFlashDto;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class UserServiceUnitTest {

    @Mock
    private UserRepository repository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService service;

    @Test
    @DisplayName("ログイン用ユーザーを取得し、該当なしなら例外にする")
    void getUser() {
        UserData data = new UserData();
        data.setMail("user@example.com");
        when(repository.findByMail(data)).thenReturn(List.of(Map.of(
                "mail", "user@example.com", "password", "hashed", "role", "顧客", "name", "利用者", "alive", false)));

        assertThat(service.getUser(data).getName()).isEqualTo("利用者");

        when(repository.findByMail(data)).thenReturn(List.of());
        assertThatThrownBy(() -> service.getUser(data))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("ユーザが見つかりません： user@example.com");
    }

    @Test
    @DisplayName("従業員と顧客の一覧をエンティティへ変換しページ数を返す")
    void getStaffAndCustomerLists() {
        Map<String, Object> row = userRow("staff@example.com");
        when(repository.getStaffList(any(), eq("asc"))).thenReturn(List.of(row));
        when(repository.countStaffList()).thenReturn(11);
        assertThat(service.getStaffList(PageRequest.of(0, 10), "asc").getTotalPages()).isEqualTo(2);
        when(repository.getCustomerList(any(), eq("desc"))).thenReturn(List.of(row));
        when(repository.countCustomerList()).thenReturn(0);
        assertThat(service.getCustomerList(PageRequest.of(0, 10), "desc").getTotalPages()).isZero();
    }

    @Test
    @DisplayName("メールアドレスからユーザー詳細を変換する")
    void findByMail() {
        when(repository.findByMail("user@example.com")).thenReturn(userRow("user@example.com"));

        UserEntity user = service.findByMail("user@example.com");

        assertThat(user.getMail()).isEqualTo("user@example.com");
        assertThat(user.getName()).isEqualTo("利用者");
        assertThat(user.getBirthday()).isEqualTo(Date.valueOf("1990-01-04"));
        assertThat(user.getIcon()).isEqualTo("sibainu1.png");
    }

    @Test
    @DisplayName("通知一覧の取得とユーザー更新系をリポジトリへ委譲する")
    void delegatesUserOperations() {
        List<Map<String, Object>> notices = List.of(Map.of("content", "お知らせ"));
        when(repository.findNotificationsByMail("user@example.com")).thenReturn(notices);
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(passwordEncoder.encode("new-password")).thenReturn("new-encoded");

        assertThat(service.getNotificationsByMail("user@example.com")).isSameAs(notices);
        service.updateStaff("user@example.com", "名前", "店員", true);
        service.deleteUser("user@example.com");
        service.registerStaff("staff@example.com", "スタッフ", "店員");
        service.stopUser("user@example.com");
        service.resumeUser("user@example.com");
        service.updateNoPassword("new@example.com", "old@example.com", "新しい名前", "sibainu1.png");
        service.updateYesPassword("new@example.com", "old@example.com", "新しい名前", "new-password", "sibainu1.png");
        service.registerCustomer("customer@example.com", "password");
        service.updateProfile("customer@example.com", "女", "1990-01-04");

        verify(repository).updateStaff("user@example.com", "名前", "店員", true);
        verify(repository).deleteUser("user@example.com");
        verify(repository).register("staff@example.com", "スタッフ", "encoded", "店員", "一般");
        verify(repository).stopUser("user@example.com");
        verify(repository).resumeUser("user@example.com");
        verify(repository).updateNoPassword("new@example.com", "old@example.com", "新しい名前", "sibainu1.png");
        verify(repository).updateYesPassword("new@example.com", "old@example.com", "新しい名前", "new-encoded", "sibainu1.png");
        verify(repository).register("customer@example.com", "customer@example.com", "encoded", "顧客", "一般");
        verify(repository).updateProfile("customer@example.com", "女", "1990-01-04");
    }

    @Test
    @DisplayName("当日注文数、臨時休業、売上フラッシュを委譲する")
    void delegatesDailyOperations() {
        when(repository.countOrder(any(LocalDate.class))).thenReturn(3);
        SalesFlashDto flash = new SalesFlashDto("10:00 〜 11:00", 1200, 2);
        when(repository.getHourlySalesFlash(any(), any(), any())).thenReturn(flash);

        assertThat(service.countOrder()).isEqualTo(3);
        service.insertClose();
        assertThat(service.getHourlySalesFlash()).isSameAs(flash);

        verify(repository).insertClose(LocalDate.now(), "臨時休業");
        verify(repository).cancelUpdate(LocalDate.now());
        ArgumentCaptor<LocalDateTime> start = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> end = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).getHourlySalesFlash(any(), start.capture(), end.capture());
        assertThat(start.getValue().getMinute()).isZero();
        assertThat(start.getValue().getSecond()).isZero();
        assertThat(end.getValue().getMinute()).isEqualTo(59);
        assertThat(end.getValue().getNano()).isEqualTo(999999999);
    }

    @Test
    @DisplayName("顧客がいない場合は一斉通知を登録しない")
    void sendBroadcastNoticeWithoutCustomers() {
        when(repository.findCustomerEmails()).thenReturn(List.of());

        service.sendBroadcastNotice("お知らせ");

        verify(repository, never()).getMaxNoticeId();
        // verify(repository, never()).insertNoticeWithId(any(Integer.class), any(), any(), any());
    }

    @Test
    @DisplayName("一斉通知のIDを連番で採番して全顧客に登録する")
    void sendBroadcastNotice() {
        when(repository.findCustomerEmails()).thenReturn(List.of("first@example.com", "second@example.com"));
        when(repository.getMaxNoticeId()).thenReturn(30);

        service.sendBroadcastNotice("お知らせ");

        // verify(repository).insertNoticeWithId(eq(31), eq("first@example.com"), any(), eq("お知らせ"));
        // verify(repository).insertNoticeWithId(eq(32), eq("second@example.com"), any(), eq("お知らせ"));
    }

    @ParameterizedTest
    @CsvSource({"月, MONDAY", "火, TUESDAY", "水, WEDNESDAY", "木, THURSDAY", "金, FRIDAY", "土, SATURDAY", "日, SUNDAY"})
    @DisplayName("日本語の曜日ごとに指定週数の定休日を登録する")
    void closeDays(String dayName, DayOfWeek dayOfWeek) {
        LocalDate expectedDate = LocalDate.now().with(TemporalAdjusters.nextOrSame(dayOfWeek));

        service.closeDays(dayName, 2);

        verify(repository).insertClose(expectedDate, "定休日");
        verify(repository).insertClose(expectedDate.plusWeeks(1), "定休日");
    }

    @Test
    @DisplayName("不正な曜日または週数ゼロでは休業日を登録しない")
    void closeDaysWithoutDates() {
        service.closeDays("invalid", 2);
        service.closeDays("月", 0);

        verify(repository, never()).insertClose(any(), eq("定休日"));
    }

    @ParameterizedTest
    @CsvSource({"true, true", "false, false"})
    @DisplayName("入力パスワードの一致・不一致を返す")
    void passwordCheck(boolean matches, boolean expected) {
        when(repository.findByMail("user@example.com")).thenReturn(Map.of("password", "encoded"));
        when(passwordEncoder.matches("plain", "encoded")).thenReturn(matches);

        assertThat(service.passwordCheck("user@example.com", "plain")).isEqualTo(expected);
    }

    @Test
    @DisplayName("利用可能な商品だけを含むプロンプトでAI提案を返す")
    void getAi() {
        when(storeRepository.getAll()).thenReturn(List.of(
                goods("提供商品", false, "一般"),
                goods("売切商品", true, "一般"),
                goods("裏商品", false, "ゴールド")));
        UserEntity user = new UserEntity();
        user.setBirthday(Date.valueOf("1990-01-04"));
        user.setGender("女");

        try (MockedConstruction<RestTemplate> restTemplates = mockConstruction(RestTemplate.class,
                (restTemplate, context) -> when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                        .thenReturn(ResponseEntity.ok(Map.of("response", "おすすめです"))))) {
            assertThat(service.getAi(user, "質問")).isEqualTo("おすすめです");
            HttpEntity request = captureAiRequest(restTemplates);
            String prompt = (String) ((Map<?, ?>) request.getBody()).get("prompt");
            assertThat(prompt).contains("提供商品", "女", "質問");
            assertThat(prompt).doesNotContain("売切商品", "裏商品");
        }
    }

    @Test
    @DisplayName("AI応答に回答キーがない場合は既定メッセージを返す")
    void getAiWithoutResponseKey() {
        try (MockedConstruction<RestTemplate> restTemplates = mockConstruction(RestTemplate.class,
                (restTemplate, context) -> when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                        .thenReturn(ResponseEntity.ok(Map.of("other", "value"))))) {
            assertThat(service.getAi(aiUser(), "質問")).isEqualTo("上手く提案を作成できませんでした。");
        }
    }

    @Test
    @DisplayName("AI応答本文がnullの場合は既定メッセージを返す")
    void getAiWithNullResponseBody() {
        try (MockedConstruction<RestTemplate> restTemplates = mockConstruction(RestTemplate.class,
                (restTemplate, context) -> when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                        .thenReturn(ResponseEntity.<Map>noContent().build()))) {
            assertThat(service.getAi(aiUser(), "質問")).isEqualTo("上手く提案を作成できませんでした。");
        }
    }

    @Test
    @DisplayName("AI呼び出し例外時はエラーメッセージを返す")
    void getAiOnError() {
        try (MockedConstruction<RestTemplate> restTemplates = mockConstruction(RestTemplate.class,
                (restTemplate, context) -> when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                        .thenThrow(new IllegalStateException("failed")))) {
            assertThat(service.getAi(aiUser(), "質問")).isEqualTo("エラーが発生しました。再度お試しください。");
        }
    }

    @Test
    @DisplayName("本日が休業日である場合に休業種別を返す")
    void getCloseDay() {
        when(repository.getCloseDay()).thenReturn(List.of(
                Map.of("close_day", Date.valueOf(LocalDate.now()), "close_type", "臨時休業")
        ));

        assertThat(service.getCloseDay()).isEqualTo("臨時休業");
    }

    private HttpEntity captureAiRequest(MockedConstruction<RestTemplate> restTemplates) {
        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplates.constructed().get(0)).postForEntity(
                eq("http://localhost:11434/api/generate"), requestCaptor.capture(), eq(Map.class));
        return requestCaptor.getValue();
    }

    private UserEntity aiUser() {
        UserEntity user = new UserEntity();
        user.setBirthday(Date.valueOf("1990-01-04"));
        user.setGender("未設定");
        return user;
    }

    private Map<String, Object> userRow(String mail) {
        Map<String, Object> row = new HashMap<>();
        row.put("mail", mail);
        row.put("name", "利用者");
        row.put("password", "hashed");
        row.put("role", "顧客");
        row.put("member_rank", "一般");
        row.put("gender", "女");
        row.put("birthday", Date.valueOf("1990-01-04"));
        row.put("icon", "sibainu1.png");
        row.put("cancel_count", 0);
        row.put("alive", false);
        row.put("point", 10);
        row.put("point_card_complete", 1);
        return row;
    }

    private Map<String, Object> goods(String name, boolean soldOut, String rank) {
        return Map.of("goods_name", name, "sold_out", soldOut, "watch_rank", rank,
                "price", 500, "calorie", 300, "allergy", "なし", "detail", "説明");
    }
}