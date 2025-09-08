package tech.ceesar.glamme.service;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tech.ceesar.glamme.common.event.EventPublisher;
import tech.ceesar.glamme.common.service.RedisCacheService;
import tech.ceesar.glamme.common.service.RedisIdempotencyService;
import tech.ceesar.glamme.common.service.RedisRateLimitService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ShippingService shippingService;

    @Mock
    private RedisCacheService cacheService;

    @Mock
    private RedisIdempotencyService idempotencyService;

    @Mock
    private RedisRateLimitService rateLimitService;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest createOrderRequest;
    private Product sampleProduct;
    private Order sampleOrder;
    private UUID customerId;
    private UUID productId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        // Set up configuration values
        ReflectionTestUtils.setField(orderService, "successUrl", "http://localhost:3000/success");
        ReflectionTestUtils.setField(orderService, "cancelUrl", "http://localhost:3000/cancel");

        sampleProduct = Product.builder()
                .productId(productId)
                .name("Test Product")
                .description("Test product description")
                .price(25.99)
                .build();

        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setCustomerId(customerId);
        createOrderRequest.setItems(List.of(new OrderItemDto(productId, 2)));
        createOrderRequest.setShippingOption(ShippingOption.CLIENT);
        createOrderRequest.setSelectedRate(new ShippingRateResponse("RATE123", "UPS", "Ground", 5.99, "USD", null));

        sampleOrder = Order.builder()
                .orderId(orderId)
                .customerId(customerId)
                .orderStatus(OrderStatus.CREATED)
                .shippingOption(ShippingOption.CLIENT)
                .shippingCarrier("UPS")
                .shippingService("Ground")
                .selectedRateId("RATE123")
                .build();
    }

    @Test
    void createOrder_Success() throws StripeException {
        // Arrange
        when(rateLimitService.checkShoppingRateLimit(anyString())).thenReturn(true);
        when(idempotencyService.startOrderOperation(anyString(), anyString(), any())).thenReturn(true);
        when(productRepository.findById(productId)).thenReturn(Optional.of(sampleProduct));
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
        doNothing().when(cacheService).set(anyString(), any(), any());
        doNothing().when(idempotencyService).completeOperation(anyString(), any());

        // Mock Stripe Session
        try (MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
            Session mockSession = mock(Session.class);
            when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/test");
            sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);

            // Act
            CreateOrderResponse result = orderService.createOrder(createOrderRequest);

            // Assert
            assertNotNull(result);
            assertEquals("https://checkout.stripe.com/pay/test", result.getCheckoutUrl());

            verify(productRepository).findById(productId);
            verify(orderRepository).save(any(Order.class));
            verify(cacheService).set(anyString(), any(), any());
        }
    }

    @Test
    void createOrder_RateLimitExceeded_ThrowsException() {
        // Arrange
        when(rateLimitService.checkShoppingRateLimit(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            orderService.createOrder(createOrderRequest));

        verify(rateLimitService).checkShoppingRateLimit(customerId.toString());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_IdempotencyCheck_Fails() {
        // Arrange
        when(rateLimitService.checkShoppingRateLimit(anyString())).thenReturn(true);
        when(idempotencyService.startOrderOperation(anyString(), anyString(), any())).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            orderService.createOrder(createOrderRequest));

        verify(idempotencyService).startOrderOperation(anyString(), anyString(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void createOrder_ProductNotFound_ThrowsException() {
        // Arrange
        when(rateLimitService.checkShoppingRateLimit(anyString())).thenReturn(true);
        when(idempotencyService.startOrderOperation(anyString(), anyString(), any())).thenReturn(true);
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            orderService.createOrder(createOrderRequest));

        verify(productRepository).findById(productId);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void handlePaymentSucceeded_WithShipping_Success() {
        // Arrange
        Order orderWithShipping = sampleOrder.toBuilder()
                .orderStatus(OrderStatus.CREATED)
                .shippingOption(ShippingOption.CLIENT)
                .build();

        ShippingPurchaseResponse shippingResponse = new ShippingPurchaseResponse(
                "SHIP123", 5.99, "TRACK123", "http://label.url");

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(orderWithShipping));
        when(shippingService.purchase(any(ShippingPurchaseRequest.class))).thenReturn(shippingResponse);
        when(orderRepository.save(any(Order.class))).thenReturn(orderWithShipping);
        doNothing().when(cacheService).set(anyString(), any(), any());

        // Act
        orderService.handlePaymentSucceeded(orderId);

        // Assert
        verify(shippingService).purchase(any(ShippingPurchaseRequest.class));
        verify(orderRepository).save(argThat(order -> 
            order.getOrderStatus() == OrderStatus.SHIPPED &&
            "TRACK123".equals(order.getShippingTracking()) &&
            "http://label.url".equals(order.getShippingLabelUrl())
        ));
        verify(cacheService).set(anyString(), any(), any());
    }

    @Test
    void handlePaymentSucceeded_WithPickup_Success() {
        // Arrange
        Order pickupOrder = sampleOrder.toBuilder()
                .orderStatus(OrderStatus.CREATED)
                .shippingOption(ShippingOption.PICKUP)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(pickupOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(pickupOrder);
        doNothing().when(cacheService).set(anyString(), any(), any());

        // Act
        orderService.handlePaymentSucceeded(orderId);

        // Assert
        verify(orderRepository).save(argThat(order -> 
            order.getOrderStatus() == OrderStatus.PICKUP_READY
        ));
        verify(shippingService, never()).purchase(any());
        verify(cacheService).set(anyString(), any(), any());
    }

    @Test
    void handlePaymentSucceeded_OrderNotFound_ThrowsException() {
        // Arrange
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> 
            orderService.handlePaymentSucceeded(orderId));

        verify(orderRepository).findById(orderId);
        verify(shippingService, never()).purchase(any());
    }
}