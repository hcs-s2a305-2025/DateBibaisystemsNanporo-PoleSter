package jp.co.dbs.nanporo.polestar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.co.dbs.nanporo.polestar.entity.TransactionDetailEntity;
import jp.co.dbs.nanporo.polestar.entity.TransactionDetailKey;

public interface TransactionDetailRepository extends JpaRepository<TransactionDetailEntity, TransactionDetailKey> {
    // 取引ID（transaction_id）に紐づく明細一覧を取得
    List<TransactionDetailEntity> findByTransactionId(Integer transactionId);
}
