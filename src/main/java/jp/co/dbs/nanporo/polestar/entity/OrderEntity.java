package jp.co.dbs.nanporo.polestar.entity;

import java.sql.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data 
@Entity
@Table(name = "order_t")
public class OrderEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer orderId; // int から Integer に変更

    @Column(name = "order_number", nullable = false)
    private String orderNumber;

    @Column(name = "get_time", nullable = false)
    private Timestamp getTime;

    @Column(name = "mail", nullable = false)
    private String mail;

    @Column(name = "register_time", nullable = false)
    private Timestamp registerTime;

    @Column(name = "sum_money", nullable = false)
    private Integer sumMoney;

    @Column(name = "memo")
    private String memo;

    @Column(name = "status", nullable = false)
    private String status;
}