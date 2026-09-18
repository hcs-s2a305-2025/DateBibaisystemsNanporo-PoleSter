package jp.co.dbs.nanporo.polestar.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import jp.co.dbs.nanporo.polestar.data.CartData;
import jp.co.dbs.nanporo.polestar.data.GoodsData;
import jp.co.dbs.nanporo.polestar.data.OrderData;
import jp.co.dbs.nanporo.polestar.request.OrderRegisterRequest;
import jp.co.dbs.nanporo.polestar.response.OrderHistoryResponse;
import jp.co.dbs.nanporo.polestar.service.OrderService;
import jp.co.dbs.nanporo.polestar.service.StoreService;

@Controller 
public class OrderController {
    
    @Autowired 
    private OrderService orderService;

    @Autowired 
    private StoreService storeService;
    

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
     * 商品追加画面を表示します。
     */
    @GetMapping("/menu/add")
    public String showAddPage(@RequestParam("goodsId") String goodsId, Model model) {
        // DBから該当商品の詳細情報を取得
        GoodsData goods = storeService.getGoodsDetail(goodsId);
        // 画面に渡す
        model.addAttribute("goods", goods);
        return "menu/add"; // templates/menu/add.html を呼び出す
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

    // カート画面表示
    @GetMapping("/cart")
    public String showCart(HttpSession session, Model model) {
        @SuppressWarnings("unchecked")
        List<CartData> cart = (List<CartData>) session.getAttribute("cart");
        if (cart == null) {
            cart = new ArrayList<>();
        }

        // 合計金額（小計）の計算
        int grandTotal = cart.stream().mapToInt(CartData::getTotalPrice).sum();

        model.addAttribute("reservedList", cart);
        model.addAttribute("grandTotal", grandTotal);
        return "menu/cart"; // カート画面のHTMLパス
    }


    // 商品追加処理（add.htmlのフォームから送信）
    @PostMapping("/cart/add")
    public String addToCart(
            @RequestParam("goodsId") String goodsId,
            @RequestParam(value = "zangiCount", defaultValue = "0") Integer zangiCount,
            @RequestParam(value = "riceAmount", defaultValue = "standard") String riceAmount,
            @RequestParam(value = "sourceType", defaultValue = "none") String sourceType,
            @RequestParam(value = "editCartItemId", required = false) String editCartItemId,
            HttpSession session) {

        GoodsData goods = storeService.getGoodsDetail(goodsId);
        if (goods == null) {
            return "redirect:/menu";
        }

        @SuppressWarnings("unchecked")
        List<CartData> cart = (List<CartData>) session.getAttribute("cart");
        if (cart == null) {
            cart = new ArrayList<>();
        }
        // 変更（再入れ直し）の場合は古いカート要素を削除
        if (editCartItemId != null && !editCartItemId.isEmpty()) {
            cart.removeIf(item -> item.getCartItemId().equals(editCartItemId));
        }

        // 各加算料金の計算
        int zPrice = getZangiPrice(zangiCount);
        int rPrice = getRicePrice(riceAmount);
        int sPrice = getSourcePrice(sourceType);

        CartData item = new CartData();
        item.setCartItemId(UUID.randomUUID().toString());
        item.setGoodsId(goods.getGoodsId());
        item.setGoodsName(goods.getGoodsName());
        item.setPrice(goods.getPrice());
        item.setPhoto(goods.getPhoto());
        
        // 今日の日付をセット
        item.setOrderDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日")));
        
        // ザンギ個数と加算料金
        item.setZangiCount(zangiCount);
        item.setZangiPrice(zPrice);
        
        // ご飯・ソース情報
        item.setRiceAmount(getRiceName(riceAmount));
        item.setRicePrice(rPrice);
        item.setSourceType(getSourceName(sourceType));
        item.setSourcePrice(sPrice);
        
        // 合計金額の計算 (基本料金 + ザンギ加算 + ご飯加算 + ソース加算)
        item.setTotalPrice(goods.getPrice() + zPrice + rPrice + sPrice);

        cart.add(item);
        session.setAttribute("cart", cart);

        return "redirect:/cart";
    }

    // 商品の変更（注文追加画面 add.html へ戻って再選択）
    @PostMapping("/cart/edit")
    public String editCartItem(@RequestParam("cartItemId") String cartItemId, HttpSession session) {
        @SuppressWarnings("unchecked")
        List<CartData> cart = (List<CartData>) session.getAttribute("cart");
        if (cart == null) return "redirect:/cart";

        CartData target = cart.stream()
                .filter(item -> item.getCartItemId().equals(cartItemId))
                .findFirst().orElse(null);

        if (target == null) return "redirect:/cart";

        return "redirect:/store/add?goodsId=" + target.getGoodsId() + "&editCartItemId=" + target.getCartItemId();
    }

    // カートから特定の要素を削除
    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam("cartItemId") String cartItemId, HttpSession session) {
        @SuppressWarnings("unchecked")
        List<CartData> cart = (List<CartData>) session.getAttribute("cart");
        if (cart != null) {
            cart.removeIf(item -> item.getCartItemId().equals(cartItemId));
            session.setAttribute("cart", cart);
        }
        return "redirect:/cart";
    }

    // カートを空にする
    @PostMapping("/cart/clear")
    public String clearCart(HttpSession session) {
        session.removeAttribute("cart");
        return "redirect:/cart";
    }

    // 注文確定処理（DB保存）
    @PostMapping("/cart/checkout")
    public String checkout(
            @RequestParam("pickupDate") String pickupDate,
            @RequestParam("pickupTime") String pickupTime,
            @RequestParam(value = "memo", required = false) String memo,
            HttpSession session) {

        @SuppressWarnings("unchecked")
        List<CartData> cart = (List<CartData>) session.getAttribute("cart");
        if (cart == null || cart.isEmpty()) {
            return "redirect:/cart";
        }

        // TODO: storeService.createOrder(cart, pickupDate, pickupTime, memo); 等でDB登録
        
        // 注文完了後、セッションのカートを削除
        session.removeAttribute("cart");

        return "redirect:/home"; // 注文完了画面へ
    }

    // --- 加算料金・名称変換ユーティリティ ---
    
    // ザンギの加算料金計算（例: 標準5個、1個追加ごとに+100円）
    private int getZangiPrice(int count) {
        int baseCount = 5; // 基本個数
        if (count > baseCount) {
            return (count - baseCount) * 100; // 5個を超える分1個につき100円加算
        }
        return 0;
    }

    private String getRiceName(String key) {
        return switch (key) {
            case "high" -> "大盛り";
            case "max" -> "特盛";
            case "min" -> "小盛";
            default -> "普通";
        };
    }

    private int getRicePrice(String key) {
        return switch (key) {
            case "high" -> 50;
            case "max" -> 100;
            default -> 0;
        };
    }

    private String getSourceName(String key) {
        return switch (key) {
            case "ponzu" -> "おろしポン酢ソース";
            case "tartar" -> "自家製タルタルソース";
            case "negi" -> "油淋鶏風ネギダレ";
            default -> "なし";
        };
    }

    private int getSourcePrice(String key) {
        return switch (key) {
            case "ponzu", "negi" -> 80;
            case "tartar" -> 100;
            default -> 0;
        };
    }

}
