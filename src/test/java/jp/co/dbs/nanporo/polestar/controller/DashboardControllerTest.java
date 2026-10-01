package jp.co.dbs.nanporo.polestar.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;

import jp.co.dbs.nanporo.polestar.service.NotificationService;
import jp.co.dbs.nanporo.polestar.service.UserService;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DashboardController controller;

    @Test
    @DisplayName("ダッシュボードへ当日の注文数と売上フラッシュを設定する")
    void testGetDashboard() {
        when(userService.countOrder()).thenReturn(3);
        Principal principal = () -> "manager@example.com";
        var model = new ExtendedModelMap();

        assertThat(controller.getDashboard(principal, model)).isEqualTo("w/dashboard");
        assertThat(model.asMap()).containsEntry("orderCnt", 3).containsKey("salesFlash");
        verify(userService).getHourlySalesFlash();
    }

    @Test
    @DisplayName("臨時休業登録に成功すると200を返す")
    void testCloseSystem() {
        var response = controller.closeSystem();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("OK");
        verify(userService).insertClose();
    }

    @Test
    @DisplayName("臨時休業の重複登録は409を返す")
    void testCloseSystemDuplicate() {
        doThrow(new DuplicateKeyException("duplicate")).when(userService).insertClose();

        var response = controller.closeSystem();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("Already Closed");
    }

    @Test
    @DisplayName("臨時休業登録のその他の失敗は500を返す")
    void testCloseSystemFailure() {
        doThrow(new IllegalStateException("failure")).when(userService).insertClose();

        var response = controller.closeSystem();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("Error");
    }

    @Test
    @DisplayName("一斉通知に成功すると200を返す")
    void testSendBroadcastNotification() {
        var response = controller.sendBroadcastNotification("お知らせ");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("OK");
        verify(notificationService).sendBroadcastNotification("お知らせ");
    }

    @Test
    @DisplayName("一斉通知の失敗は500を返す")
    void testSendBroadcastNotificationFailure() {
        doThrow(new IllegalArgumentException("invalid"))
                .when(notificationService).sendBroadcastNotification("不正");

        var response = controller.sendBroadcastNotification("不正");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("Error");
    }
}