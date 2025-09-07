package tech.ceesar.glamme.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsStats {
    private long totalSent;
    private long totalDelivered;
    private long totalFailed;
    private double deliveryRate;
    private double failureRate;
    private String monthlyUsage;
}
