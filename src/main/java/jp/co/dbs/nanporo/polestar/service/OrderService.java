package jp.co.dbs.nanporo.polestar.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.co.dbs.nanporo.polestar.data.CartData;
import jp.co.dbs.nanporo.polestar.data.OrderData;
import jp.co.dbs.nanporo.polestar.data.OrderDetailData;
import jp.co.dbs.nanporo.polestar.repository.OrderRepository;
import jp.co.dbs.nanporo.polestar.request.OrderDetailRequest;
import jp.co.dbs.nanporo.polestar.request.OrderRegisterRequest;
import jp.co.dbs.nanporo.polestar.response.ActiveOrderResponse;
import jp.co.dbs.nanporo.polestar.response.OrderHistoryResponse;
import jp.co.dbs.nanporo.polestar.response.OrderRegisterResponse;

@Service 
public class OrderService {

    @Autowired 
    private OrderRepository orderRepository;

    @Autowired
    private NotificationService notificationService;

    // 文字列や数値型を安全に Integer へ変換するメソッド
    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }

    /**
     * 新規注文を登録します。
     * 日ごとの注文番号の自動採番と、親データ・明細データの登録を同一トランザクションで実行します。
     */
    @Transactional 
    public OrderRegisterResponse insertOrder(OrderRegisterRequest request) {
        // リクエストからデータへ変換
        OrderData data = new OrderData();

        // リクエストの値を設定
        data.setGetTime(java.sql.Timestamp.valueOf(java.time.LocalDateTime.parse(request.getGetTime())));
        data.setMail(request.getMail());
        data.setRegisterTime(java.sql.Timestamp.valueOf(java.time.LocalDateTime.parse(request.getRegisterTime())));
        data.setSumMoney(request.getSumMoney());
        data.setMemo(request.getMemo());
        data.setStatus(request.getStatus());

        // 1.予約・店頭注文の識別情報を設定
        data.setOrderType(request.getOrderType());

        // 2. 注文親データ（order_t）を登録
        int orderId = orderRepository.insertOrder(data);
        if (orderId <= 0) {
            throw new RuntimeException("注文情報の登録に失敗しました。");
        }

        // 3. 注文明細データ（order_detail_t）をループして登録
        List<OrderDetailRequest> detailList = request.getOrderDetails();
        if (detailList != null && !detailList.isEmpty()) {
            int orderCount = 1; // 明細内の連番 (1, 2, 3...)
            for (OrderDetailRequest detailRequest : detailList) {
                OrderDetailData detail = new OrderDetailData();
                detail.setOrderId(orderId);
                detail.setOrderCount(orderCount++);
                
                String goodsId = detailRequest.getGoodsId();
                detail.setGoodsId(goodsId);
                
                // --- ★1. setGoodsId の設定（0やnull対策） ---
                Integer setGoodsId = detailRequest.getSetGoodsId();
                if (setGoodsId != null && setGoodsId > 0) {
                    detail.setSetGoodsId(setGoodsId);
                } else {
                    detail.setSetGoodsId(null); // 0やnullの場合はDBにNULLで登録
                }

                // --- ★2. customId (ご飯の量・ソース) の設定 ---
                boolean isSideMenu = goodsId != null && goodsId.toUpperCase().startsWith("S");
                if (isSideMenu) {
                    detail.setCustomId(0); // サイドメニューはカスタムなし(0)
                } else {
                    Integer cId = detailRequest.getCustomId();
                    // 届いた customId が null または 0 の場合は 20(普通) にする
                    detail.setCustomId((cId == null || cId == 0) ? 20 : cId);
                }

                detail.setCount(detailRequest.getCount());
                
                // プラスザンギ数の設定 (null対策)
                Integer plusZangi = detailRequest.getPlusZangiCount();
                detail.setPlusZangiCount(plusZangi != null ? plusZangi : 0);

                // ✕ 不要な重複行（108行目の detail.setCustomId(detailRequest.getCustomId()); は削除します）

                // DBへのインサート実行
                int insertedDetail = orderRepository.insertOrderDetail(detail);
                if (insertedDetail != 1) {
                    throw new RuntimeException("注文明細の登録に失敗しました。");
                }
            }
        }

        // 4. レスポンスの生成
        OrderRegisterResponse response = new OrderRegisterResponse();
        response.setOrderId(orderId);

        return response;
    }

    /**
     * ログインユーザーの予約中の注文（受付・調理中・完成）を取得します。
     */
    /**
 * ログインユーザーの予約中の注文（受付・調理中・完成）を取得します。
 */
