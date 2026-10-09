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

        // 販売停止中の商品は追加画面を開かせない
        if (goods != null && Boolean.TRUE.equals(goods.getSoldOut())) {
            return "redirect:/menu";
        }

        // 初期値（新規追加時）
        String selectedRice = "20";
        String selectedSource = "0";
        Integer selectedZangiCount = 0;
        String selectedSet = "0";

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
                    if (target.getZangiCount() != null) selectedZangiCount = target.getZangiCount();
                    if (target.getSetGoodsId() != null) selectedSet = target.getSetGoodsId();
                }
            }
        }

        // 画面に渡す
        model.addAttribute("goods", goods);
        model.addAttribute("editCartItemId", editCartItemId);
        model.addAttribute("selectedRice", selectedRice);
        model.addAttribute("selectedSource", selectedSource);
        model.addAttribute("selectedZangiCount", selectedZangiCount);
        model.addAttribute("selectedSet", selectedSet);
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
            @RequestParam (value = "setGoodsId", defaultValue = "0") String setGoodsName,
            @RequestParam(value = "editCartItemId", required = false) String editCartItemId,
            HttpSession session) {

        GoodsData goods = storeService.getGoodsDetail(goodsId);
        if (goods == null || Boolean.TRUE.equals(goods.getSoldOut())) {
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
        int setPrice = getSetGoodsPrice(setGoodsName);

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

        item.setSetGoodsId(setGoodsName);
        item.setSetGoodsName(getSetName(setGoodsName));
        item.setSetPrice(setPrice);

        item.setTotalPrice(goods.getPrice() + zPrice + rPrice + sPrice + setPrice);

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

        // 受取日時チェック（当日・明日・明後日）（30分以降の予約のみ）
        try {
            // pickupTime のフォーマット補正 ("12:00" -> "12:00:00")
            String formattedPickupTime = pickupTime.length() == 5 ? pickupTime + ":00" : pickupTime;
            
            // 選択された受取日時
            LocalDateTime pickupDateTime = LocalDateTime.parse(pickupDate + "T" + formattedPickupTime);
            LocalDateTime now = LocalDateTime.now();

            // 1. 日付チェック（今日〜明後日）
            LocalDate selectedDate = pickupDateTime.toLocalDate();
            LocalDate today = now.toLocalDate();
            LocalDate maxDate = today.plusDays(2);

            if (selectedDate.isBefore(today) || selectedDate.isAfter(maxDate)) {
                return "redirect:/cart";
            }

            // 2. 時間チェック（閉店時間 15:00 超過チェック）
            if (pickupDateTime.toLocalTime().isAfter(java.time.LocalTime.of(18, 0))) {
                return "redirect:/cart";
            }

            // 3. 直前予約チェック（現在時刻から30分未満の場合は拒否）
            if (pickupDateTime.isBefore(now.plusMinutes(30))) {
                return "redirect:/cart";
            }

        } catch (Exception e) {
            // フォーマット不正などのエラー時はカート画面に戻す
            return "redirect:/cart";
        }

        // ログインユーザーのmail取得
        String userMail = principal.getName();

        // 3. 受け取り日時と登録日時のフォーマット整形 (OrderService.java の LocalDateTime.parse に対応)
        // pickupTime が "12:00" の場合は ":00" を補填して "12:00:00" にします
        String formattedPickupTime = pickupTime.length() == 5 ? pickupTime + ":00" : pickupTime;
        String getTimeStr = pickupDate + "T" + formattedPickupTime; // 例: "2026-09-07T12:00:00"
        
        String registerTimeStr = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // 4. カートアイテムを1商品につき1行の明細へ変換
        List<OrderDetailRequest> detailList = new ArrayList<>();
        int orderCount = 1; // 明細の枝番カウンタ

        for (CartData item : cart) {
            
            // ご飯コードの取得（デフォルト20:普通）
            int riceCode = 20;
            if (item.getRiceCode() != null && !item.getRiceCode().trim().isEmpty()) {
                try {
                    riceCode = Integer.parseInt(item.getRiceCode());
                } catch (NumberFormatException e) {
                    riceCode = 20;
                }
            }

            // ソースコードの取得
            int sourceId = 0;
            if (item.getSourceCode() != null && !"0".equals(item.getSourceCode())) {
                try {
                    sourceId = Integer.parseInt(item.getSourceCode());
                } catch (NumberFormatException e) {
                    sourceId = 0;
                }
            }

            int setGoodsId = 0;
            if (item.getSetGoodsId() != null && !item.getSetGoodsId().trim().isEmpty()) {
                try {
                    setGoodsId = Integer.parseInt(item.getSetGoodsId());
                } catch (NumberFormatException e) {
                    setGoodsId = 0;
                }
            }

            OrderDetailRequest detail = new OrderDetailRequest();
            detail.setOrderCount(orderCount++);
            detail.setGoodsId(item.getGoodsId());
            detail.setSetGoodsId(setGoodsId);
            detail.setCount(1);
            detail.setPlusZangiCount(item.getZangiCount() != null ? item.getZangiCount() : 0);
            detail.setCustomId(riceCode);
            detail.setSourceCustomId(sourceId > 0 ? sourceId : null);
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

    /**
     * 注文を取り消します（キャンセル）
     */
    @PostMapping("/order/cancel")
    public String cancelOrder(@RequestParam("orderId") Integer orderId, Principal principal) {
        if (principal != null) {
            orderService.cancelOrder(orderId);
        }
        return "redirect:/";
    }


    /**
     * 注文を「完成」に変更します。
     * 完成後、注文者への通知処理を実行します。
     */
    @PostMapping("/order/complete")
    public String completeOrder(
            @RequestParam("orderId") Integer orderId,
            Principal principal) {

        if (principal != null) {
            orderService.completeOrder(orderId);
        }

        return "redirect:/";
    }


    /**
     * 既存の注文データを復元してカートへ移動し、元の注文をキャンセルします（注文変更機能）
     */
    @GetMapping("/order/edit")
    public String editOrder(@RequestParam("orderId") Integer orderId, HttpSession session, Principal principal) {
        if (principal != null) {
            // 1. 指定された注文から CartData のリストを復元
            List<CartData> restoredCart = orderService.restoreCartFromOrder(orderId);
            
            if (restoredCart != null && !restoredCart.isEmpty()) {
                // セッションのカートに復元データをセット
                session.setAttribute("cart", restoredCart);
                
                // 2. カートに復元できたため、既存の注文は取り消し
                orderService.cancelOrder(orderId);
            }
        }
        return "redirect:/cart";
    }

    /**
     * 過去の注文履歴から商品をカートへ複製し、カート画面へ遷移します（再注文機能）。
     * ※ 既存の注文データは削除・キャンセルされません。
     */
    @GetMapping("/order/reorder")
    public String reorder(@RequestParam("orderId") Integer orderId, HttpSession session) {
        // 過去の注文から CartData リストを復元（コピー）
        List<CartData> restoredCart = orderService.restoreCartFromOrder(orderId);
        
        if (restoredCart != null && !restoredCart.isEmpty()) {
            // カートセッションを更新（必要に応じて既存カートへの追加・上書きを選択）
            session.setAttribute("cart", restoredCart);
        }
        
        return "redirect:/cart"; // カート画面へ遷移
    }

    // --- 加算料金・名称変換ユーティリティ ---
    
    // ザンギは追加1個につき100円
    private int getZangiPrice(int count) {
        return count * 100;
    }

    private String getRiceName(String key) {
        return switch (key) {
            case "0" -> "なし";
            case "10" -> "小盛り (150g)";
            case "30" -> "大盛り (350g)";
            case "40" -> "特盛 (450g)";
            default -> "普通 (250g)";
        };
    }

    private int getRicePrice(String key) {
        return switch (key) {
            case "0" -> 0;
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
            case "50" -> 80;
            case "51" -> 120;
            case "52" -> 150;
            case "60", "70" -> 80;
            case "61", "71" -> 120;
            case "62", "72" -> 150;
            case "80" -> 100;
            case "81" -> 140;
            case "82" -> 180;
            default -> 0;
        };
    }

    private String getSetName(String key) {
        return switch (key) {
            case "11" -> "満腹ザンギセット（味噌汁＋ポテトサラダ）";
            case "12" -> "満腹ザンギセット（味噌汁＋大根サラダ）";
            case "13" -> "満腹ザンギセット（味噌汁＋マカロニたまご）";
            case "20" -> "定番コンビセット";
            default -> "なし";
        };
    }

    private int getSetGoodsPrice(String key) {
        return switch (key) {
            case "11" -> 200;
            case "12" -> 200;
            case "13" -> 200;
            case "20" -> 200;
            default -> 0;
        };
    }

}
