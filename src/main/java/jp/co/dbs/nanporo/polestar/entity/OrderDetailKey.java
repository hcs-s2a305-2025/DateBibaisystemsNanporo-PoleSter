package jp.co.dbs.nanporo.polestar.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailKey implements Serializable {
    private Integer orderId;
    private Integer orderCount;
}
