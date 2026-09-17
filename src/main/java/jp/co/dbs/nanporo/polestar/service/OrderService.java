package jp.co.dbs.nanporo.polestar.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jp.co.dbs.nanporo.polestar.data.OrderData;
import jp.co.dbs.nanporo.polestar.data.OrderDetailData;
import jp.co.dbs.nanporo.polestar.entity.OrderDetailEntity;
import jp.co.dbs.nanporo.polestar.entity.OrderEntity;
import jp.co.dbs.nanporo.polestar.repository.OrderRepository;
import jp.co.dbs.nanporo.polestar.request.OrderDetailRequest;
import jp.co.dbs.nanporo.polestar.request.OrderRegisterRequest;
import jp.co.dbs.nanporo.polestar.response.OrderRegisterResponse;

@Service 
public class OrderService {

    @Autowired 
    private OrderRepository orderRepository;

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
        // ※ 採番処理（order_id / order_number の生成）は orderRepository.insertOrder 内で実行
        int orderId = orderRepository.insertOrder(data);
        if (orderId <= 0) {
            throw new RuntimeException("注文情報の登録に失敗しました。");
        }

        // 3. 注文明細データ（order_detail_t）をループして登録
        List<OrderDetailRequest> detailList = request.getOrderDetails();
        if (detailList != null && !detailList.isEmpty()) {
            int orderCount = 1; // 明細内の連番 (1, 2, 3...)
            for (OrderDetailRequest detailRequest : detailList) {
                // 親の orderId と明細連番をセット
                OrderDetailData detail = new OrderDetailData();
                detail.setOrderId(orderId);
                detail.setOrderCount(String.valueOf(orderCount++));
                detail.setGoodsId(detailRequest.getGoodsId());
                detail.setSetGoodsId(detailRequest.getSetGoodsId());
                detail.setCount(detailRequest.getCount());
                detail.setPlusZangiCount(detailRequest.getPlusZangiCount());
                detail.setCustomId(detailRequest.getCustomId());

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
    public List<OrderData> getActiveOrders(String mail) {
        List<Map<String, Object>> rows = orderRepository.getActiveOrdersByMail(mail);
        List<OrderData> orderList = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            OrderData order = new OrderData();
            order.setOrderId((Integer) row.get("order_id"));
            order.setOrderNumber((String) row.get("order_number"));
            
            if (row.get("get_time") != null) {
                order.setGetTime((java.sql.Timestamp) row.get("get_time"));
            }
            order.setMail((String) row.get("mail"));
            order.setSumMoney((Integer) row.get("sum_money"));
            order.setMemo((String) row.get("memo"));
            order.setStatus((String) row.get("status"));
            
            orderList.add(order);
        }

        return orderList;
    }
    
}
