package jp.co.dbs.nanporo.polestar.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jp.co.dbs.nanporo.polestar.entity.OrderEntity;

@Repository 
public interface OrderTRepository extends JpaRepository<OrderEntity, Integer> {

    // 当日の指定注文番号の注文を取得
    @Query("SELECT o FROM OrderEntity o WHERE o.orderNumber = :orderNumber " +
            "AND o.getTime >= :startOfDay AND o.getTime <= :endOfDay")
    Optional<OrderEntity> findTodayOrderByNumber(
        @Param("orderNumber") String orderNumber,
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay
    );
    
}
