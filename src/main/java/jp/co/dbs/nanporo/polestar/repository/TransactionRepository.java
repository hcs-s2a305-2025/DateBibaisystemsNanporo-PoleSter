package jp.co.dbs.nanporo.polestar.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.co.dbs.nanporo.polestar.entity.TransactionEntity;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Integer> {
    // 指定された order_id の取引データが存在するかチェック（二重会計防止用）
    boolean existsByOrderId(Integer orderId);

    // 指定された日時範囲内の取引履歴を取得（最新順）
    List<TransactionEntity> findByTransactionDateBetweenOrderByTransactionDateDesc(LocalDateTime start, LocalDateTime end);
}
