package jp.co.dbs.nanporo.polestar.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import jp.co.dbs.nanporo.polestar.response.OuterdisplayResponse;
import jp.co.dbs.nanporo.polestar.service.OuterdisplayService;

@ExtendWith(MockitoExtension.class)
class OuterdisplayControllerTest {

    @Mock
    private OuterdisplayService outerdisplayService;

    @InjectMocks
    private OuterdisplayController controller;

    @Test
    @DisplayName("外部表示の注文情報をModelに登録する")
    void testShowDisplay() {
        OuterdisplayResponse response = new OuterdisplayResponse();
        when(outerdisplayService.getDisplayOrders()).thenReturn(response);
        var model = new ExtendedModelMap();

        assertThat(controller.showDisplay(model)).isEqualTo("w/outerdisplay");
        assertThat(model.asMap()).containsEntry("displayOrders", response);
    }
}