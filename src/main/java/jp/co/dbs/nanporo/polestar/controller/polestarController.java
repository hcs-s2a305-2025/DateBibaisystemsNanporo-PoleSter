package jp.co.dbs.nanporo.polestar.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jp.co.dbs.nanporo.polestar.data.OrderData;
import jp.co.dbs.nanporo.polestar.response.ActiveOrderResponse;
import jp.co.dbs.nanporo.polestar.service.OrderService;

@Controller
public class polestarController {

    @Autowired 
    private OrderService orderService;

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        if (principal != null) {
            String mail = principal.getName();
            // 予約中の注文リストを取得してModelに登録
            List<ActiveOrderResponse> activeOrders = orderService.getActiveOrders(mail);
            model.addAttribute("activeOrders", activeOrders);
        }

        return "home";
    }

    @GetMapping("/home")
    public String homeRedirect() {
        return "redirect:/"; // 「/」の表示処理へ転送
    }
}