package tech.ceesar.glamme.shopping.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.ceesar.glamme.common.exception.ResourceNotFoundException;
import tech.ceesar.glamme.shopping.dto.AddressDto;
import tech.ceesar.glamme.shopping.dto.OrderResponse;
import tech.ceesar.glamme.shopping.entity.Address;
import tech.ceesar.glamme.shopping.entity.Order;
import tech.ceesar.glamme.shopping.enums.OrderStatus;
import tech.ceesar.glamme.shopping.enums.PaymentStatus;
import tech.ceesar.glamme.shopping.enums.ShippingOption;
import tech.ceesar.glamme.shopping.repositories.OrderRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderQueryService {
    private final OrderRepository orderRepo;

    /**
     * Fetch a single order by its ID, or throw if not found.
     */
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order", "id", orderId)
                );
        return mapToDto(order);
    }

    /**
     * List all orders in the system.
     */
    public List<OrderResponse> listAllOrders() {
        return orderRepo.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * List orders for a specific customer.
     */
    public List<OrderResponse> listOrdersByCustomer(UUID customerId) {
        return orderRepo.findAll().stream()
                .filter(o -> customerId.equals(o.getCustomerId()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Convert Order entity to OrderResponse DTO.
     */
    private OrderResponse mapToDto(Order o) {
        AddressDto addr = null;
        Address a = o.getShippingAddress();
//        PaymentStatus paymentStatus = PaymentStatus.valueOf(
//                Optional.ofNullable(o.getPaymentStatus())
//                        .orElse(PaymentStatus.PENDING)
//                        .name()
//        );
        PaymentStatus paymentStatus = Optional.ofNullable(o.getPaymentStatus())
                .map(ps -> PaymentStatus.valueOf(ps.name()))
                .orElse(PaymentStatus.PENDING);

        OrderStatus orderStatus = Optional.ofNullable(o.getOrderStatus())
                .map(os -> OrderStatus.valueOf(os.name()))
                .orElse(OrderStatus.CREATED);

        ShippingOption shippingOption = Optional.ofNullable(o.getShippingOption())
                .map(so -> ShippingOption.valueOf(so.name()))
                .orElse(ShippingOption.PICKUP);

        if (a != null) {
            addr = new AddressDto(
                    a.getName(),
                    a.getStreet1(),
                    a.getStreet2(),
                    a.getCity(),
                    a.getState(),
                    a.getPostalCode(),
                    a.getCountry()
            );
        }

        return new OrderResponse(
                o.getOrderId(),
                o.getProductTotal(),
                o.getShippingCost(),
                o.getTotalAmount(),
                paymentStatus,
                orderStatus,
                shippingOption,
                o.getShippingCarrier(),
                o.getShippingService(),
                o.getShippingTracking(),
                o.getShippingLabelUrl(),
                addr,
                o.getCreatedAt()
        );
    }
}
