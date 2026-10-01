package jp.co.dbs.nanporo.polestar.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.service.UserService;

@ExtendWith(MockitoExtension.class)
class SettingControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private SettingController controller;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("プロフィール設定画面にユーザー情報を表示する")
    void testGetSetting() {
        assertUserView("settings", controller::getSetting);
    }

    @Test
    @DisplayName("プロフィール編集画面にユーザー情報を表示する")
    void testGetSettingEdit() {
        assertUserView("settings/edit", controller::getSettingEdit);
    }

    @Test
    @DisplayName("定休日設定を無効にして成功する")
    void testUpdateScheduleDisabled() {
        var attributes = new RedirectAttributesModelMap();

        assertThat(controller.updateSchedule(false, "月", attributes)).isEqualTo("redirect:/settings");
        assertThat(attributes.getAttribute("success")).isEqualTo("true");
    }

    @Test
    @DisplayName("定休日設定を有効にして52週分登録する")
    void testUpdateScheduleEnabled() {
        var attributes = new RedirectAttributesModelMap();

        controller.updateSchedule(true, "月", attributes);

        verify(userService).closeDays("月", 52);
        assertThat(attributes.getAttribute("success")).isEqualTo("true");
    }

    @Test
    @DisplayName("定休日登録に失敗した場合にエラー属性を設定する")
    void testUpdateScheduleFailure() {
        doThrow(new IllegalStateException("failure")).when(userService).closeDays("月", 52);
        var attributes = new RedirectAttributesModelMap();

        controller.updateSchedule(true, "月", attributes);

        assertThat(attributes.getAttribute("error")).isEqualTo("true");
    }

    @Test
    @DisplayName("パスワードを変更せずプロフィールを更新する")
    void testUpdateProfileWithoutPassword() {
        String mail = "customer@example.com";
        var attributes = new RedirectAttributesModelMap();

        String view = controller.updateProfile("新しい名前", mail, "", "", "", attributes, () -> mail);

        assertThat(view).isEqualTo("redirect:/settings");
        verify(userService).updateNoPassword(mail, mail, "新しい名前");
    }

    @Test
    @DisplayName("パスワード変更時に現在のパスワードが違えば更新しない")
    void testUpdateProfileWrongOldPassword() {
        String mail = "customer@example.com";
        when(userService.passwordCheck(mail, "wrong")).thenReturn(false);
        var attributes = new RedirectAttributesModelMap();

        String view = controller.updateProfile("名前", mail, "wrong", "new", "new", attributes, () -> mail);

        assertThat(view).isEqualTo("redirect:/settings/edit");
        assertThat(attributes.getAttribute("oldPasswordError")).isEqualTo("true");
    }

    @Test
    @DisplayName("新しいパスワードが一致しなければ更新しない")
    void testUpdateProfileNewPasswordMismatch() {
        String mail = "customer@example.com";
        when(userService.passwordCheck(mail, "old")).thenReturn(true);
        var attributes = new RedirectAttributesModelMap();

        String view = controller.updateProfile("名前", mail, "old", "new", "different", attributes, () -> mail);

        assertThat(view).isEqualTo("redirect:/settings/edit");
        assertThat(attributes.getAttribute("newPasswordError")).isEqualTo("true");
    }

    @Test
    @DisplayName("パスワード未入力を通知し、残りの更新処理を続行する")
    void testUpdateProfilePartialPassword() {
        String mail = "customer@example.com";
        when(userService.passwordCheck(mail, "")).thenReturn(true);
        var attributes = new RedirectAttributesModelMap();

        controller.updateProfile("名前", mail, "", "new", "new", attributes, () -> mail);

        assertThat(attributes.getAttribute("passwordNullError")).isEqualTo("true");
        verify(userService).updateYesPassword(mail, mail, "名前", "new");
    }

    @Test
    @DisplayName("新しいパスワード未入力をエラー属性に設定する")
    void testUpdateProfileWithoutNewPassword() {
        String mail = "customer@example.com";
        when(userService.passwordCheck(mail, "old")).thenReturn(true);
        var attributes = new RedirectAttributesModelMap();

        controller.updateProfile("名前", mail, "old", "", "", attributes, () -> mail);

        assertThat(attributes.getAttribute("passwordNullError")).isEqualTo("true");
        verify(userService).updateYesPassword(mail, mail, "名前", "");
    }

    @Test
    @DisplayName("確認用パスワード未入力をエラー属性に設定する")
    void testUpdateProfileWithoutPasswordConfirmation() {
        String mail = "customer@example.com";
        when(userService.passwordCheck(mail, "old")).thenReturn(true);
        var attributes = new RedirectAttributesModelMap();

        controller.updateProfile("名前", mail, "old", "new", "", attributes, () -> mail);

        assertThat(attributes.getAttribute("passwordNullError")).isEqualTo("true");
        assertThat(attributes.getAttribute("newPasswordError")).isEqualTo("true");
    }

    @Test
    @DisplayName("新旧パスワード欄の複数未入力をそれぞれ検出する")
    void testUpdateProfileWithMultipleMissingPasswords() {
        String mail = "customer@example.com";
        when(userService.passwordCheck(mail, "")).thenReturn(true);
        var attributes = new RedirectAttributesModelMap();

        controller.updateProfile("名前", mail, "", "", "confirmation", attributes, () -> mail);

        assertThat(attributes.getAttribute("passwordNullError")).isEqualTo("true");
        assertThat(attributes.getAttribute("newPasswordError")).isEqualTo("true");
    }

    @Test
    @DisplayName("パスワード変更とメール変更後に認証情報を更新する")
    void testUpdateProfileChangesMailAndPassword() {
        String oldMail = "old@example.com";
        String newMail = "new@example.com";
        when(userService.passwordCheck(oldMail, "old")).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(oldMail, "credentials",
                        List.of(new SimpleGrantedAuthority("ROLE_顧客"))));
        var attributes = new RedirectAttributesModelMap();

        String view = controller.updateProfile("名前", newMail, "old", "new", "new", attributes, () -> oldMail);

        assertThat(view).isEqualTo("redirect:/settings");
        verify(userService).updateYesPassword(newMail, oldMail, "名前", "new");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(newMail);
    }

    @Test
    @DisplayName("プロフィール更新に失敗した場合に編集画面へ戻す")
    void testUpdateProfileFailure() {
        String mail = "customer@example.com";
        doThrow(new IllegalStateException("failure"))
                .when(userService).updateNoPassword(mail, mail, "名前");
        var attributes = new RedirectAttributesModelMap();

        String view = controller.updateProfile("名前", mail, "", "", "", attributes, () -> mail);

        assertThat(view).isEqualTo("redirect:/settings/edit");
        assertThat(attributes.getAttribute("error")).isEqualTo("true");
    }

    private void assertUserView(String expectedView, SettingViewAction action) {
        String mail = "customer@example.com";
        UserEntity user = new UserEntity();
        when(userService.findByMail(mail)).thenReturn(user);
        var model = new ExtendedModelMap();

        assertThat(action.call(() -> mail, model)).isEqualTo(expectedView);
        assertThat(model.asMap()).containsEntry("user", user);
    }

    @FunctionalInterface
    private interface SettingViewAction {
        String call(Principal principal, org.springframework.ui.Model model);
    }
}