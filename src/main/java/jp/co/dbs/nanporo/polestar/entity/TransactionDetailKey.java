package jp.co.dbs.nanporo.polestar.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDetailKey implements Serializable {
    private Integer transactionId;
    private Integer reservationCount;
}
