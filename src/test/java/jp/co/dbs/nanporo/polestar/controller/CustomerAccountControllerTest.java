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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.response.UserGetResponse;
import jp.co.dbs.nanporo.polestar.service.UserService;

@ExtendWith(MockitoExtension.class)
class CustomerAccountControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private CustomerAccountController controller;

    @Test
    @DisplayName("顧客一覧をページ情報と共に表示する")
    void testGetCustomerAccount() {
        var pageable = PageRequest.of(2, 10);
        Principal principal = () -> "manager@example.com";
        UserGetResponse response = new UserGetResponse();
        when(userService.getCustomerList(pageable, "desc")).thenReturn(response);
        Model model = new ExtendedModelMap();

        String view = controller.getStaffAccount(pageable, "desc", principal, model);

        assertThat(view).isEqualTo("w/account/customer");
        assertThat(model.asMap()).containsEntry("response", response)
                .containsEntry("currentPage", 2).containsEntry("sort", "desc");
    }

    @Test
    @DisplayName("顧客詳細をメールアドレスで取得する")
    void testGetCustomerDetail() {
        UserEntity user = new UserEntity();
        when(userService.findByMail("customer@example.com")).thenReturn(user);

        assertThat(controller.getCustomerDetail("customer@example.com")).isSameAs(user);
    }

    @Test
    @DisplayName("顧客削除APIが削除処理を実行する")
    void testDeleteCustomer() {
        var response = controller.deleteCustomer("customer@example.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("OK");
        verify(userService).deleteUser("customer@example.com");
    }

    @Test
    @DisplayName("顧客停止APIが停止処理を実行する")
    void testStopCustomer() {
        var response = controller.stopCustomer("customer@example.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("OK");
        verify(userService).stopUser("customer@example.com");
    }

    @Test
    @DisplayName("顧客停止解除APIが再開処理を実行する")
    void testResumeCustomer() {
        var response = controller.resumeCustomer("customer@example.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("OK");
        verify(userService).resumeUser("customer@example.com");
    }
}