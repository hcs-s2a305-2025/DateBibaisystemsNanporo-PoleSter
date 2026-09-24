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
import jp.co.dbs.nanporo.polestar.request.OrderDetailRequest;
import jp.co.dbs.nanporo.polestar.request.OrderRegisterRequest;
import jp.co.dbs.nanporo.polestar.response.OrderHistoryResponse;
import jp.co.dbs.nanporo.polestar.response.OrderRegisterResponse;
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
    public String showAddPage(
        @RequestParam("goodsId") String goodsId,
        @RequestParam(value = "editCartItemId", required = false) String editCartItemId,
        HttpSession session,
        Model model) {
        // DBから該当商品の詳細情報を取得
        GoodsData goods = storeService.getGoodsDetail(goodsId);

        // 初期値（新規追加時）
        String selectedRice = "20";
        String selectedSource = "0";

        // 変更処理（編集時）の場合は、カート内から以前の選択値を復元
        if (editCartItemId != null && !editCartItemId.trim().isEmpty()) {
            @SuppressWarnings("unchecked")
            List<CartData> cart = (List<CartData>) session.getAttribute("cart");
            if (cart != null) {
                CartData target = cart.stream()
                        .filter(c -> editCartItemId.equals(c.getCartItemId()))
                        .findFirst()
                        .orElse(null);
                
                if (target != null) {
                    if (target.getRiceCode() != null) selectedRice = target.getRiceCode();
                    if (target.getSourceCode() != null) selectedSource = target.getSourceCode();
                }
            }
        }

        // 画面に渡す
        model.addAttribute("goods", goods);
        model.addAttribute("editCartItemId", editCartItemId);
        model.addAttribute("selectedRice", selectedRice);
        model.addAttribute("selectedSource", selectedSource);
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

        CartData item = new CartData();

        // 変更（再入れ直し）の場合は古いカート要素を削除
        if (editCartItemId != null && !editCartItemId.trim().isEmpty()) {
            cart.removeIf(c -> editCartItemId.equals(c.getCartItemId()));
            item.setCartItemId(editCartItemId);
        } else {
            item.setCartItemId(UUID.randomUUID().toString());
        }

        // 各加算料金の計算
        int zPrice = getZangiPrice(zangiCount);
        int rPrice = getRicePrice(riceAmount);
        int sPrice = getSourcePrice(sourceType);

        item.setGoodsId(goods.getGoodsId());
        item.setGoodsName(goods.getGoodsName());
        item.setPrice(goods.getPrice());
        item.setPhoto(goods.getPhoto());
        item.setOrderDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日")));
        
        item.setZangiCount(zangiCount);
        item.setZangiPrice(zPrice);
        
        // コード値と表示名称の両方を保存
        item.setRiceCode(riceAmount);
        item.setRiceAmount(getRiceName(riceAmount));
        item.setRicePrice(rPrice);

        item.setSourceCode(sourceType);
        item.setSourceType(getSourceName(sourceType));
        item.setSourcePrice(sPrice);
        
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

        return "redirect:/menu/add?goodsId=" + target.getGoodsId() + "&editCartItemId=" + target.getCartItemId();
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
            HttpSession session,
            Principal principal) {

        @SuppressWarnings("unchecked")
        List<CartData> cart = (List<CartData>) session.getAttribute("cart");
        if (cart == null || cart.isEmpty()) {
            return "redirect:/cart";
        }

        // ログインユーザーのmail取得
        String userMail = principal.getName();

        // 3. 受け取り日時と登録日時のフォーマット整形 (OrderService.java の LocalDateTime.parse に対応)
        // pickupTime が "12:00" の場合は ":00" を補填して "12:00:00" にします
        String formattedPickupTime = pickupTime.length() == 5 ? pickupTime + ":00" : pickupTime;
        String getTimeStr = pickupDate + "T" + formattedPickupTime; // 例: "2026-09-07T12:00:00"
        
        String registerTimeStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // 4. カートアイテムを OrderDetailRequest のリストへ変換
        List<OrderDetailRequest> detailList = new ArrayList<>();
        for (CartData item : cart) {
            OrderDetailRequest detail = new OrderDetailRequest();
            detail.setGoodsId(item.getGoodsId());
            detail.setSetGoodsId(null); // セット商品IDがある場合は設定
            detail.setCount(1); // 1明細あたりの個数
            detail.setPlusZangiCount(item.getZangiCount());
            
            // ソースコード等をカスタムIDとして設定
            detail.setCustomId(Integer.parseInt(item.getSourceCode())); 
            
            detailList.add(detail);
        }

        // 5. 注文登録リクエストオブジェクトの生成
        OrderRegisterRequest request = new OrderRegisterRequest();
        request.setGetTime(getTimeStr);
        request.setMail(userMail);
        request.setRegisterTime(registerTimeStr);
        request.setSumMoney(cart.stream().mapToInt(CartData::getTotalPrice).sum());
        request.setMemo(memo);
        request.setStatus("受付");
        request.setOrderType("RESERVATION"); // モバイル予約注文
        request.setOrderDetails(detailList);

        // 6. サービス層を実行してDBへ登録
        OrderRegisterResponse response = orderService.insertOrder(request);
        
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
            case "10" -> "小盛り (150g)";
            case "30" -> "大盛り (350g)";
            case "40" -> "特盛 (450g)";
            default -> "普通 (250g)";
        };
    }

    private int getRicePrice(String key) {
        return switch (key) {
            case "10" -> -30;
            case "30" -> 50;
            case "40" -> 100;
            default -> 0;
        };
    }

    private String getSourceName(String key) {
        return switch (key) {
            case "50" -> "おろしポン酢ソース";
            case "51" -> "おろしポン酢ソースだく";
            case "52" -> "おろしポン酢ソースだくだく";
            case "60" -> "自家製タルタルソース";
            case "61" -> "自家製タルタルソースだく";
            case "62" -> "自家製タルタルソースだくだく";
            case "70" -> "油淋鶏風ネギダレ";
            case "71" -> "油淋鶏風ネギダレだく";
            case "72" -> "油淋鶏風ネギダレだくだく";
            case "80" -> "皆辣麻婆ソース";
            case "81" -> "皆辣麻婆ソースだく";
            case "82" -> "皆辣麻婆ソースだくだく";
            default -> "なし";
        };
    }

    private int getSourcePrice(String key) {
        return switch (key) {
            case "50", "60", "70" -> 80;
            case "51", "61", "71" -> 120;
            case "52", "62", "72" -> 150;
            case "80" -> 100;
            case "81" -> 140;
            case "82" -> 180;
            default -> 0;
        };
    }

}
