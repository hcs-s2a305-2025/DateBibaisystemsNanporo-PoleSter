package jp.co.dbs.nanporo.polestar.response;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data 
@Builder 
public class MobileOrderResponse {
    private boolean success;
    private String message;
    private Integer orderId;
    private String orderNo;
    private String mail;
    private List<MobileOrderItemDto> items;

    @Data
    @Builder
    public static class MobileOrderItemDto {
        private String productId;
        private String name;
        private Integer unitPrice;
        private Integer quantity;
        private Integer unitTotal;
    }
}
