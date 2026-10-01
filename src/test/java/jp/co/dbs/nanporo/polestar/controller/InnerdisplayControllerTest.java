package jp.co.dbs.nanporo.polestar.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import jp.co.dbs.nanporo.polestar.service.InnerdisplayService;

@ExtendWith(MockitoExtension.class)
class InnerdisplayControllerTest {

    @Mock
    private InnerdisplayService innerdisplayService;

    @InjectMocks
    private InnerdisplayController controller;

    @Test
    @DisplayName("厨房注文一覧をModelに登録する")
    void testShowDisplay() {
        List<Map<String, Object>> orders = List.of(Map.of("orderId", 1));
        when(innerdisplayService.getKitchenOrdersGrouped()).thenReturn(orders);
        var model = new ExtendedModelMap();

        assertThat(controller.showDisplay(model)).isEqualTo("w/innerdisplay");
        assertThat(model.asMap()).containsEntry("orders", orders);
    }

    @Test
    @DisplayName("調理完了をサービスへ通知して画面に戻る")
    void testCompleteCook() {
        assertThat(controller.completeCook(12)).isEqualTo("redirect:/w/innerdisplay");
        verify(innerdisplayService).completeCook(12);
    }
}