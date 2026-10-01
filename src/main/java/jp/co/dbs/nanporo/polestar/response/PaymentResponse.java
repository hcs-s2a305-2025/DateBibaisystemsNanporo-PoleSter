package jp.co.dbs.nanporo.polestar.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data 
@AllArgsConstructor 
public class PaymentResponse {
    private boolean success;
    private String message;
    private Integer transactionId;
    
}
