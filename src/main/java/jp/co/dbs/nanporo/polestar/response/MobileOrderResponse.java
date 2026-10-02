package jp.co.dbs.nanporo.polestar.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor 
public class MobileOrderResponse {
    private boolean success;
    private String message;
    private Integer orderId;
    private String orderNo;
    private String mail;
    private List<MobileOrderItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor 
    @AllArgsConstructor 
    public static class MobileOrderItemDto {
        private String productId;
        private String name;
        private Integer unitPrice;
        private Integer quantity;
        private Integer unitTotal;
        private List<MobileToppingDto> toppings;
    }

    @Data 
    @Builder 
    @NoArgsConstructor 
    @AllArgsConstructor
    public static class MobileToppingDto {
        private String id;
        private String name;
        private Integer price;
        private Integer quantity;
    }
}
