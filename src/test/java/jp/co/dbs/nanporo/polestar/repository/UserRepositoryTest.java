package jp.co.dbs.nanporo.polestar.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import jp.co.dbs.nanporo.polestar.data.UserData;

@ExtendWith(MockitoExtension.class)
class UserRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    @Mock
    private JdbcTemplate plainJdbc;

    @InjectMocks
    private UserRepository repository;

    @Test
    @DisplayName("UserDataのメールアドレスでユーザー一覧を検索する")
    void testFindByMailWithUserData() {
        UserData user = new UserData();
        user.setMail("user@example.com");
        List<Map<String, Object>> expected = List.of(Map.of("mail", "user@example.com"));
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(expected);

        assertThat(repository.findByMail(user)).isSameAs(expected);
        assertThat(captureQueryForListParams()).containsEntry("mail", "user@example.com");
    }

    @Test
    @DisplayName("従業員一覧を降順でページング取得する")
    void testGetStaffListDescending() {
        var pageable = PageRequest.of(2, 5);
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(List.of());

        repository.getStaffList(pageable, "DESC");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForList(sql.capture(), anyMap());
        assertThat(sql.getValue()).contains("ORDER BY mail DESC");
        assertThat(captureQueryForListParams()).containsEntry("limit", 5).containsEntry("offset", 10L);
    }

    @Test
    @DisplayName("従業員一覧の既定ソートを昇順にする")
    void testGetStaffListAscending() {
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(List.of());

        repository.getStaffList(PageRequest.of(0, 10), "asc");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForList(sql.capture(), anyMap());
        assertThat(sql.getValue()).contains("ORDER BY mail ASC");
    }

    @Test
    @DisplayName("従業員件数を取得する")
    void testCountStaffList() {
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Integer.class))).thenReturn(4);

        assertThat(repository.countStaffList()).isEqualTo(4);
    }

    @Test
    @DisplayName("顧客一覧を降順でページング取得する")
    void testGetCustomerListDescending() {
        var pageable = PageRequest.of(1, 20);
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(List.of());

        repository.getCustomerList(pageable, "desc");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForList(sql.capture(), anyMap());
        assertThat(sql.getValue()).contains("role = '顧客'").contains("ORDER BY mail DESC");
        assertThat(captureQueryForListParams()).containsEntry("limit", 20).containsEntry("offset", 20L);
    }

    @Test
    @DisplayName("顧客一覧の既定ソートを昇順にする")
    void testGetCustomerListAscending() {
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(List.of());

        repository.getCustomerList(PageRequest.of(0, 10), "asc");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForList(sql.capture(), anyMap());
        assertThat(sql.getValue()).contains("ORDER BY mail ASC");
    }

    @Test
    @DisplayName("顧客件数を取得する")
    void testCountCustomerList() {
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Integer.class))).thenReturn(8);

        assertThat(repository.countCustomerList()).isEqualTo(8);
    }

    @Test
    @DisplayName("メールアドレスからユーザー情報を1件取得する")
    void testFindByMail() {
        Map<String, Object> expected = Map.of("mail", "user@example.com");
        when(jdbc.queryForMap(anyString(), anyMap())).thenReturn(expected);

        assertThat(repository.findByMail("user@example.com")).isSameAs(expected);
        assertThat(captureQueryForMapParams()).containsEntry("mail", "user@example.com");
    }

    @Test
    @DisplayName("従業員情報を更新する")
    void testUpdateStaff() {
        repository.updateStaff("staff@example.com", "名前", "店員", true);

        assertThat(captureUpdateParams()).containsEntry("mail", "staff@example.com")
                .containsEntry("name", "名前").containsEntry("role", "店員").containsEntry("alive", true);
    }

    @Test
    @DisplayName("ユーザーを削除する")
    void testDeleteUser() {
        repository.deleteUser("user@example.com");

        assertThat(captureUpdateParams()).containsEntry("mail", "user@example.com");
    }

    @Test
    @DisplayName("ユーザーを初期値付きで登録する")
    void testRegister() {
        repository.register("user@example.com", "名前", "encoded", "顧客", "一般");

        Map<String, Object> params = captureUpdateParams();
        assertThat(params).containsEntry("mail", "user@example.com").containsEntry("name", "名前")
                .containsEntry("password", "encoded").containsEntry("role", "顧客")
                .containsEntry("member_rank", "一般").containsEntry("gender", "未")
                .containsEntry("birthday", Date.valueOf("1000-01-01")).containsEntry("cancel_count", 0);
    }

    @Test
    @DisplayName("アカウント利用停止・再開を更新する")
    void testStopAndResumeUser() {
        repository.stopUser("user@example.com");
        repository.resumeUser("user@example.com");

        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(jdbc, times(2)).update(anyString(), params.capture());
        assertThat(params.getAllValues()).allSatisfy(values ->
            assertThat(values).containsEntry("mail", "user@example.com"));
    }

    @Test
    @DisplayName("指定日の有効注文数を取得する")
    void testCountOrder() {
        LocalDate day = LocalDate.of(2026, 10, 1);
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Integer.class))).thenReturn(7);

        assertThat(repository.countOrder(day)).isEqualTo(7);
        assertThat(captureQueryForObjectParams()).containsEntry("getTime", day);
    }

    @Test
    @DisplayName("休業日情報を登録する")
    void testInsertClose() {
        LocalDate day = LocalDate.of(2026, 10, 1);

        repository.insertClose(day, "臨時休業");

        assertThat(captureUpdateParams()).containsEntry("today", day).containsEntry("type", "臨時休業");
    }

    @Test
    @DisplayName("指定日の注文を休業に伴いキャンセルする")
    void testCancelUpdate() {
        LocalDate day = LocalDate.of(2026, 10, 1);

        repository.cancelUpdate(day);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(sql.capture(), anyMap());
        assertThat(sql.getValue()).contains("SET status = 'キャンセル'");
        assertThat(captureUpdateParams()).containsEntry("startTime", day.atStartOfDay())
                .containsEntry("endTime", day.plusDays(1).atStartOfDay());
    }

    @Test
    @DisplayName("ポイント・カード完了数・会員ランクを更新する")
    void testUpdateMemberPointAndRank() {
        repository.updateMemberPointAndRank("user@example.com", 5, 2, "シルバー");

        assertThat(captureUpdateParams()).containsEntry("mail", "user@example.com")
                .containsEntry("point", 5)
                .containsEntry("pointCardComplete", 2)
                .containsEntry("memberRank", "シルバー");
    }

    @Test
    @DisplayName("休業日一覧を取得する")
    void testGetCloseDay() {
        List<Map<String, Object>> expected = List.of(Map.of("close_type", "定休日"));
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(expected);

        assertThat(repository.getCloseDay()).isSameAs(expected);
        assertThat(captureQueryForListParams()).isEmpty();
    }

    @Test
    @DisplayName("売上集計をRowMapperで生成する")
    void testGetHourlySalesFlash() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 10, 1, 10, 0);
        LocalDateTime end = start.plusHours(1);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            RowMapper<UserRepository.SalesFlashDto> mapper = invocation.getArgument(2);
            ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
            when(resultSet.getInt("total_sales")).thenReturn(2500);
            when(resultSet.getInt("customer_count")).thenReturn(3);
            return mapper.mapRow(resultSet, 0);
        }).when(jdbc).queryForObject(anyString(), anyMap(), any(RowMapper.class));

        UserRepository.SalesFlashDto result = repository.getHourlySalesFlash("10:00-11:00", start, end);

        assertThat(result).isEqualTo(new UserRepository.SalesFlashDto("10:00-11:00", 2500, 3));
        assertThat(captureQueryForObjectWithMapperParams()).containsEntry("start", start).containsEntry("end", end);
    }

    @Test
    @DisplayName("顧客メール一覧を取得する")
    void testFindCustomerEmails() {
        when(jdbc.getJdbcTemplate()).thenReturn(plainJdbc);
        when(plainJdbc.queryForList(anyString(), eq(String.class))).thenReturn(List.of("user@example.com"));

        assertThat(repository.findCustomerEmails()).containsExactly("user@example.com");
    }

    @Test
    @DisplayName("通知IDが存在する場合は最大値を返す")
    void testGetMaxNoticeId() {
        when(jdbc.getJdbcTemplate()).thenReturn(plainJdbc);
        when(plainJdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(12);

        assertThat(repository.getMaxNoticeId()).isEqualTo(12);
    }

    @Test
    @DisplayName("通知IDがnullの場合は0を返す")
    void testGetMaxNoticeIdWhenNull() {
        when(jdbc.getJdbcTemplate()).thenReturn(plainJdbc);
        when(plainJdbc.queryForObject(anyString(), eq(Integer.class))).thenReturn(null);

        assertThat(repository.getMaxNoticeId()).isZero();
    }

    @Test
    @DisplayName("通知を登録する")
    void testInsertNoticeWithId() {
        LocalDateTime time = LocalDateTime.of(2026, 10, 1, 12, 0);

        repository.insertNoticeWithId("user@example.com", time, "お知らせ");

        assertThat(captureUpdateParams()).containsEntry("mail", "user@example.com")
                .containsEntry("registerTime", time).containsEntry("content", "お知らせ");
    }

    @Test
    @DisplayName("メールアドレスで通知一覧を検索する")
    void testFindNotificationsByMail() {
        List<Map<String, Object>> expected = List.of(Map.of("content", "お知らせ"));
        when(jdbc.queryForList(anyString(), anyMap())).thenReturn(expected);

        assertThat(repository.findNotificationsByMail("user@example.com")).isSameAs(expected);
        assertThat(captureQueryForListParams()).containsEntry("mail", "user@example.com");
    }

    @Test
    @DisplayName("パスワードを変更せずユーザー情報を更新する")
    void testUpdateNoPassword() {
        repository.updateNoPassword("new@example.com", "old@example.com", "新しい名前", "sibainu1.png");

        assertThat(captureUpdateParams()).containsEntry("mail", "new@example.com")
                .containsEntry("nowMail", "old@example.com").containsEntry("name", "新しい名前")
                .containsEntry("icon", "sibainu1.png");
    }

    @Test
    @DisplayName("パスワードを含めてユーザー情報を更新する")
    void testUpdateYesPassword() {
        repository.updateYesPassword("new@example.com", "old@example.com", "新しい名前", "encoded", "sibainu1.png");

        assertThat(captureUpdateParams()).containsEntry("mail", "new@example.com")
                .containsEntry("nowMail", "old@example.com").containsEntry("name", "新しい名前")
                .containsEntry("password", "encoded").containsEntry("icon", "sibainu1.png");
    }

    @Test
    @DisplayName("プロフィールの性別と誕生日を更新する")
    void testUpdateProfile() {
        repository.updateProfile("user@example.com", "女", "2000-01-02");

        assertThat(captureUpdateParams()).containsEntry("mail", "user@example.com")
                .containsEntry("gender", "女").containsEntry("birthday", Date.valueOf("2000-01-02"));
    }

    private Map<String, Object> captureUpdateParams() {
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(jdbc).update(anyString(), params.capture());
        return params.getValue();
    }

    private Map<String, Object> captureQueryForListParams() {
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(jdbc).queryForList(anyString(), params.capture());
        return params.getValue();
    }

    private Map<String, Object> captureQueryForMapParams() {
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(jdbc).queryForMap(anyString(), params.capture());
        return params.getValue();
    }

    private Map<String, Object> captureQueryForObjectParams() {
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(jdbc).queryForObject(anyString(), params.capture(), eq(Integer.class));
        return params.getValue();
    }

    private Map<String, Object> captureQueryForObjectWithMapperParams() {
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(jdbc).queryForObject(anyString(), params.capture(), any(RowMapper.class));
        return params.getValue();
    }
}