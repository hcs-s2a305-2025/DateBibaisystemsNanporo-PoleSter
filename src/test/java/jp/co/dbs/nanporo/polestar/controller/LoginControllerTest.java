package jp.co.dbs.nanporo.polestar.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class LoginControllerTest {

    private final LoginController controller = new LoginController();

    @Test
    @DisplayName("ログイン画面を返す")
    void testGetLogin() {
        assertThat(controller.getLogin()).isEqualTo("login");
    }

    @Test
    @DisplayName("店長権限はスタッフ画面へ遷移する")
    void testLoginSuccessForManager() {
        assertThat(controller.loginSuccess(authentication("店長"))).isEqualTo("redirect:/w/home");
    }

    @Test
    @DisplayName("店員権限はスタッフ画面へ遷移する")
    void testLoginSuccessForStaff() {
        assertThat(controller.loginSuccess(authentication("ROLE_店員"))).isEqualTo("redirect:/w/home");
    }

    @Test
    @DisplayName("顧客または認証なしは一般ホームへ遷移する")
    void testLoginSuccessForCustomerOrAnonymous() {
        assertThat(controller.loginSuccess(authentication("ROLE_顧客"))).isEqualTo("redirect:/");
        assertThat(controller.loginSuccess(null)).isEqualTo("redirect:/");
    }

    private Authentication authentication(String authority) {
        Authentication authentication = mock(Authentication.class);
        doReturn(List.of(new SimpleGrantedAuthority(authority)))
                .when(authentication).getAuthorities();
        return authentication;
    }
}