public List<ActiveOrderResponse> getActiveOrders(String mail) {
    List<Map<String, Object>> rows = orderRepository.getActiveOrdersByMail(mail);
    Map<Integer, ActiveOrderResponse> map = new LinkedHashMap<>();

    for (Map<String, Object> row : rows) {
        Integer orderId = toInteger(row.get("order_id"));

        // 親データの生成（初回のみ）
        ActiveOrderResponse response = map.computeIfAbsent(orderId, id -> {
            ActiveOrderResponse res = new ActiveOrderResponse();
            
            OrderData order = new OrderData();
            order.setOrderId(id);
            order.setOrderNumber((String) row.get("order_number"));
            order.setGetTime((java.sql.Timestamp) row.get("get_time"));
            order.setMail((String) row.get("mail"));
            Integer sumMoney = toInteger(row.get("sum_money"));
            order.setSumMoney(sumMoney != null ? sumMoney : 0);
            order.setMemo((String) row.get("memo"));
            order.setStatus((String) row.get("status"));
            
            res.setOrder(order);
            res.setDetails(new ArrayList<>());
            return res;
        });

        // 明細データの追加
        if (row.get("goods_id") != null) {
            String goodsId = (String) row.get("goods_id");
            Integer customId = toInteger(row.get("custom_id"));

            // 既に同じ商品がグループに追加されているかチェック
            // （必要に応じて goods_id だけでなく order_count / カートID 単位で判別）
            ActiveOrderResponse.OrderDetailItem existingItem = response.getDetails().stream()
                    .filter(item -> item.getGoodsId().equals(goodsId))
                    .findFirst()
                    .orElse(null);

            if (existingItem != null) {
                // 2行目（ソースなど）の場合：既存のアイテムにソース名を追加・上書き
                if (customId != null && customId >= 50) { // 50以上はソース
                    existingItem.setSourceName(getSourceName(String.valueOf(customId)));
                }
            } else {
                // 1行目（ご飯など）の場合：新規アイテムを作成
                ActiveOrderResponse.OrderDetailItem item = new ActiveOrderResponse.OrderDetailItem();
                item.setGoodsId(goodsId);
                item.setGoodsName((String) row.get("goods_name"));
                item.setCount(toInteger(row.get("count")));

                // ご飯の量の判定
                boolean isSideMenu = goodsId != null && goodsId.toUpperCase().startsWith("S");
                String riceCode = "20"; // デフォルト（普通）
                
                if (isSideMenu) {
                    riceCode = "0";
                } else if (customId != null && customId < 50) { // 50未満をご飯コードとする場合
                    riceCode = String.valueOf(customId);
                }

                item.setRiceAmount(getRiceName(riceCode));
                item.setRicePrice(getRicePrice(riceCode));

                // もし1行目にソースコードが入っている場合への対応
                if (customId != null && customId >= 50) {
                    item.setSourceName(getSourceName(String.valueOf(customId)));
                }

                response.getDetails().add(item);
            }
        }
    }

    // 表示用の商品名文字列（重複を除外して生成）
    for (ActiveOrderResponse res : map.values()) {
        String names = res.getDetails().stream()
                .map(ActiveOrderResponse.OrderDetailItem::getGoodsName)
                .distinct()
                .collect(Collectors.joining(", "));
        res.setGoodsNames(names);
    }

    return new ArrayList<>(map.values());
}

    /**
     * ログインユーザーの予約履歴一覧を取得します。
     */
    public List<OrderHistoryResponse> getOrderHistory(String mail) {
        List<Map<String, Object>> rows = orderRepository.getOrderHistoryByMail(mail);
        Map<Integer, OrderHistoryResponse> historyMap = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy年M月d日");

        for (Map<String, Object> row : rows) {
            Integer orderId = (Integer) row.get("order_id");
            if (orderId == null) continue;

            // 注文親データの生成（初回のみ）
            OrderHistoryResponse response = historyMap.computeIfAbsent(orderId, id -> {
                OrderHistoryResponse res = new OrderHistoryResponse();
                res.setOrderId(id);
                res.setOrderNumber((String) row.get("order_number"));
                res.setSumMoney((Integer) row.get("sum_money"));
                
                if (row.get("get_time") != null) {
                    java.sql.Timestamp getTime = (java.sql.Timestamp) row.get("get_time");
                    res.setFormattedDate(getTime.toLocalDateTime().format(formatter));
                }
                res.setItems(new ArrayList<>());
                return res;
            });

            // 明細データの追加
            if (row.get("goods_id") != null) {
                OrderHistoryResponse.OrderDetailItem item = new OrderHistoryResponse.OrderDetailItem();
                item.setGoodsId((String) row.get("goods_id"));
                item.setGoodsName((String) row.get("goods_name"));
                item.setGoodsPrice(toInteger(row.get("price")));
                
                // 画像パスの設定（DBになければデフォルト画像）
                String photo = (String) row.get("photo");
                item.setPhoto(photo != null && !photo.isEmpty() ? photo : "img/ザンギ弁当.jpg");
                
                item.setCount(toInteger(row.get("count")));
                
                // トッピング・ソース情報のセット
                Integer customId = toInteger(row.get("custom_id"));
                if (customId != null && customId > 0) {
                    String sourceCode = String.valueOf(customId);
                    item.setCustomName(getSourceName(sourceCode));
                    item.setCustomPrice(getSourcePrice(sourceCode));
                }
                
                response.getItems().add(item);
            }
        }

        return new ArrayList<>(historyMap.values());
    }

    /* ==================================================
     *  注文取り消し・カート復元処理
     * ================================================== */

    /**
     * 指定された注文を取り消し（削除）します。
     */
    @Transactional
    public void cancelOrder(Integer orderId) {
        if (orderId != null) {
            orderRepository.deleteOrder(orderId);
        }
    }

    /**
     * 指定された注文を「完成」に変更し、
     * 注文者へ完成通知を送信します。
     */
    @Transactional
    public void completeOrder(Integer orderId) {

        if (orderId == null) {
            throw new IllegalArgumentException("注文IDが指定されていません。");
        }

        // 対象注文を取得
        Map<String, Object> order = orderRepository.getOrderById(orderId);

        if (order == null) {
            throw new IllegalArgumentException("指定された注文が存在しません。");
        }

        // 現在のステータスを確認
        String currentStatus = (String) order.get("status");

        // 「受付」「調理中」以外は完成処理を行わない
        if (!"受付".equals(currentStatus) && !"調理中".equals(currentStatus)) {
            throw new IllegalStateException(
                    "この注文は完成状態へ変更できません。現在のステータス: " + currentStatus);
        }

        // ステータスを「完成」に変更
        int updatedCount = orderRepository.updateStatusToComplete(orderId);

        if (updatedCount != 1) {
            throw new IllegalStateException("注文ステータスの更新に失敗しました。");
        }

        // 完成通知を作成・メール送信
        notificationService.sendOrderCompleteNotification(orderId);
    }

    /**
     * 指定された注文から情報を復元し、カート保持用の List<CartData> を構築します。
     */
    public List<CartData> restoreCartFromOrder(Integer orderId) {
        if (orderId == null) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> details = orderRepository.getOrderDetailsByOrderId(orderId);
        List<CartData> cartList = new ArrayList<>();

        for (Map<String, Object> row : details) {
            CartData item = new CartData();
            item.setCartItemId(UUID.randomUUID().toString());

            String goodsId = (String) row.get("goods_id");
            item.setGoodsId(goodsId);
            item.setGoodsName((String) row.get("goods_name"));

            Integer basePrice = toInteger(row.get("price"));
            item.setPrice(basePrice != null ? basePrice : 0);
            item.setPhoto((String) row.get("photo"));
            item.setOrderDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年M月d日")));

            // ザンギ追加数と加算額
            Integer zangiCount = toInteger(row.get("plus_zangi_count"));
            item.setZangiCount(zangiCount != null ? zangiCount : 0);
            int zPrice = getZangiPrice(item.getZangiCount());
            item.setZangiPrice(zPrice);

            // ソースコードと加算額
            Integer customIdObj = toInteger(row.get("custom_id"));
            String sourceCode = customIdObj != null ? String.valueOf(customIdObj) : "0";
            item.setSourceCode(sourceCode);
            item.setSourceType(getSourceName(sourceCode));
            int sPrice = getSourcePrice(sourceCode);
            item.setSourcePrice(sPrice);

            // ライスコード（初期値: 標準 "20"）
            boolean isSideMenu = goodsId != null && goodsId.toUpperCase().startsWith("S");
            
            // DB（row）からライスコードを取得（カラム名が set_goods_id や rice_code などにある場合）
            Integer setGoodsId = toInteger(row.get("set_goods_id"));
            String riceCode;

            if (setGoodsId != null && setGoodsId > 0) {
                riceCode = String.valueOf(setGoodsId);
            } else {
                riceCode = isSideMenu ? "0" : "20";
            }

            item.setRiceCode(riceCode);
            item.setRiceAmount(getRiceName(riceCode));
            int rPrice = getRicePrice(riceCode);
            item.setRicePrice(rPrice);

            // 合計金額の算出
            item.setTotalPrice(item.getPrice() + zPrice + rPrice + sPrice);

            cartList.add(item);
        }

        return cartList;
    }

    // --- 加算料金・名称計算ヘルパーメソッド ---

    private int getZangiPrice(int count) {
        int baseCount = 5;
        if (count > baseCount) {
            return (count - baseCount) * 100;
        }
        return 0;
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
