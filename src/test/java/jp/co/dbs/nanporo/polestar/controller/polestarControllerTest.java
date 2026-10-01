package jp.co.dbs.nanporo.polestar.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jp.co.dbs.nanporo.polestar.service.NotificationService;
import jp.co.dbs.nanporo.polestar.service.OrderService;
import jp.co.dbs.nanporo.polestar.service.UserService;

@ExtendWith(MockitoExtension.class)
class PolestarControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private UserService userService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PolestarController controller;

    @Test
    @DisplayName("未ログイン時にホーム画面を返しサービスを呼び出さない")
    void testHomeWithoutPrincipal() {
        Model model = new ExtendedModelMap();

        String view = controller.home(model, null);

        assertThat(view).isEqualTo("home");
        assertThat(model.asMap()).isEmpty();
        verifyNoInteractions(orderService, userService);
    }

    @Test
    @DisplayName("ログイン時にホーム画面へ注文と通知を登録する")
    void testHomeWithPrincipal() {
        String mail = "customer@example.com";
        Principal principal = () -> mail;
        List<Map<String, Object>> notifications = List.of(Map.of("content", "お知らせ"));
        when(orderService.getActiveOrders(mail)).thenReturn(List.of());
        when(userService.getNotificationsByMail(mail)).thenReturn(notifications);
        Model model = new ExtendedModelMap();

        String view = controller.home(model, principal);

        assertThat(view).isEqualTo("home");
        assertThat(model.asMap()).containsEntry("activeOrders", List.of());
        assertThat(model.asMap().get("notificationList")).isSameAs(notifications);
        verify(orderService).getActiveOrders(mail);
        verify(userService).getNotificationsByMail(mail);
    }

    @Test
    @DisplayName("未ログイン時に通知フラグメントを返しサービスを呼び出さない")
    void testNotificationFragmentWithoutPrincipal() {
        Model model = new ExtendedModelMap();

        String view = controller.getNotificationFragment(model, null);

        assertThat(view).isEqualTo("home :: #notification-area");
        assertThat(model.asMap()).isEmpty();
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("ログイン時に通知一覧を登録して通知フラグメントを返す")
    void testNotificationFragmentWithPrincipal() {
        String mail = "customer@example.com";
        Principal principal = () -> mail;
        List<Map<String, Object>> notifications = List.of(Map.of("content", "お知らせ"));
        when(userService.getNotificationsByMail(mail)).thenReturn(notifications);
        Model model = new ExtendedModelMap();

        String view = controller.getNotificationFragment(model, principal);

        assertThat(view).isEqualTo("home :: #notification-area");
        assertThat(model.asMap().get("notificationList")).isSameAs(notifications);
        verify(userService).getNotificationsByMail(mail);
    }

    @Test
    @DisplayName("ホーム画面へのリダイレクト先を返す")
    void testHomeRedirect() {
        assertThat(controller.homeRedirect()).isEqualTo("redirect:/");
    }

    @Test
    @DisplayName("店員用ホーム画面を返す")
    void testWorkerHome() {
        assertThat(controller.workerHome()).isEqualTo("w/home");
    }
}