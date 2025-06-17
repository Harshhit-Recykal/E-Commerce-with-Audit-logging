package com.ecommerce.ecommerce.controllers;

import aj.org.objectweb.asm.commons.Remapper;
import com.ecommerce.ecommerce.dto.ApiResponse;
import com.ecommerce.ecommerce.dto.ProductDto;
import com.ecommerce.ecommerce.entity.Product;
import com.ecommerce.ecommerce.service.ProductService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    private final ModelMapper modelMapper;

    @Autowired
    public ProductController(ProductService productService, ModelMapper modelMapper) {
        this.productService = productService;
        this.modelMapper = modelMapper;
    }

    @PostMapping("/new")
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(@RequestBody Product product) {
        ProductDto savedProduct = productService.createProduct(product);
        ApiResponse<ProductDto> response = new ApiResponse<>(
                true,
                "New product created successfully",
                savedProduct
        );
        return ResponseEntity.created(URI.create("/api/products/" + savedProduct.getProductById()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> getProductById(@PathVariable Long id) {
        Optional<ProductDto> productOpt = productService.getProductById(id);
        return productOpt.map(productDto ->
                ResponseEntity.ok().body(new ApiResponse<>(
                        true,
                        "Product fetched successfully",
                        productDto))
        ).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, "Product not available", null)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProductDto>> getProductByName(@RequestParam(name = "product_name") String productName) {
        Optional<ProductDto> productOpt = productService.getProductByName(productName);
        return productOpt.map(productDto ->
                ResponseEntity.ok().body(new ApiResponse<>(
                        true,
                        "Product fetched successfully",
                        productDto))
        ).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, "Product not available", null)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> updateProductById(@PathVariable Long id, @RequestBody ProductDto request) {
        Optional<ProductDto> productOpt = productService.getProductById(id);
        if (productOpt.isPresent()) {
            productOpt = Optional.ofNullable(productService.updateProduct(modelMapper.map(productOpt, Product.class), request));
        }
        return productOpt.map(productDto ->
                ResponseEntity.ok().body(new ApiResponse<>(
                        true,
                        "Product updated successfully",
                        productDto))
        ).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, "Product not available", null)));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getAllProducts() {
        List<ProductDto> products = productService.getAllProduct();
        String message = products.isEmpty() ? "No products available" : "Products listed below";
        ApiResponse<List<ProductDto>> response = new ApiResponse<>(true, message, products);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> deleteProductById(@PathVariable Long id) {
        boolean isDeleted = productService.deleteProduct(id);
        if (isDeleted) {
            return ResponseEntity.ok().body(new ApiResponse<>(true, "Product deleted successfully", null));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, "Cannot find the product", null));
    }
}
