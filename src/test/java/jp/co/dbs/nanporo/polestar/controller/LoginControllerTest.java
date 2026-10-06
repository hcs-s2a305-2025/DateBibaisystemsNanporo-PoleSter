package jp.co.dbs.nanporo.polestar.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jp.co.dbs.nanporo.polestar.service.UserService;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private UserService service;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private LoginController controller;

    @Test
    @DisplayName("ログイン画面を返す")
    void testGetLogin() {
        assertThat(controller.getLogin()).isEqualTo("login");
    }

    @Test
    @DisplayName("営業日：店長権限はスタッフ画面へ遷移する")
    void testLoginSuccessForManagerOnOpenDay() {
        // 店長は休業日チェックがスキップされるか、または営業日の場合
        assertThat(controller.loginSuccess(authentication("店長"), redirectAttributes))
                .isEqualTo("redirect:/w/home");
    }

    @Test
    @DisplayName("休業日：店長権限は休業日であってもログインできスタッフ画面へ遷移する")
    void testLoginSuccessForManagerOnClosedDay() {
        // 休業日（「臨時休業」）が設定されていても、店長はスルーされる
        when(service.getCloseDay()).thenReturn("臨時休業");

        assertThat(controller.loginSuccess(authentication("店長"), redirectAttributes))
                .isEqualTo("redirect:/w/home");

        // redirectAttributes にメッセージが設定されていないことを検証
        verifyNoInteractions(redirectAttributes);
    }

    @Test
    @DisplayName("営業日：店員権限はスタッフ画面へ遷移する")
    void testLoginSuccessForStaffOnOpenDay() {
        when(service.getCloseDay()).thenReturn(null);

        assertThat(controller.loginSuccess(authentication("ROLE_店員"), redirectAttributes))
                .isEqualTo("redirect:/w/home");
    }

    @Test
    @DisplayName("休業日：店員権限はログイン画面へリダイレクトされメッセージが設定される")
    void testLoginSuccessForStaffOnClosedDay() {
        when(service.getCloseDay()).thenReturn("定休日");

        String result = controller.loginSuccess(authentication("ROLE_店員"), redirectAttributes);

        assertThat(result).isEqualTo("redirect:/login");
        verify(redirectAttributes).addFlashAttribute(
            "closedMessage", 
            "本日は「定休日」のため、システムを休止しております。"
        );
    }

    @Test
    @DisplayName("営業日：顧客または認証なしは一般ホームへ遷移する")
    void testLoginSuccessForCustomerOrAnonymousOnOpenDay() {
        when(service.getCloseDay()).thenReturn(null);

        assertThat(controller.loginSuccess(authentication("ROLE_顧客"), redirectAttributes))
                .isEqualTo("redirect:/");
        assertThat(controller.loginSuccess(null, redirectAttributes))
                .isEqualTo("redirect:/");
    }

    @Test
    @DisplayName("休業日：顧客または認証なしはログイン画面へリダイレクトされメッセージが設定される")
    void testLoginSuccessForCustomerOrAnonymousOnClosedDay() {
        when(service.getCloseDay()).thenReturn("臨時休業");

        String resultCustomer = controller.loginSuccess(authentication("ROLE_顧客"), redirectAttributes);

        assertThat(resultCustomer).isEqualTo("redirect:/login");
        verify(redirectAttributes).addFlashAttribute(
            "closedMessage", 
            "本日は「臨時休業」のため、システムを休止しております。"
        );
    }

    private Authentication authentication(String authority) {
        Authentication authentication = mock(Authentication.class);
        doReturn(List.of(new SimpleGrantedAuthority(authority)))
                .when(authentication).getAuthorities();
        return authentication;
    }
}