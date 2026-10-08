package jp.co.dbs.nanporo.polestar.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jp.co.dbs.nanporo.polestar.response.ActiveOrderResponse;
import jp.co.dbs.nanporo.polestar.service.OrderService;
import jp.co.dbs.nanporo.polestar.service.UserService;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class PolestarController {

    @Autowired 
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        if (principal != null) {
            String mail = principal.getName();
            // 予約中の注文リストを取得してModelに登録
            List<ActiveOrderResponse> activeOrders = orderService.getActiveOrders(mail);
            model.addAttribute("activeOrders", activeOrders);
            // 通知一覧を取得してModelに登録
            List<Map<String, Object>> notificationList =
                    userService.getNotificationsByMail(mail);
            model.addAttribute("notificationList", notificationList);
        }

        return "home";
    }

    /**
     * 【追加】通知部分（フラグメント）のみを更新して返す非同期処理エンドポイント
     */
    @GetMapping("/notifications/fragment")
    public String getNotificationFragment(Model model, Principal principal) {
        if (principal != null) {
            String mail = principal.getName();
            // 通知一覧を取得してModelに登録
            List<Map<String, Object>> notificationList =
                    userService.getNotificationsByMail(mail);
            model.addAttribute("notificationList", notificationList);
        }
        // 「HTMLテンプレート名 :: #更新したい要素のID名」を返す
        return "home :: #notification-area";
    }

    @GetMapping("/home")
    public String homeRedirect() {
        return "redirect:/"; // 「/」の表示処理へ転送
    }

    /**
     * 店員・店長用ホーム画面 (templates/w/home.html)
     */
    @GetMapping("/w/home")
    public String workerHome() {
        return "w/home"; // templates/w/home.html を表示
    }
    
}