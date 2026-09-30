package jp.co.dbs.nanporo.polestar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.co.dbs.nanporo.polestar.entity.GoodsEntity;
import jp.co.dbs.nanporo.polestar.entity.OrderDetailEntity;
import jp.co.dbs.nanporo.polestar.entity.OrderEntity;
import jp.co.dbs.nanporo.polestar.entity.TransactionDetailEntity;
import jp.co.dbs.nanporo.polestar.entity.TransactionEntity;
import jp.co.dbs.nanporo.polestar.repository.OrderDetailRepository;
import jp.co.dbs.nanporo.polestar.repository.OrderTRepository;
import jp.co.dbs.nanporo.polestar.repository.StoreRepository;
import jp.co.dbs.nanporo.polestar.repository.TransactionDetailRepository;
import jp.co.dbs.nanporo.polestar.repository.TransactionRepository;
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

        List<OrderDetailEntity> details = orderDetailRepository.findByOrderId(order.getOrderId());
        List<MobileOrderResponse.MobileOrderItemDto> itemDtos = new ArrayList<>();

        for (OrderDetailEntity detail : details) {
            String goodsName = storeRepository.getGoodsEntityById(detail.getGoodsId())
                    .map(GoodsEntity::getGoodsName)
                    .orElse("商品ID:" + detail.getGoodsId());

            Integer unitPrice = (detail.getCount() > 0) ? (order.getSumMoney() / detail.getCount()) : 0;

            itemDtos.add(MobileOrderResponse.MobileOrderItemDto.builder()
                    .productId(detail.getGoodsId())
                    .name(goodsName)
                    .unitPrice(unitPrice)
                    .quantity(detail.getCount())
                    .unitTotal(unitPrice * detail.getCount())
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
        Integer orderId = 0;
        String mail = "guest@example.com";

        // 1. 注文番号の頭文字が 'M' の場合：予約注文の受取会計処理
        if (request.isMobileOrder()) {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
            
            OrderEntity order = orderTRepository.findTodayOrderByNumber(request.getMobileOrderNo().trim(), startOfDay, endOfDay)
                    .orElse(null);

            if (order != null) {
                orderId = order.getOrderId();
                mail = order.getMail();
                
                // 予約注文のステータスを「受取済」に更新
                order.setStatus("受取済");
                orderTRepository.save(order);
            }
        } 
        // 2. 'M' が付いていない場合：店頭直接会計（order_t の更新処理はスキップ）

        // 取引トラン (transaction_t) の登録
        TransactionEntity transaction = new TransactionEntity();
        transaction.setOrderId(orderId);
        transaction.setMail(request.getQrId() != null ? request.getQrId() : mail);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setUseCoupon(request.getDiscount() != null && request.getDiscount() < 0 ? "値引" : null);
        transaction.setReceivedMoney(request.getReceived());
        transaction.setChangeMoney(request.getChange());
        transaction.setSumMoney(request.getTotal());

        TransactionEntity savedTransaction = transactionRepository.save(transaction);

        // 取引明細トラン (transaction_detail_t) の登録
        int reservationCount = 1;
        if (request.getItems() != null) {
            for (PaymentRequest.PaymentItemRequest item : request.getItems()) {
                TransactionDetailEntity detail = new TransactionDetailEntity();
                detail.setTransactionId(savedTransaction.getTransactionId());
                detail.setReservationCount(reservationCount++);
                detail.setGoodsName(item.getName());
                detail.setSetGoodsName(null);
                detail.setCount(item.getQuantity());
                detail.setPlusZangiCount(0);
                detail.setCustomId(null);
                detail.setPrice(item.getTotal());

                transactionDetailRepository.save(detail);
            }
        }

        return new PaymentResponse(true, "会計処理が正常に完了しました", savedTransaction.getTransactionId());
    }

    
}
