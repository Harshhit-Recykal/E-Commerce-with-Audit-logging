package com.ecommerce.ecommerce.service;

import com.ecommerce.ecommerce.entity.Product;
import com.ecommerce.ecommerce.dto.ProductDto;

import java.util.List;
import java.util.Optional;


public interface ProductService {
    ProductDto createProduct(ProductDto product);

    Optional<ProductDto> getProductById(Long id);

    Optional<ProductDto> getProductByName(String name);

    ProductDto updateProduct(ProductDto product, ProductDto request);

    List<ProductDto> getAllProduct();

    boolean deleteProduct(Long id);
}

