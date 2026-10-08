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
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.service.UserService;

@ExtendWith(MockitoExtension.class)
class StampcardControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private StampcardController controller;

    @Test
    @DisplayName("一般会員の次ランクまでの達成枚数を表示する")
    void testGetStampcardForGeneral() {
        assertProgress("一般", 0, 1);
    }

    @Test
    @DisplayName("ブロンズ会員の次ランクまでの達成枚数を表示する")
    void testGetStampcardForBronze() {
        assertProgress("ブロンズ", 1, 2);
    }

    @Test
    @DisplayName("シルバー会員の次ランクまでの達成枚数を表示する")
    void testGetStampcardForSilver() {
        assertProgress("シルバー", 2, 3);
    }

    @Test
    @DisplayName("その他の会員ランクは進捗0を表示する")
    void testGetStampcardForOtherRank() {
        assertProgress("ゴールド", 5, 0);
    }

    @Test
    @DisplayName("割引券を使用できた場合はスタンプカード画面へ戻る")
    void testUseCoupon() {
        when(userService.activateStampCoupon("customer@example.com")).thenReturn(true);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        assertThat(controller.useCoupon(() -> "customer@example.com", redirect)).isEqualTo("redirect:/stampcard");
        assertThat(redirect.getFlashAttributes().containsKey("couponError")).isFalse();
    }

    @Test
    @DisplayName("使用できる割引券がない場合はエラーメッセージを渡す")
    void testUseCouponWithoutCoupon() {
        when(userService.activateStampCoupon("customer@example.com")).thenReturn(false);
        RedirectAttributes redirect = new RedirectAttributesModelMap();

        assertThat(controller.useCoupon(() -> "customer@example.com", redirect)).isEqualTo("redirect:/stampcard");
        assertThat(redirect.getFlashAttributes().get("couponError")).isEqualTo("使用できる割引カードがありません。");
    }

    @Test
    @DisplayName("割引券の使用を取り消してスタンプカード画面へ戻る")
    void testCancelCoupon() {
        assertThat(controller.cancelCoupon(() -> "customer@example.com")).isEqualTo("redirect:/stampcard");
        verify(userService).cancelStampCoupon("customer@example.com");
    }

    private void assertProgress(String rank, int completedCards, int expectedProgress) {
        String mail = "customer@example.com";
        UserEntity user = new UserEntity();
        user.setMemberRank(rank);
        user.setPointCardComplete(completedCards);
        when(userService.findByMail(mail)).thenReturn(user);
        Principal principal = () -> mail;
        Model model = new ExtendedModelMap();

        assertThat(controller.getStampcard(principal, model)).isEqualTo("stampcard");
        assertThat(model.asMap()).containsEntry("nextRankProgress", expectedProgress)
                .containsEntry("user", user);
    }
}