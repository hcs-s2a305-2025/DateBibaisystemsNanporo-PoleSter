package jp.co.dbs.nanporo.polestar.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.co.dbs.nanporo.polestar.entity.TransactionDetailEntity;
import jp.co.dbs.nanporo.polestar.entity.TransactionDetailKey;

public interface TransactionDetailRepository extends JpaRepository<TransactionDetailEntity, TransactionDetailKey> {
    
}
