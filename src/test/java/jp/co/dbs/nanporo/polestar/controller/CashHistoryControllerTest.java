package jp.co.dbs.nanporo.polestar.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import jp.co.dbs.nanporo.polestar.service.PosService;

@ExtendWith(MockitoExtension.class)
class CashHistoryControllerTest {

    @Mock
    private PosService posService;

    @InjectMocks
    private CashHistoryController controller;

    @Test
    @DisplayName("日付未指定時は本日の会計履歴を画面に設定する")
    void getCasherHistoryWithoutDate() {
        List<String> history = List.of("取引履歴");
        when(posService.getCasherHistory(any(LocalDate.class))).thenReturn(Map.of(
                "historyList", history,
                "totalSales", 1200,
                "totalCustomers", 2,
                "selectedDate", "2026-10-07"));
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.getCasherHistory(null, model)).isEqualTo("w/casherhistory");

        ArgumentCaptor<LocalDate> date = ArgumentCaptor.forClass(LocalDate.class);
        verify(posService).getCasherHistory(date.capture());
        assertThat(date.getValue()).isEqualTo(LocalDate.now());
        assertThat(model.asMap()).containsEntry("historyList", history)
                .containsEntry("totalSales", 1200)
                .containsEntry("totalCustomers", 2)
                .containsEntry("selectedDate", "2026-10-07");
    }

    @Test
    @DisplayName("指定日の会計履歴を取得する")
    void getCasherHistoryForSelectedDate() {
        LocalDate date = LocalDate.of(2026, 10, 1);
        List<String> history = List.of("指定日の取引");
        when(posService.getCasherHistory(date)).thenReturn(Map.of(
                "historyList", history, "totalSales", 900, "totalCustomers", 1, "selectedDate", date.toString()));
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.getCasherHistory(date, model)).isEqualTo("w/casherhistory");

        verify(posService).getCasherHistory(date);
        assertThat(model.asMap()).containsEntry("historyList", history)
                .containsEntry("totalSales", 900)
                .containsEntry("totalCustomers", 1)
                .containsEntry("selectedDate", date.toString());
    }

    @Test
    @DisplayName("会計金額を更新して選択日に戻る")
    void updateCasherHistory() {
        assertThat(controller.updateCasherHistory(3, 1000, 1500, 500, "2026-10-01"))
                .isEqualTo("redirect:/w/casherhistory?date=2026-10-01");

        verify(posService).updateTransactionMoney(3, 1000, 1500, 500);
    }
}
