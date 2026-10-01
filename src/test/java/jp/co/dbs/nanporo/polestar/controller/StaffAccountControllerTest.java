package jp.co.dbs.nanporo.polestar.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.response.UserGetResponse;
import jp.co.dbs.nanporo.polestar.service.UserService;

@ExtendWith(MockitoExtension.class)
class StaffAccountControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private StaffAccountController controller;

    @Test
    @DisplayName("従業員一覧をページ情報と共に表示する")
    void testGetStaffAccount() {
        var pageable = PageRequest.of(1, 10);
        Principal principal = () -> "manager@example.com";
        UserGetResponse response = new UserGetResponse();
        when(userService.getStaffList(pageable, "asc")).thenReturn(response);
        Model model = new ExtendedModelMap();

        String view = controller.getStaffAccount(pageable, "asc", principal, model);

        assertThat(view).isEqualTo("w/account/staff");
        assertThat(model.asMap()).containsEntry("response", response)
                .containsEntry("currentPage", 1).containsEntry("sort", "asc");
    }

    @Test
    @DisplayName("従業員詳細をメールアドレスで取得する")
    void testGetStaffDetail() {
        UserEntity user = new UserEntity();
        when(userService.findByMail("staff@example.com")).thenReturn(user);

        assertThat(controller.getStaffDetail("staff@example.com")).isSameAs(user);
    }

    @Test
    @DisplayName("従業員情報を更新する")
    void testUpdateStaff() {
        var response = controller.updateStaff("staff@example.com", "店員", "店員", true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("OK");
        verify(userService).updateStaff("staff@example.com", "店員", "店員", true);
    }

    @Test
    @DisplayName("従業員を削除する")
    void testDeleteStaff() {
        var response = controller.deleteStaff("staff@example.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("OK");
        verify(userService).deleteUser("staff@example.com");
    }

    @Test
    @DisplayName("従業員登録に成功する")
    void testRegisterStaff() {
        var response = controller.registerStaff("staff@example.com", "店員", "店員");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("OK");
        verify(userService).registerStaff("staff@example.com", "店員", "店員");
    }

    @Test
    @DisplayName("従業員登録の重複は409を返す")
    void testRegisterStaffDuplicate() {
        whenRegisterStaffThrows(new DuplicateKeyException("duplicate"));

        var response = controller.registerStaff("staff@example.com", "店員", "店員");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo("Already Exists");
    }

    @Test
    @DisplayName("従業員登録のその他の失敗は500を返す")
    void testRegisterStaffFailure() {
        whenRegisterStaffThrows(new IllegalStateException("failure"));

        var response = controller.registerStaff("staff@example.com", "店員", "店員");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo("Error");
    }

    private void whenRegisterStaffThrows(RuntimeException exception) {
        org.mockito.Mockito.doThrow(exception).when(userService)
                .registerStaff("staff@example.com", "店員", "店員");
    }
}