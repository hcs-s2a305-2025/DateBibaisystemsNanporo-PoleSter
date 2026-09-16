package jp.co.dbs.nanporo.polestar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.co.dbs.nanporo.polestar.entity.OrderDetailEntity;
import jp.co.dbs.nanporo.polestar.entity.OrderEntity;
import jp.co.dbs.nanporo.polestar.repository.OrderRepository;

@Service 
public class OrderService {

    @Autowired 
    private OrderRepository orderRepository;

    /**
     * 新規注文を登録します。
     * 日ごとの注文番号の自動採番と、親データ・明細データの登録を同一トランザクションで実行します。
     */
    @Transactional 
    public int createOrder(OrderEntity order, List<OrderDetailEntity> details) {
        // 1. 当日の最大注文番号を取得し、+1 して自動採番
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        String nextOrderNumber = String.valueOf(orderRepository.getMaxOrderNumberByDate(startOfDay, endOfDay) + 1);
        order.setOrderNumber(nextOrderNumber);

        // 2. 注文親データ（order_t）の登録と生成された order_id の取得
        int orderId = orderRepository.insertOrder(order);

        // 3. 注文明細データ（order_detail_t）の登録
        if (details != null && !details.isEmpty()) {
            int count = 1;
            for (OrderDetailEntity detail : details) {
                detail.setOrderId(orderId);
                detail.setOrderCount(String.valueOf(count++));
                orderRepository.insertOrderDetail(detail);
            }
        }

        return orderId;

    }
    
}
