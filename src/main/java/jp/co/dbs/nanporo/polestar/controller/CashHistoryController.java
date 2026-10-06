package jp.co.dbs.nanporo.polestar.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jp.co.dbs.nanporo.polestar.service.PosService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CashHistoryController {

    private final PosService posService;

    @GetMapping("/w/casherhistory")
    public String getCasherHistory(
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {

        if (date == null) {
            date = LocalDate.now();
        }

        Map<String, Object> historyData = posService.getCasherHistory(date);

        model.addAttribute("historyList", historyData.get("historyList"));
        model.addAttribute("totalSales", historyData.get("totalSales"));
        model.addAttribute("totalCustomers", historyData.get("totalCustomers")); // 総客数を追加
        model.addAttribute("selectedDate", historyData.get("selectedDate"));

        return "w/casherhistory";
    }
    
    /**
     * 会計金額の編集処理
     */
    @PostMapping("/w/casherhistory/update")
    public String updateCasherHistory(
            @RequestParam("transactionId") Integer transactionId,
            @RequestParam("sumMoney") Integer sumMoney,
            @RequestParam("receivedMoney") Integer receivedMoney,
            @RequestParam("changeMoney") Integer changeMoney,
            @RequestParam("selectedDate") String selectedDate) {

        posService.updateTransactionMoney(transactionId, sumMoney, receivedMoney, changeMoney);

        return "redirect:/w/casherhistory?date=" + selectedDate;
    }
}
