package jp.co.dbs.nanporo.polestar.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jp.co.dbs.nanporo.polestar.data.OrderData;
import jp.co.dbs.nanporo.polestar.request.OrderRegisterRequest;
import jp.co.dbs.nanporo.polestar.response.OrderHistoryResponse;
import jp.co.dbs.nanporo.polestar.service.OrderService;

@Controller 
public class OrderController {
    
    @Autowired 
    private OrderService orderService;

    /**
     * ホーム画面を表示します。
     */
    @GetMapping("/home/order")
    public String index(Model model, Principal principal) {
        if (principal != null) {
            String mail = principal.getName();
            // 予約中の注文リストを取得してModelに登録
            List<OrderData> activeOrders = orderService.getActiveOrders(mail);
            model.addAttribute("activeOrders", activeOrders);
        }

        return "home";
    }

    /**
     * 予約履歴画面を表示します。
     */
    @GetMapping("/history")
    public String showHistory(Model model, Principal principal) {
        if (principal != null) {
            String mail = principal.getName();
            // 予約履歴を取得してModelへセット
            List<OrderHistoryResponse> historyList = orderService.getOrderHistory(mail);
            model.addAttribute("historyList", historyList);
        }

        return "history";
    }
    

    /**
     * 注文データを登録します（予約注文 または 店頭注文）
     * 
     * @param request 画面から送信された注文登録データ
     * @param action 押されたボタンのvalue（"reserve" または "store" など）
     * @param principal ログインユーザー情報
     */
    @PostMapping("/order/post")
    public String postOrder(OrderRegisterRequest request, @RequestParam("action") String action, Principal principal) {

        // 1. ログイン中のユーザーID（メールアドレス）を取得してセット
        if (principal != null) {
            String loginMail = principal.getName();
            request.setMail(loginMail);
        }

        // 2. 登録日時が画面から届かない場合は現在日時を自動設定
        if (request.getRegisterTime() == null || request.getRegisterTime().isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            request.setRegisterTime(LocalDateTime.now().format(formatter));
        }

        // 3. 押されたボタン（action）によって注文区分（RESERVATION / STORE）と初期ステータスを自動設定
        if ("reserve".equals(action)) {
            request.setOrderType("RESERVATION"); // モバイル予約用（番号: M0001〜）
            request.setStatus("受付");
        } else {
            request.setOrderType("STORE");       // 店頭注文用（番号: 0001〜）
            request.setStatus("調理中");
        }

        // 4. Serviceを呼び出してデータベースへ登録
        orderService.insertOrder(request);

        // 5. 登録完了後は注文完了画面（またはトップ画面）へリダイレクト
        return "redirect:/order/complete";
    }

}
