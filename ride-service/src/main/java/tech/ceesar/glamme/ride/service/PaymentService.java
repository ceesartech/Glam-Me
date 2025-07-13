package tech.ceesar.glamme.ride.service;

import com.stripe.exception.StripeException;

import java.util.UUID;

public interface PaymentService {
    void chargeCustomer(UUID customerId, double amount, String currency) throws StripeException;
}
