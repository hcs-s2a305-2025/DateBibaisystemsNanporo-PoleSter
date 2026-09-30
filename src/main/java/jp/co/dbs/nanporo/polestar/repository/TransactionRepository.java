package jp.co.dbs.nanporo.polestar.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import jp.co.dbs.nanporo.polestar.entity.TransactionEntity;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Integer> {
    
}
