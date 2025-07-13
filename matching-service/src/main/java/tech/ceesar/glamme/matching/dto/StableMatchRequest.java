package tech.ceesar.glamme.matching.dto;

import lombok.Data;

import java.util.List;

@Data
public class StableMatchRequest {
    private List<CustomerDto> customers;
    private List<StylistDto> stylists;
}
