package tech.ceesar.glamme.ride.service;

import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.ceesar.glamme.ride.entity.CustomerPaymentInfo;
import tech.ceesar.glamme.ride.exception.PaymentFailedException;
import tech.ceesar.glamme.ride.exception.PaymentMethodMissingException;
import tech.ceesar.glamme.ride.exception.PaymentServiceException;
import tech.ceesar.glamme.ride.repositories.CustomerPaymentInfoRepository;

import java.util.Map;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class StripePaymentService implements PaymentService {

    private final CustomerPaymentInfoRepository repo;

    @Override
    @Transactional
    public void chargeCustomer(UUID userId, double amount, String currency) {
        // 1) Lookup or create Stripe Customer
        CustomerPaymentInfo info = repo.findByUserId(userId)
                .orElseGet(() -> {
                    try {
                        Customer stripeCust = Customer.create(
                                CustomerCreateParams.builder()
                                        .setMetadata(Map.of("userId", userId.toString()))
                                        .build()
                        );
                        CustomerPaymentInfo cpi = CustomerPaymentInfo.builder()
                                .userId(userId)
                                .stripeCustomerId(stripeCust.getId())
                                .build();
                        return repo.save(cpi);
                    } catch (StripeException e) {
                        throw new PaymentServiceException(e);
                    }
                });

        // 2) Ensure default Payment Method is set
        String pm = info.getDefaultPaymentMethodId();
        if (pm == null) {
            throw new PaymentMethodMissingException(userId);
        }

        // 3) Build PaymentIntent params
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) Math.round(amount * 100))
                .setCurrency(currency)
                .setCustomer(info.getStripeCustomerId())
                .setPaymentMethod(pm)
                .setOffSession(true)
                .setConfirm(true)
                .build();

        // 4) Create & confirm
        try {
            PaymentIntent intent = PaymentIntent.create(params);
            // optionally check intent.getStatus() for "succeeded"
        } catch (CardException e) {
            // card declined, etc.
            throw new PaymentFailedException(e.getMessage());
        } catch (StripeException e) {
            // API error, network failure, etc.
            throw new PaymentServiceException(e);
        }
    }
}