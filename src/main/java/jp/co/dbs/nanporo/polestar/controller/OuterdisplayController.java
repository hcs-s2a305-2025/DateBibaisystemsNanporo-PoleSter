package jp.co.dbs.nanporo.polestar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jp.co.dbs.nanporo.polestar.response.OuterdisplayResponse;
import jp.co.dbs.nanporo.polestar.service.OuterdisplayService;

@Controller
@RequestMapping("/outerdisplay")
public class OuterdisplayController {

    @Autowired
    private OuterdisplayService outerdisplayService;

    @GetMapping
    public String showDisplay(Model model) {
        // Serviceから調理中・呼び出し中に分かれたデータを取得
        OuterdisplayResponse displayOrders = outerdisplayService.getDisplayOrders();
        
        // Modelに登録してHTMLへ渡す
        model.addAttribute("displayOrders", displayOrders);
        
        return "outerdisplay"; // templates/outerdisplay.html を表示
    }
}