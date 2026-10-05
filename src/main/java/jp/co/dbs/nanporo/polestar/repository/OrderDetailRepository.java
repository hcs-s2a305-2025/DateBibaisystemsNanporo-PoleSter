package jp.co.dbs.nanporo.polestar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import jp.co.dbs.nanporo.polestar.entity.OrderDetailEntity;
import jp.co.dbs.nanporo.polestar.entity.OrderDetailKey;

public interface OrderDetailRepository extends JpaRepository<OrderDetailEntity, OrderDetailKey> {
    List<OrderDetailEntity> findByOrderId(Integer orderId);
}
