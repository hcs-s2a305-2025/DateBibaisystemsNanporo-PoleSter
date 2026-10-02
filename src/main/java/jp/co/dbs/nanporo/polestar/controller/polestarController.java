package jp.co.dbs.nanporo.polestar.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jp.co.dbs.nanporo.polestar.response.ActiveOrderResponse;
import jp.co.dbs.nanporo.polestar.service.NotificationService;
import jp.co.dbs.nanporo.polestar.service.OrderService;
import jp.co.dbs.nanporo.polestar.service.UserService;

@Controller
public class PolestarController {

    @Autowired 
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @Autowired 
    private NotificationService notificationService;

    @GetMapping("/")
    public String home(Model model, Principal principal, Authentication authentication) {
        // 1. ログインユーザーの権限（ロール）チェック
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isWorker = authentication.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .anyMatch(role -> 
                    // DBの登録名そのもの、または Spring Security が ROLE_ を付与した形式のどちらかに一致するか検証
                    "店員".equals(role) || "店長".equals(role) ||
                    "ROLE_店員".equals(role) || "ROLE_店長".equals(role)
                );
            if (isWorker) {
                // 店員・店長の場合は従業員用ホーム画面へリダイレクト
                return "redirect:/w/home";
            }
        }

        // 2.顧客（または未ログイン）の場合の処理
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