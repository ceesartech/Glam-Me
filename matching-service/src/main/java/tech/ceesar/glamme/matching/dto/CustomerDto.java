package tech.ceesar.glamme.matching.dto;

import lombok.Data;
import tech.ceesar.glamme.common.enums.SubscriptionType;

import java.util.List;
import java.util.UUID;

@Data
public class CustomerDto {
    private UUID id;
    private SubscriptionType subscriptionType;
}
