package tech.ceesar.glamme.shopping.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {
    private String name;
    private String street1;
    private String street2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
