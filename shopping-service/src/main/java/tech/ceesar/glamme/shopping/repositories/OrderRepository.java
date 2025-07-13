package tech.ceesar.glamme.shopping.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.ceesar.glamme.shopping.entity.Order;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> { }
