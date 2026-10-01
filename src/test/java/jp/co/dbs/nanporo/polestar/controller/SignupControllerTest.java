package jp.co.dbs.nanporo.polestar.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.co.dbs.nanporo.polestar.service.UserService;

@ExtendWith(MockitoExtension.class)
class SignupControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private SignupController controller;

    @Test
    @DisplayName("新規登録画面を表示する")
    void testGetSignup() {
        assertThat(controller.getSignup()).isEqualTo("signup");
    }

    @Test
    @DisplayName("メールアドレス未入力時にエラーを設定する")
    void testPostSignupWithoutMail() {
        var attributes = new RedirectAttributesModelMap();

        assertThat(controller.postSignup(" ", "password", "password", attributes)).isEqualTo("redirect:/signup");
        assertThat(attributes.getAttribute("mailNullError")).isEqualTo("true");
    }

    @Test
    @DisplayName("パスワード未入力時にエラーを設定する")
    void testPostSignupWithoutPassword() {
        var attributes = new RedirectAttributesModelMap();

        assertThat(controller.postSignup("a@example.com", " ", "", attributes)).isEqualTo("redirect:/signup");
        assertThat(attributes.getAttribute("passwordNullError")).isEqualTo("true");
    }

    @Test
    @DisplayName("確認用パスワードが異なる場合にエラーを設定する")
    void testPostSignupPasswordMismatch() {
        var attributes = new RedirectAttributesModelMap();

        assertThat(controller.postSignup("a@example.com", "password", "different", attributes)).isEqualTo("redirect:/signup");
        assertThat(attributes.getAttribute("passwordError")).isEqualTo("true");
    }

    @Test
    @DisplayName("顧客登録に成功した場合に成功属性を設定する")
    void testPostSignupSuccess() {
        var attributes = new RedirectAttributesModelMap();

        assertThat(controller.postSignup("a@example.com", "password", "password", attributes)).isEqualTo("redirect:/signup");
        assertThat(attributes.getAttribute("success")).isEqualTo("true");
        assertThat(attributes.getAttribute("mail")).isEqualTo("a@example.com");
        verify(userService).registerCustomer("a@example.com", "password");
    }

    @Test
    @DisplayName("登録済みメールアドレスの場合に重複エラーを設定する")
    void testPostSignupDuplicate() {
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(userService).registerCustomer("a@example.com", "password");
        var attributes = new RedirectAttributesModelMap();

        controller.postSignup("a@example.com", "password", "password", attributes);

        assertThat(attributes.getAttribute("sameMailError")).isEqualTo("true");
    }

    @Test
    @DisplayName("顧客登録で予期しない例外が発生した場合にエラーを設定する")
    void testPostSignupFailure() {
        doThrow(new IllegalStateException("failure"))
                .when(userService).registerCustomer("a@example.com", "password");
        var attributes = new RedirectAttributesModelMap();

        controller.postSignup("a@example.com", "password", "password", attributes);

        assertThat(attributes.getAttribute("error")).isEqualTo("true");
    }

    @Test
    @DisplayName("追加プロフィールを更新してログイン画面へ遷移する")
    void testUpdateProfile() {
        var attributes = new RedirectAttributesModelMap();

        assertThat(controller.updateProfile("a@example.com", "女", "2000-01-01", attributes))
                .isEqualTo("redirect:/login");
        verify(userService).updateProfile("a@example.com", "女", "2000-01-01");
    }
}