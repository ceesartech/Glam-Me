package tech.ceesar.glamme.booking.service;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.common.exception.ResourceNotFoundException;
import tech.ceesar.glamme.matching.entity.AddOn;
import tech.ceesar.glamme.matching.entity.ServiceOffering;
import tech.ceesar.glamme.matching.repository.ServiceOfferingRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final ServiceOfferingRepository offeringRepository;

    @Value("${stripe.successUrl}")
    private String successUrl;

    @Value("${stripe.cancelUrl}")
    private String cancelUrl;

    public String createCheckoutSession(UUID bookingId) throws StripeException {
        ServiceOffering off = offeringRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("ServiceOffering", "id", bookingId)
                );

        // total cost in cents
        double totalCost = off.getCostPerHour() * off.getEstimatedHours()
                + off.getAddOns().stream().mapToDouble(AddOn::getCost).sum();
        long amount = Math.round(totalCost * 100);

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(bookingId.toString())
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("usd")
                                                .setUnitAmount(amount)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName("Hair Service: " + off.getStyleName())
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }
}
