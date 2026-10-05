package jp.co.dbs.nanporo.polestar.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.co.dbs.nanporo.polestar.entity.CustomEntity;
import jp.co.dbs.nanporo.polestar.entity.GoodsEntity;
import jp.co.dbs.nanporo.polestar.entity.OrderDetailEntity;
import jp.co.dbs.nanporo.polestar.entity.OrderEntity;
import jp.co.dbs.nanporo.polestar.entity.SetGoodsEntity;
import jp.co.dbs.nanporo.polestar.entity.TransactionDetailEntity;
import jp.co.dbs.nanporo.polestar.entity.TransactionEntity;
import jp.co.dbs.nanporo.polestar.entity.UserEntity;
import jp.co.dbs.nanporo.polestar.repository.OrderDetailRepository;
import jp.co.dbs.nanporo.polestar.repository.OrderTRepository;
import jp.co.dbs.nanporo.polestar.repository.StoreRepository;
import jp.co.dbs.nanporo.polestar.repository.TransactionDetailRepository;
import jp.co.dbs.nanporo.polestar.repository.TransactionRepository;
import jp.co.dbs.nanporo.polestar.repository.UserRepository;
import jp.co.dbs.nanporo.polestar.request.MobileOrderRequest;
import jp.co.dbs.nanporo.polestar.request.PaymentRequest;
import jp.co.dbs.nanporo.polestar.response.MobileOrderResponse;
import jp.co.dbs.nanporo.polestar.response.PaymentResponse;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class PosServiceImple implements PosService {

    private final OrderTRepository orderTRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final StoreRepository storeRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionDetailRepository transactionDetailRepository;
    private final UserRepository userRepository;

    /**
     * 当日の予約注文（モバイルオーダー）取得処理
     */
    @Override
    @Transactional(readOnly = true)
    public MobileOrderResponse getTodayMobileOrder(MobileOrderRequest request) {
        String targetOrderNo = request.getEffectiveOrderNo();

        if (targetOrderNo.isEmpty()) {
            throw new IllegalArgumentException("注文番号が指定されていません。");
        }

        // 頭文字が 'M' でない場合は予約注文ではないためエラーを返す
        if (!request.isMobileOrder()) {
            throw new IllegalArgumentException("入力された番号（" + targetOrderNo + "）は予約注文番号ではありません。");
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        // order_t から当日の 'M' 付き注文番号で検索
        OrderEntity order = orderTRepository.findTodayOrderByNumber(targetOrderNo, startOfDay, endOfDay)
                .orElseThrow(() -> new RuntimeException("当日の予約注文が見つかりません: " + targetOrderNo));
        // 取引トラン(transaction_t)に注文番号(order_id)が存在する場合はエラーを返す
        if (transactionRepository.existsByOrderId(order.getOrderId())) {
            throw new IllegalArgumentException("注文番号（" + targetOrderNo + "）は既に会計が完了しています。");
        }

        List<OrderDetailEntity> details = orderDetailRepository.findByOrderId(order.getOrderId());
        List<MobileOrderResponse.MobileOrderItemDto> itemDtos = new ArrayList<>();

        for (OrderDetailEntity detail : details) {
            // 1. 本体商品の取得と価格設定
            GoodsEntity goods = storeRepository.getGoodsEntityById(detail.getGoodsId()).orElse(null);
            String baseGoodsName = (goods != null) ? goods.getGoodsName() : "商品ID:" + detail.getGoodsId();
            int goodsPrice = (goods != null && goods.getPrice() != null) ? goods.getPrice() : 0;
            List<MobileOrderResponse.MobileToppingDto> toppingList = new ArrayList<>();

            // 2. セット商品の追加価格取得（setGoodsId が存在する場合）
            int setPrice = 0;
            if (detail.getSetGoodsId() != null) {
                SetGoodsEntity setGoods = storeRepository.getSetGoodsEntityById(detail.getSetGoodsId()).orElse(null);
                if (setGoods != null) {
                    setPrice = (setGoods.getPrice() != null) ? setGoods.getPrice() : 0;
                    String setName = setGoods.getGoodsName();
                    if (setName != null && !setName.isEmpty()) {
                        toppingList.add(MobileOrderResponse.MobileToppingDto.builder()
                            .id("SET_" + detail.getSetGoodsId())
                            .name(setName)
                            .price(setPrice)
                            .quantity(1)
                            .build());
                    }
                }
            }

            // 3. カスタム/トッピングの取得と名称の結合（customId が存在する場合）
            int customPrice = 0;
            if (detail.getCustomId() != null) {
                CustomEntity custom = storeRepository.getCustomEntityById(detail.getCustomId()).orElse(null);
                if (custom != null) {
                    customPrice = (custom.getPrice() != null) ? custom.getPrice() : 0;
                    String customName = custom.getGoodsName(); 
                    if (customName != null && !customName.isEmpty()) {
                        toppingList.add(MobileOrderResponse.MobileToppingDto.builder()
                            .id("CUSTOM_" + detail.getCustomId())
                            .name(customName)
                            .price(customPrice)
                            .quantity(1)
                            .build());
                    }
                }
            }

            // 1個あたりの合計単価（本体 + セット + カスタム）
            int unitPrice = goodsPrice + setPrice + customPrice;
            int quantity = (detail.getCount() != null && detail.getCount() > 0) ? detail.getCount() : 1;

            itemDtos.add(MobileOrderResponse.MobileOrderItemDto.builder()
                    .productId(detail.getGoodsId())
                    .name(baseGoodsName) // 改行付きで結合した名称をセット
                    .unitPrice(unitPrice)
                    .quantity(quantity)
                    .unitTotal(unitPrice * quantity)
                    .toppings(toppingList)
                    .build());
        }

        return MobileOrderResponse.builder()
                .success(true)
                .message("予約注文情報を取得しました")
                .orderId(order.getOrderId())
                .orderNo(order.getOrderNumber())
                .mail(order.getMail())
                .items(itemDtos)
                .build();
    }

    /**
     * 会計登録処理（予約注文と店頭注文の自動判別対応）
     */
    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Integer orderId = 0;
        String mail = request.getQrId() != null ? request.getQrId() : "店頭注文";

        // 1. 注文番号の頭文字が 'M' の場合：予約注文の受取会計処理
        if (request.isMobileOrder()) {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
            
            OrderEntity order = orderTRepository.findTodayOrderByNumber(request.getMobileOrderNo().trim(), startOfDay, endOfDay)
                    .orElse(null);

            if (order != null) {
                // 【二重会計防止】既に取引トランに同一order_idが存在する場合は弾く
                if (transactionRepository.existsByOrderId(order.getOrderId())) {
                    throw new IllegalArgumentException("この予約注文は既に会計が完了しています。");
                }
                orderId = order.getOrderId();
                mail = order.getMail();
                
                // 予約注文のステータスを「受取済」に更新
                order.setStatus("受取済");
                orderTRepository.save(order);
            }
        } else {
            // --- B. 店頭直接注文の場合 ---
            OrderEntity newOrder = new OrderEntity();
            // 【4桁連番採番ロジック】当日の最大の注文番号（数値）を取得して +1 する
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
            
            String maxOrderNo = orderTRepository.findMaxOrderNumberToday(startOfDay, endOfDay).orElse(null);
            int nextNumber = 1;
            if (maxOrderNo != null) {
                try {
                    nextNumber = Integer.parseInt(maxOrderNo.trim()) + 1;
                } catch (NumberFormatException e) {
                    nextNumber = 1;
                }
            }
            
            // 4桁ゼロ埋め（0001, 0002...）
            String formattedOrderNo = String.format("%04d", nextNumber);
            newOrder.setOrderNumber(formattedOrderNo);
            newOrder.setMail(mail); // 「店頭注文」または QR ID
            newOrder.setRegisterTime(Timestamp.valueOf(now)); // 会計時間を設定
            newOrder.setGetTime(Timestamp.valueOf(now));      // 会計時間と全く同じ時間を設定
            newOrder.setStatus("会計済");
            newOrder.setSumMoney(request.getTotal());

            // order_t に登録し、自動採番された order_id を取得
            OrderEntity savedOrder = orderTRepository.save(newOrder);
            orderId = savedOrder.getOrderId();

            // 【店頭注文】order_detail_t への登録（トッピング重複時の分割登録対応）
            if (request.getItems() != null) {
                int orderCount = 1; // 連番用カウンタ
                for (PaymentRequest.PaymentItemRequest item : request.getItems()) {
                    List<OrderDetailEntity> detailList = createOrderDetailsFromItem(orderId, item);
                    for (OrderDetailEntity detail : detailList) {
                        detail.setOrderCount(orderCount++); // order_count に連番をセット
                        orderDetailRepository.save(detail);
                    }
                }
            }
        }

        // 取引トラン (transaction_t) の登録
        TransactionEntity transaction = new TransactionEntity();
        transaction.setOrderId(orderId);
        transaction.setMail(mail);
        transaction.setTransactionDate(now);
        // ★リクエストからの useCoupon （例: "学生割引,スタンプカード割引"）を優先して登録★
        if (request.getUseCoupon() != null && !request.getUseCoupon().trim().isEmpty()) {
            transaction.setUseCoupon(request.getUseCoupon().trim());
        } else if (request.getDiscount() != null && request.getDiscount() < 0) {
            transaction.setUseCoupon("値引");
        } else {
            transaction.setUseCoupon(null);
        }
        transaction.setReceivedMoney(request.getReceived());
        transaction.setChangeMoney(request.getChange());
        transaction.setSumMoney(request.getTotal());

        TransactionEntity savedTransaction = transactionRepository.save(transaction);

        // 取引明細トラン (transaction_detail_t) の登録
        int reservationCount = 1;
        List<OrderDetailEntity> orderDetails = orderDetailRepository.findByOrderId(orderId);
        
        for (OrderDetailEntity od : orderDetails) {
            TransactionDetailEntity detail = new TransactionDetailEntity();
            detail.setTransactionId(savedTransaction.getTransactionId());
            detail.setReservationCount(reservationCount++);

            // 商品名の取得
            GoodsEntity goods = storeRepository.getGoodsEntityById(od.getGoodsId()).orElse(null);
            detail.setGoodsName(goods != null ? goods.getGoodsName() : "商品ID:" + od.getGoodsId());

            // セット商品名の取得（set_goods_id が存在する場合）
            if (od.getSetGoodsId() != null) {
                SetGoodsEntity setGoods = storeRepository.getSetGoodsEntityById(od.getSetGoodsId()).orElse(null);
                detail.setSetGoodsName(setGoods != null ? setGoods.getGoodsName() : null);
            } else {
                detail.setSetGoodsName(null);
            }

            // 数量・増量数・カスタムID
            detail.setCount(od.getCount() != null ? od.getCount() : 1);
            detail.setPlusZangiCount(od.getPlusZangiCount() != null ? od.getPlusZangiCount() : 0);
            detail.setCustomId(od.getCustomId());

            // 1行あたりの小計価格を計算
            int gPrice = (goods != null && goods.getPrice() != null) ? goods.getPrice() : 0;
            int sPrice = 0;
            if (od.getSetGoodsId() != null) {
                SetGoodsEntity setGoods = storeRepository.getSetGoodsEntityById(od.getSetGoodsId()).orElse(null);
                if (setGoods != null && setGoods.getPrice() != null) sPrice = setGoods.getPrice();
            }
            int cPrice = 0;
            if (od.getCustomId() != null) {
                CustomEntity custom = storeRepository.getCustomEntityById(od.getCustomId()).orElse(null);
                if (custom != null && custom.getPrice() != null) cPrice = custom.getPrice();
            }
            detail.setPrice((gPrice + sPrice + cPrice) * detail.getCount());

            transactionDetailRepository.save(detail);
        }
        // 取引トラン保存完了後、ポイントおよび会員ランクの更新処理を実行
        updateMemberPointAndRank(mail, orderId);
        return new PaymentResponse(true, "会計処理が正常に完了しました", savedTransaction.getTransactionId());
    }

    /**
     * 会員ポイント・ポイントカード完了数・会員ランクの更新処理
     */
    private void updateMemberPointAndRank(String mailOrQrId, Integer orderId) {
        // 会員情報がない・店頭注文（未読み取り）の場合は処理をスキップ
        if (mailOrQrId == null || "店頭注文".equals(mailOrQrId) || mailOrQrId.trim().isEmpty()) {
            return;
        }

        // 1. 会員検索 (queryForMapは一致するレコードがないと EmptyResultDataAccessException を投げるため例外捕捉)
        Map<String, Object> userMap;
        try {
            userMap = userRepository.findByMail(mailOrQrId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // 該当するユーザーが存在しない場合は終了
            return;
        }

        if (userMap == null || userMap.isEmpty()) {
            return;
        }

        List<OrderDetailEntity> orderDetails = orderDetailRepository.findByOrderId(orderId);
        int totalZangiCount = 0;

        // 2. 今回の注文に含まれるザンギ個数を集計
        for (OrderDetailEntity detail : orderDetails) {
            GoodsEntity goods = storeRepository.getGoodsEntityById(detail.getGoodsId()).orElse(null);
            
            // 商品マスタのザンギ個数 (zangi_count)
            int baseZangiCount = (goods != null && goods.getZangiCount() != null) ? goods.getZangiCount() : 0;
            // 明細のプラスザンギ個数 (plus_zangi_count)
            int plusZangiCount = (detail.getPlusZangiCount() != null) ? detail.getPlusZangiCount() : 0;
            // 注文数量
            int itemCount = (detail.getCount() != null && detail.getCount() > 0) ? detail.getCount() : 1;

            totalZangiCount += (baseZangiCount + plusZangiCount) * itemCount;
        }

        if (totalZangiCount <= 0) {
            return;
        }

        // 3. Mapから現在の値を取り出し（DBのカラム名と合わせる）
        int currentPoint = userMap.get("point") != null ? ((Number) userMap.get("point")).intValue() : 0;
        int currentCardComplete = userMap.get("point_card_complete") != null ? ((Number) userMap.get("point_card_complete")).intValue() : 0;
        String currentRank = (String) userMap.get("member_rank");

        // 4. ポイント計算と繰り越し処理
        int totalPoint = currentPoint + totalZangiCount;
        int completedCardsToAdd = totalPoint / 20; // 20ポイントで1枚達成
        int remainingPoint = totalPoint % 20;      // 余りポイント

        int newCardComplete = currentCardComplete + completedCardsToAdd;

        // 5. 会員ランク判定
        String newRank = currentRank;
        if (newCardComplete >= 5) {
            newRank = "ゴールド";
        } else if (newCardComplete >= 3) {
            newRank = "シルバー";
        } else if (newCardComplete >= 1) {
            newRank = "ブロンズ";
        }

        // 6. UserRepository の UPDATE メソッドでDBを更新
        userRepository.updateMemberPointAndRank(mailOrQrId, remainingPoint, newCardComplete, newRank);
    }

    /**
     * 店頭注文リクエストから OrderDetailEntity のリストを生成するヘルパーメソッド
     * トッピング（ご飯の量、ソース等）が複数ある場合はレコードを分割生成します。
     */
    private List<OrderDetailEntity> createOrderDetailsFromItem(Integer orderId, PaymentRequest.PaymentItemRequest item) {
        List<OrderDetailEntity> details = new ArrayList<>();

        Integer riceCustomId = null;
        Integer sauceCustomId = null;
        Integer setGoodsId = null;
        int plusZangiCount = 0;

        // リクエストの toppings 配列からカスタムID・セット商品ID等を正確に分類
        if (item.getToppings() != null) {
            for (PaymentRequest.ToppingRequest t : item.getToppings()) {
                String name = t.getName();
                if (name == null || name.isEmpty()) continue;

                // 1. ごはんの量の判定 (custom_m)
                if (name.contains("小盛り")) {
                    riceCustomId = 10;
                } else if (name.contains("普通")) {
                    riceCustomId = 20;
                } else if (name.contains("大盛り")) {
                    riceCustomId = 30;
                } else if (name.contains("特盛り")) {
                    riceCustomId = 40;
                } 
                // 2. ソース類の判定 (custom_m) - 「だくだく」「ソースだく/だく」の名称から判定
                else if (name.contains("おろしポン酢")) {
                    if (name.contains("だくだく")) {
                        sauceCustomId = 52;
                    } else if (name.contains("だく")) {
                        sauceCustomId = 51;
                    } else {
                        sauceCustomId = 50;
                    }
                } else if (name.contains("タルタル")) {
                    if (name.contains("だくだく")) {
                        sauceCustomId = 62;
                    } else if (name.contains("だく")) {
                        sauceCustomId = 61;
                    } else {
                        sauceCustomId = 60;
                    }
                } else if (name.contains("ネギダレ") || name.contains("ネギタレ")) {
                    if (name.contains("だくだく")) {
                        sauceCustomId = 72;
                    } else if (name.contains("だく")) {
                        sauceCustomId = 71;
                    } else {
                        sauceCustomId = 70;
                    }
                } else if (name.contains("麻婆")) {
                    if (name.contains("だくだく")) {
                        sauceCustomId = 82;
                    } else if (name.contains("だく")) {
                        sauceCustomId = 81;
                    } else {
                        sauceCustomId = 80;
                    }
                }
                // 3. セット商品の判定 (set_goods_m)
                else if (name.contains("ポテトサラダ")) {
                    setGoodsId = 11;
                } else if (name.contains("大根サラダ")) {
                    setGoodsId = 12;
                } else if (name.contains("マカロニ")) {
                    setGoodsId = 13;
                } else if (name.contains("緑茶")) {
                    setGoodsId = 20;
                }
                // 4. 追加ザンギ等のカウント
                else if (t.getPlusZangiCount() != null) {
                    plusZangiCount += t.getPlusZangiCount();
                }
            }
        }

        // --- 1行目の作成 (メイン商品 + ご飯量カスタム + セット商品) ---
        OrderDetailEntity detail1 = new OrderDetailEntity();
        detail1.setOrderId(orderId);
        detail1.setGoodsId(item.getProductId());
        detail1.setCount(item.getQuantity() != null ? item.getQuantity() : 1);
        detail1.setSetGoodsId(setGoodsId);
        detail1.setPlusZangiCount(plusZangiCount);
        detail1.setCustomId(riceCustomId != null ? riceCustomId : sauceCustomId);
        details.add(detail1);

        // --- 2行目の作成 (ご飯の量とソースの両方がある場合、ソース用の2行目を追加) ---
        // if (riceCustomId != null && sauceCustomId != null) {
        //     OrderDetailEntity detail2 = new OrderDetailEntity();
        //     detail2.setOrderId(orderId);
        //     detail2.setGoodsId(item.getProductId());
        //     detail2.setCount(item.getQuantity() != null ? item.getQuantity() : 1);
        //     detail2.setSetGoodsId(null);
        //     detail2.setPlusZangiCount(0);
        //     detail2.setCustomId(sauceCustomId); // ソースのcustom_idを設定
        //     details.add(detail2);
        // }

        return details;
    }

    /**
     * 商品の販売停止/再開状態を更新する処理
     */
    @Override
    @Transactional
    public void updateGoodsSoldOut(String goodsId, Boolean soldOut) {
        if (goodsId == null || soldOut == null) {
            throw new IllegalArgumentException("商品IDまたは販売状態が指定されていません。");
        }

        // リポジトリの updateSoldOut を実行して更新件数を取得
        int updatedCount = storeRepository.updateSoldOut(goodsId, soldOut);

        // 更新対象のレコードが存在しなかった場合
        if (updatedCount == 0) {
            throw new IllegalArgumentException("指定された商品が見つかりません。ID: " + goodsId);
        }
    }
}
