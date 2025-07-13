package tech.ceesar.glamme.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tech.ceesar.glamme.common.exception.ResourceNotFoundException;
import tech.ceesar.glamme.shopping.dto.OrderResponse;
import tech.ceesar.glamme.shopping.entity.Order;
import tech.ceesar.glamme.shopping.enums.OrderStatus;
import tech.ceesar.glamme.shopping.enums.PaymentStatus;
import tech.ceesar.glamme.shopping.enums.ShippingOption;
import tech.ceesar.glamme.shopping.repositories.OrderRepository;
import tech.ceesar.glamme.shopping.service.OrderQueryService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class OrderQueryServiceTest {
    @Mock OrderRepository orderRepo;
    @InjectMocks OrderQueryService svc;

    private final UUID orderId = UUID.randomUUID();
    private Order baseOrder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        baseOrder = Order.builder()
                .orderId(orderId)
                .customerId(UUID.randomUUID())
                .productTotal(10.0)
                .shippingCost(2.0)
                .totalAmount(12.0)
                .paymentStatus(PaymentStatus.PENDING)
                .orderStatus(OrderStatus.CREATED)
                .shippingOption(ShippingOption.PICKUP)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void getOrderById_found() {
        when(orderRepo.findById(orderId))
                .thenReturn(Optional.of(baseOrder));

        OrderResponse resp = svc.getOrderById(orderId);
        assertEquals(orderId, resp.getOrderResponseId());
        assertEquals(12.0, resp.getTotalAmount());
    }

    @Test
    void getOrderById_notFound_throws() {
        when(orderRepo.findById(orderId))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> svc.getOrderById(orderId));
    }

    @Test
    void listAllOrders_returnsAll() {
        Order o2 = Order.builder().orderId(UUID.randomUUID())
                .customerId(baseOrder.getCustomerId()).build();
        when(orderRepo.findAll()).thenReturn(List.of(baseOrder, o2));

        var list = svc.listAllOrders();
        assertEquals(2, list.size());
    }

    @Test
    void listOrdersByCustomer_filtersCorrectly() {
        UUID cust = baseOrder.getCustomerId();
        Order o2 = Order.builder()
                .orderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .build();
        when(orderRepo.findAll()).thenReturn(List.of(baseOrder, o2));

        var list = svc.listOrdersByCustomer(cust);
        assertEquals(1, list.size());
//        assertEquals(cust, list.get(0).getCustomerId());
    }
}
