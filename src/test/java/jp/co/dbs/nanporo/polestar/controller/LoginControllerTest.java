package jp.co.dbs.nanporo.polestar.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.AuthenticationManager;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.co.dbs.nanporo.polestar.service.UserService;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private UserService service;

    @Mock
    private RedirectAttributes redirectAttributes;

    private LoginController controller;

    @BeforeEach
    void setUp() {
        controller = new LoginController(mock(AuthenticationManager.class));
        controller.service = service;
    }

    @Test
    @DisplayName("ログイン画面を返し、画面表示時に休業日を通知する")
    void testGetLogin() {
        ExtendedModelMap model = new ExtendedModelMap();
        when(service.getCloseDay()).thenReturn("定休日");

        assertThat(controller.getLogin(model)).isEqualTo("login");
        assertThat(model.get("closedMessage")).isEqualTo("本日は「定休日」のため、店長のみログイン可能です。");
    }

    @Test
    @DisplayName("営業日はログイン画面に休業日メッセージを表示しない")
    void testGetLoginOnOpenDay() {
        ExtendedModelMap model = new ExtendedModelMap();
        when(service.getCloseDay()).thenReturn(null);

        assertThat(controller.getLogin(model)).isEqualTo("login");
        assertThat(model.containsAttribute("closedMessage")).isFalse();
    }

    @Test
    @DisplayName("ログイン画面に既存の休業日メッセージがあれば再取得しない")
    void testGetLoginWithExistingMessage() {
        ExtendedModelMap model = new ExtendedModelMap();
        model.addAttribute("closedMessage", "通知済み");

        assertThat(controller.getLogin(model)).isEqualTo("login");
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("営業日：店長権限はスタッフ画面へ遷移する")
    void testLoginSuccessForManagerOnOpenDay() {
        // 店長は休業日チェックがスキップされるか、または営業日の場合
        assertThat(controller.loginSuccess(authentication("店長"), null, redirectAttributes))
                .isEqualTo("redirect:/w/home");
    }

    @Test
    @DisplayName("休業日：店長権限は休業日であってもログインできスタッフ画面へ遷移する")
    void testLoginSuccessForManagerOnClosedDay() {
        assertThat(controller.loginSuccess(authentication("店長"), request(), redirectAttributes))
                .isEqualTo("redirect:/w/home");

        // redirectAttributes にメッセージが設定されていないことを検証
        verifyNoInteractions(redirectAttributes);
    }

    @Test
    @DisplayName("営業日：店員権限はスタッフ画面へ遷移する")
    void testLoginSuccessForStaffOnOpenDay() {
        when(service.getCloseDay()).thenReturn(null);

        assertThat(controller.loginSuccess(authentication("ROLE_店員"), request(), redirectAttributes))
                .isEqualTo("redirect:/w/home");
    }

    @Test
    @DisplayName("休業日：店員権限はログイン画面へリダイレクトされメッセージが設定される")
    void testLoginSuccessForStaffOnClosedDay() {
        when(service.getCloseDay()).thenReturn("定休日");

        HttpServletRequest request = requestWithSession();
        String result = controller.loginSuccess(authentication("ROLE_店員"), request, redirectAttributes);

        assertThat(result).isEqualTo("redirect:/login");
        verify(request.getSession(false)).invalidate();
        verify(redirectAttributes).addFlashAttribute(
            "closedMessage", 
            "本日は「定休日」のため、店長以外のログインを制限しております。"
        );
    }

    @Test
    @DisplayName("営業日：顧客または認証なしは一般ホームへ遷移する")
    void testLoginSuccessForCustomerOrAnonymousOnOpenDay() {
        when(service.getCloseDay()).thenReturn(null);

        assertThat(controller.loginSuccess(authentication("ROLE_顧客"), request(), redirectAttributes))
                .isEqualTo("redirect:/");
        assertThat(controller.loginSuccess(null, request(), redirectAttributes))
                .isEqualTo("redirect:/");
    }

    @Test
    @DisplayName("休業日：顧客または認証なしはログイン画面へリダイレクトされメッセージが設定される")
    void testLoginSuccessForCustomerOrAnonymousOnClosedDay() {
        when(service.getCloseDay()).thenReturn("臨時休業");

        String resultCustomer = controller.loginSuccess(authentication("ROLE_顧客"), request(), redirectAttributes);

        assertThat(resultCustomer).isEqualTo("redirect:/login");
        verify(redirectAttributes).addFlashAttribute(
            "closedMessage", 
            "本日は「臨時休業」のため、店長以外のログインを制限しております。"
        );
    }

    @Test
    @DisplayName("休業日でセッションがない場合も顧客をログイン画面へ戻す")
    void testLoginSuccessForAnonymousOnClosedDayWithoutSession() {
        when(service.getCloseDay()).thenReturn("臨時休業");

        assertThat(controller.loginSuccess(null, request(), redirectAttributes)).isEqualTo("redirect:/login");
        verify(redirectAttributes).addFlashAttribute(
                "closedMessage", "本日は「臨時休業」のため、店長以外のログインを制限しております。");
    }

    private Authentication authentication(String... authorities) {
        Authentication authentication = mock(Authentication.class);
        doReturn(Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList())
                .when(authentication).getAuthorities();
        return authentication;
    }

    private HttpServletRequest request() {
        return mock(HttpServletRequest.class);
    }

    private HttpServletRequest requestWithSession() {
        HttpServletRequest request = request();
        when(request.getSession(false)).thenReturn(mock(HttpSession.class));
        return request;
    }
}