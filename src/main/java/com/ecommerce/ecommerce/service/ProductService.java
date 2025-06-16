package com.ecommerce.ecommerce.service;

import com.ecommerce.ecommerce.entity.Product;
import com.ecommerce.ecommerce.dto.ProductDto;

import java.util.List;
import java.util.Optional;


public interface ProductService {
    ProductDto createProduct(Product product);

    Optional<ProductDto> getProductById(Long id);

    Optional<ProductDto> getProductByName(String name);

    void updateProduct(ProductDto product);

    List<ProductDto> getAllProduct();

    boolean deleteProduct(Long id);
}

