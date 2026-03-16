package com.ecommerse.backend.repository;

import com.ecommerse.backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    long countByCategoryIgnoreCase(String category);
}
