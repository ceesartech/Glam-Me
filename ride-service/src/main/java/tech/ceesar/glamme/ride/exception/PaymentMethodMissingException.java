package tech.ceesar.glamme.ride.exception;

import java.util.UUID;

public class PaymentMethodMissingException extends RuntimeException {
    public PaymentMethodMissingException(UUID userId) {
        super("No default payment method for customer " + userId);
    }
}
