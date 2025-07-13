package tech.ceesar.glamme.shopping.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.ceesar.glamme.shopping.entity.Product;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> { }
