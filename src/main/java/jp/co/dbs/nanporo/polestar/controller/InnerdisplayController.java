package jp.co.dbs.nanporo.polestar.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jp.co.dbs.nanporo.polestar.service.InnerdisplayService;

@Controller
public class InnerdisplayController {

    @Autowired
    private InnerdisplayService innerdisplayService;

    @GetMapping("/w/innerdisplay")
    public String showDisplay(Model model) {
        List<Map<String, Object>> orders = innerdisplayService.getKitchenOrdersGrouped();
        model.addAttribute("orders", orders);
        return "w/innerdisplay";
    }

    @PostMapping("/w/innerdisplay/complete")
    public String completeCook(@RequestParam("orderId") Integer orderId) {
        innerdisplayService.completeCook(orderId);
        return "redirect:/w/innerdisplay";
    }
}