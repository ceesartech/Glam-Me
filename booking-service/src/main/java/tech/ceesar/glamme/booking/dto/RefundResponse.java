package tech.ceesar.glamme.booking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RefundResponse {
    
    private String refundId;
    
    private String paymentIntentId;
    
    private BigDecimal amount;
    
    private String status;
    
    private String reason;
    
    private String errorMessage;
}
