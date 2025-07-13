package tech.ceesar.glamme.ride.service;

import com.stripe.exception.CardException;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.*;
import org.mockito.*;
import tech.ceesar.glamme.ride.entity.CustomerPaymentInfo;
import tech.ceesar.glamme.ride.exception.PaymentFailedException;
import tech.ceesar.glamme.ride.exception.PaymentMethodMissingException;
import tech.ceesar.glamme.ride.exception.PaymentServiceException;
import tech.ceesar.glamme.ride.repositories.CustomerPaymentInfoRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class StripePaymentServiceTest {

    @Mock
    CustomerPaymentInfoRepository repo;
    @InjectMocks StripePaymentService svc;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void chargeCustomer_createsCustomerAndFailsIfNoPM() throws Exception {
        when(repo.findByUserId(userId)).thenReturn(Optional.empty());
        try (MockedStatic<Customer> custMock = mockStatic(Customer.class)) {
            Customer stripeCust = mock(Customer.class);
            when(stripeCust.getId()).thenReturn("cus_123");
            custMock.when(() -> Customer.create(any(CustomerCreateParams.class)))
                    .thenReturn(stripeCust);

            CustomerPaymentInfo saved = CustomerPaymentInfo.builder()
                    .id(1L).userId(userId).stripeCustomerId("cus_123").build();
            when(repo.save(any())).thenReturn(saved);

            assertThrows(PaymentMethodMissingException.class,
                    () -> svc.chargeCustomer(userId, 10.0, "usd")
            );
        }
    }

    @Test
    void chargeCustomer_successful() throws Exception {
        CustomerPaymentInfo info = CustomerPaymentInfo.builder()
                .id(1L).userId(userId)
                .stripeCustomerId("cus_123")
                .defaultPaymentMethodId("pm_456")
                .build();
        when(repo.findByUserId(userId)).thenReturn(Optional.of(info));

        try (MockedStatic<PaymentIntent> piMock = mockStatic(PaymentIntent.class)) {
            PaymentIntent intent = mock(PaymentIntent.class);
            when(intent.getStatus()).thenReturn("succeeded");
            piMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenReturn(intent);

            assertDoesNotThrow(() -> svc.chargeCustomer(userId, 15.25, "usd"));

            // disambiguate to the PaymentIntentCreateParams overload
            piMock.verify(() -> PaymentIntent.create(argThat((PaymentIntentCreateParams p) ->
                    p.getAmount().equals(1525L) &&
                            "usd".equals(p.getCurrency()) &&
                            "cus_123".equals(p.getCustomer()) &&
                            "pm_456".equals(p.getPaymentMethod())
            )));
        }
    }

    @Test
    void chargeCustomer_cardDecline_throwsPaymentFailed() throws Exception {
        CustomerPaymentInfo info = CustomerPaymentInfo.builder()
                .id(1L).userId(userId)
                .stripeCustomerId("cus_123")
                .defaultPaymentMethodId("pm_456")
                .build();
        when(repo.findByUserId(userId)).thenReturn(Optional.of(info));

        try (MockedStatic<PaymentIntent> piMock = mockStatic(PaymentIntent.class)) {
            CardException cardEx = new CardException(
                    "Your card was declined", "req_xyz", "card_error", null, null, "card_declined", 402, null
            );
            piMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenThrow(cardEx);

            assertThrows(PaymentFailedException.class,
                    () -> svc.chargeCustomer(userId, 5.0, "usd")
            );
        }
    }

    @Test
    void chargeCustomer_stripeError_throwsPaymentService() throws Exception {
        CustomerPaymentInfo info = CustomerPaymentInfo.builder()
                .id(1L).userId(userId)
                .stripeCustomerId("cus_123")
                .defaultPaymentMethodId("pm_456")
                .build();
        when(repo.findByUserId(userId)).thenReturn(Optional.of(info));

        try (MockedStatic<PaymentIntent> piMock = mockStatic(PaymentIntent.class)) {
            StripeException se = new ApiConnectionException("timeout", null);
            piMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                    .thenThrow(se);

            PaymentServiceException ex = assertThrows(
                    PaymentServiceException.class,
                    () -> svc.chargeCustomer(userId, 5.0, "usd")
            );
            assertSame(se, ex.getCause());
        }
    }
}
