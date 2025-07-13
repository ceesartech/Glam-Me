package tech.ceesar.glamme.service;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import tech.ceesar.glamme.shopping.dto.*;
import tech.ceesar.glamme.shopping.entity.Order;
import tech.ceesar.glamme.shopping.entity.Product;
import tech.ceesar.glamme.shopping.enums.OrderStatus;
import tech.ceesar.glamme.shopping.enums.ShippingOption;
import tech.ceesar.glamme.shopping.repositories.OrderRepository;
import tech.ceesar.glamme.shopping.repositories.ProductRepository;
import tech.ceesar.glamme.shopping.service.OrderService;
import tech.ceesar.glamme.shopping.service.ShippingService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OrderServiceTest {
    @Mock
    OrderRepository orderRepo;
    @Mock
    ProductRepository productRepo;
    @Mock
    ShippingService shippingService;
    @InjectMocks
    OrderService orderService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createOrder_buildsSession() {
        UUID prodId = UUID.randomUUID();
        Product p = new Product(); p.setProductId(prodId); p.setPrice(20);
        when(productRepo.findById(prodId)).thenReturn(Optional.of(p));

        CreateOrderRequest req = new CreateOrderRequest();
        req.setCustomerId(UUID.randomUUID());
        req.setItems(List.of(new OrderItemDto(prodId,2)));
        req.setShippingOption(ShippingOption.PICKUP);
        req.setSelectedRate(new ShippingRateResponse("","","",0,"",null));

        Order saved = Order.builder().orderId(UUID.randomUUID()).build();
        when(orderRepo.save(any())).thenReturn(saved);
        // stub Stripe Session
        try (MockedStatic<Session> mock = mockStatic(Session.class)) {
            Session s = mock(Session.class);
            when(s.getUrl()).thenReturn("https://pay");
            mock.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(s);

            CreateOrderResponse resp = orderService.createOrder(req);
            assertEquals("https://pay", resp.getCheckoutUrl());
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void handlePaymentSucceeded_purchasesShipmentAndUpdatesOrder() {
        UUID orderId = UUID.randomUUID();
        Order o = Order.builder()
                .orderId(orderId)
                .shippingOption(ShippingOption.CLIENT)
                .shippingCarrier("UPS")
                .shippingService("Ground")
                .selectedRateId("RATE123")
                .build();
        when(orderRepo.findById(orderId)).thenReturn(Optional.of(o));

        ShippingPurchaseResponse resp = new ShippingPurchaseResponse(
                "SHIP123", 5.0, "TRACK123", "http://label.url");
        when(shippingService.purchase(
                argThat(req -> "RATE123".equals(req.getRateId()))
        )).thenReturn(resp);

        orderService.handlePaymentSucceeded(orderId);

        verify(shippingService).purchase(any());
        verify(orderRepo).save(argThat(saved ->
                saved.getOrderStatus() == OrderStatus.SHIPPED &&
                        "TRACK123".equals(saved.getShippingTracking()) &&
                        "http://label.url".equals(saved.getShippingLabelUrl())
        ));
    }
}
