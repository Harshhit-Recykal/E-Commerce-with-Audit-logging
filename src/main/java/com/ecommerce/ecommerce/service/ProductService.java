//package com.ecommerce.ecommerce.Service;
//
//import com.ecommerce.ecommerce.Entity.Product;
//import com.ecommerce.ecommerce.dto.ProductDto;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface ProductService {
//
//    ProductDto createProduct(Product product);
//    Optional<ProductDto> getProductById(Long id);
//    Optional<ProductDto> getProductByName(String productName);
//    List<ProductDto> getAllProduct();
//    void updateProduct(Product product);
//    boolean deleteProduct(Long id);
//}
package com.ecommerce.ecommerce.service;
import com.ecommerce.ecommerce.entity.Product;
import com.ecommerce.ecommerce.dto.ProductDto;

import java.util.List;
import java.util.Optional;


public interface ProductService {
    ProductDto createProduct(Product product);
    Optional<ProductDto> getProductById(Long id);
    Optional<ProductDto> getProductByName(String name);
    void updateProduct(Product product);
    List<ProductDto> getAllProduct();
    boolean deleteProduct(Long id);
}

