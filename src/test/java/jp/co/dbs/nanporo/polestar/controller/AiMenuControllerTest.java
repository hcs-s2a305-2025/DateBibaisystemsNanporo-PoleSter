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
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.service.StoreService;
import jp.co.dbs.nanporo.polestar.service.UserService;

@ExtendWith(MockitoExtension.class)
class AiMenuControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private StoreService storeService;

    @InjectMocks
    private AiMenuController controller;

    @Test
    @DisplayName("AI提案を取得してフラッシュ属性に設定する")
    void testPostAiMenu() {
        String mail = "customer@example.com";
        String prompt = "おすすめを教えて";
        UserEntity user = new UserEntity();
        Principal principal = () -> mail;
        RedirectAttributesModelMap attributes = new RedirectAttributesModelMap();
        when(userService.findByMail(mail)).thenReturn(user);
        when(userService.getAi(user, prompt)).thenReturn("提案内容");

        String view = controller.postAiMenu(prompt, principal, attributes);

        assertThat(view).isEqualTo("redirect:/menu");
        assertThat(attributes.getFlashAttributes().get("aiAnswer")).isEqualTo("提案内容");
        assertThat(attributes.getFlashAttributes().get("userPrompt")).isEqualTo(prompt);
        verify(userService).findByMail(mail);
        verify(userService).getAi(user, prompt);
    }
}