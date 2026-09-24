package jp.co.dbs.nanporo.polestar.repository;

import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jp.co.dbs.nanporo.polestar.entity.OrderEntity;

@Repository
public interface OuterdisplayRepository extends JpaRepository<OrderEntity, Integer> {

    // 外出しディスプレイ用: '受取済' 以外 ＆ 受取日時(get_time)が本日のデータのみ取得
    @Query(value = "SELECT o.order_id, o.order_number, o.get_time, o.mail, o.sum_money, o.memo, o.status, " +
                    "d.goods_id, d.goods_name, d.count " +
                    "FROM order_t o " +
                    "LEFT JOIN order_detail_t d ON o.order_id = d.order_id " +
                    "WHERE o.status != '受取済' " +
                    "AND CAST(o.get_time AS DATE) = CURRENT_DATE " +
                    "ORDER BY o.get_time ASC", nativeQuery = true)
    List<Map<String, Object>> getAllActiveOrders();
}