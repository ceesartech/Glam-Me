package tech.ceesar.glamme.shopping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddressDto {
    private String name;

    private String street1;

    private String street2;

    private String city;

    private String state;

    private String postalCode;

    private String country;
}
