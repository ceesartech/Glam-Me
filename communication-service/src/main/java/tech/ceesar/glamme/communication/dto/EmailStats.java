package tech.ceesar.glamme.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailStats {
    private long totalSent;
    private long totalDelivered;
    private long totalBounced;
    private long totalComplained;
    private double deliveryRate;
    private double bounceRate;
    private double complaintRate;
}
