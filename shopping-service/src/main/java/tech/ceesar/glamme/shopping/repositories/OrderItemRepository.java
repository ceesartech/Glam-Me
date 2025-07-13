package tech.ceesar.glamme.shopping.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.ceesar.glamme.shopping.entity.OrderItem;

import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> { }
